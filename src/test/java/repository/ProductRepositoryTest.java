package repository;

import entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class ProductRepositoryTest {
    private ProductRepository productRepository;

    @BeforeEach
    public void setup(){
        productRepository = new ProductRepository();
    }

    @Test
    public void shouldIncrementId() {
        int id = productRepository.create(
                "product1",
                10,
                new BigDecimal(100),
                1
        );

        assertEquals(id + 1, productRepository.create(
                        "product2",
                        10,
                        new BigDecimal(100),
                        1
                )
        );

        assertEquals(id + 2, productRepository.create(
                        "product3",
                        10,
                        new BigDecimal(100),
                        1
                )
        );
    }

    @Test
    public void shouldFindById() {
        int id = productRepository.create(
                "product1",
                10,
                new BigDecimal(100),
                1
        );

        Product product = productRepository.get(id);

        assertNotNull(product);
        assertEquals(id, product.getId());
        assertEquals("product1", product.getName().get());
        assertEquals(10, product.getCountInStock().get());
        assertEquals(new BigDecimal(100), product.getPrice().get());
        assertEquals(1, product.getProductCategoryId().get());
    }

    @Test
    public void shouldDeleteById() {
        int id = productRepository.create(
                "product1",
                10,
                new BigDecimal(100),
                1
        );

        Product product = productRepository.delete(id);
        assertNotNull(product);
        assertEquals(id, product.getId());
        assertEquals("product1", product.getName().get());
        assertEquals(10, product.getCountInStock().get());
        assertEquals(new BigDecimal(100), product.getPrice().get());
        assertEquals(1, product.getProductCategoryId().get());

        assertNull(productRepository.get(id));
        assertNull(productRepository.delete(id));
    }

    @Test
    public void shouldCheckPresenceInCategory() {
        productRepository.create(
                "product1",
                10,
                new BigDecimal(100),
                1
        );

        productRepository.create(
                "product2",
                10,
                new BigDecimal(100),
                1
        );

        productRepository.create(
                "product3",
                10,
                new BigDecimal(100),
                2
        );

        assertTrue(productRepository.hasProductsInCategory(1));
        assertTrue(productRepository.hasProductsInCategory(2));
        assertFalse(productRepository.hasProductsInCategory(3));
    }

    @Test
    public void shouldFindAllInCategory() {
        productRepository.create(
                "product1",
                10,
                new BigDecimal(100),
                1
        );

        productRepository.create(
                "product2",
                10,
                new BigDecimal(100),
                1
        );

        productRepository.create(
                "product3",
                10,
                new BigDecimal(100),
                2
        );

        List<Product> products = productRepository.getAllByCategoryId(1);
        assertNotNull(products);
        assertEquals(2, products.size());

        products = productRepository.getAllByCategoryId(2);
        assertNotNull(products);
        assertEquals(1, products.size());

        products = productRepository.getAllByCategoryId(3);
        assertNotNull(products);
        assertEquals(0, products.size());
    }

    @Test
    public void shouldAddStock() {
        Product p = new Product(
                "product1",
                10,
                new BigDecimal(100),
                1
        );

        assertEquals(10, p.getCountInStock().get());

        p.addStock(10);
        assertEquals(20, p.getCountInStock().get());

        p.addStock(30);
        assertEquals(50, p.getCountInStock().get());
    }

    @Test
    public void shouldThrowErrorAndNotAddNegativeStock() {
        Product p = new Product(
                "product1",
                10,
                new BigDecimal(100),
                1
        );

        assertThatThrownBy(() -> p.addStock(-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 0");

        assertEquals(10, p.getCountInStock().get());
    }

    @Test
    public void shouldDeductStock() {
        Product p = new Product(
                "product1",
                10,
                new BigDecimal(100),
                1
        );

        assertTrue(p.deductStock(5));
        assertEquals(5, p.getCountInStock().get());

        assertTrue(p.deductStock(5));
        assertEquals(0, p.getCountInStock().get());
    }

    @Test
    public void shouldThrowErrorAndNotDeductNegativeStock() {
        Product p = new Product(
                "product1",
                10,
                new BigDecimal(100),
                1
        );

        assertThatThrownBy(() -> p.deductStock(-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 0");

        assertEquals(10, p.getCountInStock().get());
    }

    @Test
    public void shouldNotDeductIfLessThenAmount() {
        Product p = new Product(
                "product1",
                10,
                new BigDecimal(100),
                1
        );

        assertFalse(p.deductStock(15));
        assertEquals(10, p.getCountInStock().get());
    }
}
