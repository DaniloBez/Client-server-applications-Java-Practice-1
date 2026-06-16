package server;

import client.IClient;
import client.StoreClientTCP;
import client.StoreClientUDP;
import decryptor.IDecryptor;
import decryptor.MessageDecryptor;
import dto.Message;
import encryptor.MessageEncryptor;
import entity.Product;
import entity.ProductCategory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import processor.IProcessor;
import processor.Processor;
import repository.ProductCategoryRepository;
import repository.ProductRepository;
import service.ProductCategoryService;
import service.ProductService;
import utils.DBConnectionPool;

import java.math.BigDecimal;
import java.util.function.Consumer;

@Testcontainers
public abstract class BaseIntegrationTest {
    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    protected int testCategoryId;
    protected int testProductId;

    private DBConnectionPool pool;

    private ProductCategoryService categoryService;
    private ProductService productService;
    private service.UserService userService;

    @BeforeEach
    void setUp() {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();

        pool = new DBConnectionPool(20, postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        ProductCategoryRepository categoryRepository = new ProductCategoryRepository(pool);
        ProductRepository productRepository = new ProductRepository(pool);
        repository.UserRepository userRepository = new repository.UserRepository(pool);

        categoryService = new ProductCategoryService(categoryRepository, productRepository);
        productService = new ProductService(productRepository, categoryRepository);
        userService = new service.UserService(userRepository, com.auth0.jwt.algorithms.Algorithm.HMAC256("secret"));

        try {
            userService.register("testuser", "testpass");
        } catch (IllegalArgumentException ignored) {}

        testCategoryId = categoryRepository.create(new ProductCategory(0, "Electronics"));
        testProductId = productRepository.create(new Product(0, "Product", 10, new BigDecimal(10), testCategoryId));
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.closeAll();
        }
    }

    protected Server initServer(int senderCount, int decryptorCount, int encryptorCount, int processorCount) {
        IDecryptor serverDecryptor = new MessageDecryptor();
        MessageEncryptor serverEncryptor = new MessageEncryptor();

        IProcessor serverProcessor = new Processor(productService, categoryService);

        return new Server(
                senderCount, 
                serverDecryptor, 
                decryptorCount, 
                serverEncryptor, 
                encryptorCount, 
                serverProcessor, 
                processorCount, 
                10000, 
                8080, 
                userService, 
                productService
        );
    }

    protected IClient getClient(int id, Consumer<Message> consumer) {
        if (id % 2 == 0)
            return new StoreClientTCP(new MessageEncryptor(), new MessageDecryptor(), consumer);
        else
            return new StoreClientUDP(new MessageEncryptor(), new MessageDecryptor(), consumer);
    }
}
