package repository;

import dto.request.PaginationDTO;
import dto.request.ProductFilterDTO;
import dto.request.SearchProductsRequest;
import dto.request.SortDTO;
import dto.response.PageResponse;
import entity.Product;
import utils.DBConnectionPool;
import utils.SqlQueryBuilder;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository extends AbstractRepository<Product> {
    public ProductRepository(DBConnectionPool dbConnectionPool) {
        super(dbConnectionPool);
    }

    @Override
    public int create(Product product) {
        String sql = "INSERT INTO product (name, count_in_stock, price, product_category_id) VALUES (?, ?, ?, ?)";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, product.name());
                ps.setInt(2, product.countInStock());
                ps.setDouble(3, product.price().doubleValue());
                ps.setInt(4, product.productCategoryId());

                ps.executeUpdate();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next())
                        return generatedKeys.getInt(1);
                    else
                        throw new SQLException("Creating product failed, no ID obtained.");
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error creating product", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
    }

    @Override
    public void update(int id, Product product) {
        String sql = "UPDATE product SET name=?, count_in_stock=?, price=?, product_category_id=? WHERE id=?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, product.name());
                ps.setInt(2, product.countInStock());
                ps.setDouble(3, product.price().doubleValue());
                ps.setInt(4, product.productCategoryId());
                ps.setInt(5, id);

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected == 0)
                    throw new IllegalArgumentException("The item with ID " + id + " was not found");
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error updating product", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM product WHERE id=?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rowsAffected = ps.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error deleting product", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
    }

    @Override
    public Product get(int id) {
        String sql = "SELECT * FROM product WHERE id=?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        return mapResultSetToProduct(rs);
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error fetching product by id: " + id, e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
        return null;
    }

    public void setProductPrice(int id, BigDecimal price) {
        String sql = "UPDATE product SET price=? WHERE id=?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, price.doubleValue());
                ps.setInt(2, id);

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected == 0) {
                    throw new IllegalArgumentException("The product with ID " + id + " was not found");
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error updating product price", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
    }

    public void addStock(int productId, int amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be greater than 0");

        String sql = "UPDATE product SET count_in_stock = count_in_stock + ? WHERE id=?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setInt(2, productId);

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected == 0) {
                    throw new IllegalArgumentException("The product with ID " + productId + " was not found");
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error adding stock", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
    }

    public boolean deductStock(int productId, int amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be greater than 0");

        String sql = "UPDATE product SET count_in_stock = count_in_stock - ? WHERE id=? AND count_in_stock >= ?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setInt(2, productId);
                ps.setInt(3, amount);

                int rowsAffected = ps.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error deducting stock", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
    }

    public PageResponse<Product> searchProducts(SearchProductsRequest request) {
        ProductFilterDTO filter = request != null ? request.filter() : null;
        PaginationDTO pagination = request != null ? request.pagination() : null;
        SortDTO sort = request != null ? request.sort() : null;

        SqlQueryBuilder dataBuilder = new SqlQueryBuilder("SELECT * FROM product");
        SqlQueryBuilder countBuilder = new SqlQueryBuilder("SELECT COUNT(*) FROM product");

        if (filter != null) {
            dataBuilder.whereILike("name", filter.nameLike())
                    .whereGreaterOrEqual("price", filter.minPrice())
                    .whereLessOrEqual("price", filter.maxPrice())
                    .whereEqual("product_category_id", filter.categoryId());

            countBuilder.whereILike("name", filter.nameLike())
                    .whereGreaterOrEqual("price", filter.minPrice())
                    .whereLessOrEqual("price", filter.maxPrice())
                    .whereEqual("product_category_id", filter.categoryId());
        }

        if (sort != null && sort.column() != null) {
            if ("DESC".equalsIgnoreCase(sort.direction()))
                dataBuilder.orderByDesc(sort.column());
            else
                dataBuilder.orderByAsc(sort.column());
        } else {
            dataBuilder.orderByAsc("id");
        }

        if (pagination != null) {
            dataBuilder.paginate(pagination.page(), pagination.size());
        }

        List<Product> products = fetchListWithBuilder(dataBuilder.getSql(), dataBuilder.getParams());
        int totalElements = fetchCountWithBuilder(countBuilder.getSql(), countBuilder.getParams());

        int currentPage = (pagination != null && pagination.page() != null) ? pagination.page() : 1;
        int pageSize = (pagination != null && pagination.size() != null) ? pagination.size() : Math.max(totalElements, 1);
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;

        return new PageResponse<>(products, totalElements, totalPages, currentPage);
    }

    private List<Product> fetchListWithBuilder(String sql, List<Object> params) {
        List<Product> products = new ArrayList<>();
        Connection conn = null;
        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                setParameters(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        products.add(mapResultSetToProduct(rs));
                    }
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error searching products", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
        return products;
    }

    private int fetchCountWithBuilder(String sql, List<Object> params) {
        Connection conn = null;
        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                setParameters(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error counting products", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
        return 0;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("count_in_stock"),
                BigDecimal.valueOf(rs.getDouble("price")),
                rs.getInt("product_category_id")
        );
    }
}