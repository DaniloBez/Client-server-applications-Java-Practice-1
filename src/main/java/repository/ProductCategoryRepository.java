package repository;

import entity.ProductCategory;
import utils.DBConnectionPool;

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
                        throw new SQLException("Creating product failed, no ID obtained.");
                }
            }
        }
        catch (SQLException | InterruptedException e) {
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
                    throw new IllegalArgumentException("Категорію з ID " + id + " не знайдено");
            }
        }
        catch (SQLException | InterruptedException e) {
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
        }
        catch (SQLException | InterruptedException e) {
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

    @Override
    public List<ProductCategory> getAll() {
        String sql = "SELECT * FROM product_category";
        Connection conn = null;

        List<ProductCategory> categories = new ArrayList<>();

        try {
            conn = dbConnectionPool.getConnection();

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    categories.add(new ProductCategory(
                            rs.getInt("id"),
                            rs.getString("name")
                    ));
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error fetching all categories", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }

        return categories;
    }
}
