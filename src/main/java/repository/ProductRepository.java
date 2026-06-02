package repository;

import entity.Product;
import utils.DBConnectionPool;

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

    @Override
    public List<Product> getAll() {
        String sql = "SELECT * FROM product";
        return fetchList(sql, null);
    }

    public List<Product> getAllByCategoryId(int categoryId) {
        String sql = "SELECT * FROM product WHERE product_category_id=?";
        return fetchList(sql, categoryId);
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


    private List<Product> fetchList(String sql, Integer categoryIdParam) {
        List<Product> products = new ArrayList<>();
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (categoryIdParam != null)
                    ps.setInt(1, categoryIdParam);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        products.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error fetching products list", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
        return products;
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