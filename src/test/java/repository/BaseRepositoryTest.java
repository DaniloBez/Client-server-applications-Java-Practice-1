package repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import utils.DBConnectionPool;


@Testcontainers
public abstract class BaseRepositoryTest {
    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    protected DBConnectionPool pool;
    protected ProductCategoryRepository productCategoryRepository;
    protected ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();

        pool = new DBConnectionPool(5, postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        productCategoryRepository = new ProductCategoryRepository(pool);
        productRepository = new ProductRepository(pool);
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.closeAll();
        }
    }
}
