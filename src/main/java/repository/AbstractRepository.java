package repository;

import utils.DBConnectionPool;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public abstract class AbstractRepository<Entity> {
    protected final DBConnectionPool dbConnectionPool;

    public AbstractRepository(DBConnectionPool dbConnectionPool) {
        this.dbConnectionPool = dbConnectionPool;
    }

    public abstract int create(Entity entity);

    public abstract void update(int id, Entity entity);

    public abstract boolean delete(int id);

    public abstract Entity get(int id);

    protected void setParameters(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }
    }
}
