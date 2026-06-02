package repository;

import utils.DBConnectionPool;

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

    public abstract List<Entity> getAll();
}
