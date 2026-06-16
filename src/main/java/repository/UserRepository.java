package repository;

import entity.User;
import utils.DBConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserRepository extends AbstractRepository<User> {
    public UserRepository(DBConnectionPool dbConnectionPool) {
        super(dbConnectionPool);
    }

    @Override
    public int create(User user) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.username());
                ps.setString(2, user.password());

                ps.executeUpdate();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next())
                        return generatedKeys.getInt(1);
                    else
                        throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error creating user", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
    }

    @Override
    public void update(int id, User user) {
        String sql = "UPDATE users SET username = ?, password = ? WHERE id = ?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.username());
                ps.setString(2, user.password());
                ps.setInt(3, id);

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected == 0)
                    throw new IllegalArgumentException("The user with ID " + id + " was not found");
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error updating user", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rowsAffected = ps.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error deleting user", e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
    }

    @Override
    public User get(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error fetching user by id: " + id, e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
        return null;
    }

    public User getByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = null;

        try {
            conn = dbConnectionPool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Error fetching user by username: " + username, e);
        } finally {
            dbConnectionPool.releaseConnection(conn);
        }
        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password")
        );
    }
}
