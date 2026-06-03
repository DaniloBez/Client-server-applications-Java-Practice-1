package repository;

import dto.request.CategoryFilterDTO;
import dto.request.PaginationDTO;
import dto.request.SearchCategoriesRequest;
import dto.request.SortDTO;
import dto.response.PageResponse;
import entity.ProductCategory;
import utils.DBConnectionPool;
import utils.SqlQueryBuilder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductCategoryRepository extends AbstractRepository<ProductCategory> {
    public ProductCategoryRepository(DBConnectionPool dbConnectionPool) {
        super(dbConnectionPool);
    }

    @Override
    public int create(ProductCategory productCategory) {
        String sql = "INSERT INTO product_category (name) VALUES (?)";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, productCategory.name());

                ps.execute();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next())
                        return generatedKeys.getInt(1);
                    else
                        throw new SQLException("Creating category failed, no ID obtained.");
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error creating category", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
    }

    @Override
    public void update(int id, ProductCategory productCategory) {
        String sql = "UPDATE product_category SET name=? WHERE id=?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try(PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, productCategory.name());
                ps.setInt(2, id);

                int rowsAffected = ps.executeUpdate();

                if (rowsAffected == 0)
                    throw new IllegalArgumentException("The category with ID " + id + " was not found");
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error updating category", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM product_category WHERE id=?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try(PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);

                int rowsAffected = ps.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error deleting category", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
    }

    @Override
    public ProductCategory get(int id) {
        String sql = "SELECT * FROM product_category WHERE id=?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new ProductCategory(
                                rs.getInt("id"),
                                rs.getString("name")
                        );
                    }
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error fetching category by id: " + id, e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }

        return null;
    }

    public PageResponse<ProductCategory> searchCategories(SearchCategoriesRequest request) {
        CategoryFilterDTO filter = request != null ? request.filter() : null;
        PaginationDTO pagination = request != null ? request.pagination() : null;
        SortDTO sort = request != null ? request.sort() : null;

        SqlQueryBuilder dataBuilder = new SqlQueryBuilder("SELECT * FROM product_category");
        SqlQueryBuilder countBuilder = new SqlQueryBuilder("SELECT COUNT(*) FROM product_category");

        if (filter != null) {
            dataBuilder.whereILike("name", filter.nameLike());
            countBuilder.whereILike("name", filter.nameLike());
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

        List<ProductCategory> categories = fetchCategoriesWithBuilder(dataBuilder.getSql(), dataBuilder.getParams());
        int totalElements = fetchCountWithBuilder(countBuilder.getSql(), countBuilder.getParams());

        int currentPage = (pagination != null && pagination.page() != null) ? pagination.page() : 1;
        int pageSize = (pagination != null && pagination.size() != null) ? pagination.size() : Math.max(totalElements, 1);
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;

        return new PageResponse<>(categories, totalElements, totalPages, currentPage);
    }

    private List<ProductCategory> fetchCategoriesWithBuilder(String sql, List<Object> params) {
        List<ProductCategory> categories = new ArrayList<>();
        Connection conn = null;
        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                setParameters(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        categories.add(new ProductCategory(
                                rs.getInt("id"),
                                rs.getString("name")
                        ));
                    }
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error searching categories", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
        return categories;
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
            throw new RuntimeException("Error counting categories", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
        return 0;
    }
}