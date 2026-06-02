package repository;

import entity.Product;
import entity.ProductCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class ProductRepositoryTest extends BaseRepositoryTest {

    @Test
    public void shouldFindById() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );

        Product product = productRepository.get(id);

        assertNotNull(product);
        assertEquals(id, product.id());
        assertEquals("product1", product.name());
        assertEquals(10, product.countInStock());
        assertEquals(new BigDecimal("100.0"), product.price());
        assertEquals(categoryId, product.productCategoryId());
    }

    @Test
    public void shouldDeleteById() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );


        assertTrue(productRepository.delete(id));
        assertNull(productRepository.get(id));
        assertFalse(productRepository.delete(id));
    }

    @Test
    public void shouldCheckPresenceInCategory() {
        int categoryId1 = productCategoryRepository.create(new ProductCategory(0, "Category for Product1"));
        int categoryId2 = productCategoryRepository.create(new ProductCategory(0, "Category for Product2"));
        productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId1
                )
        );

        productRepository.create(
                new Product(
                        0,
                        "product2",
                        10,
                        new BigDecimal(100),
                        categoryId1
                )
        );

        productRepository.create(
                new Product(
                        0,
                        "product3",
                        10,
                        new BigDecimal(100),
                        categoryId2
                )
        );

        assertFalse(productRepository.getAllByCategoryId(categoryId1).isEmpty());
        assertFalse(productRepository.getAllByCategoryId(categoryId2).isEmpty());
        assertTrue(productRepository.getAllByCategoryId(categoryId2 + categoryId1).isEmpty());
    }

    @Test
    public void shouldFindAllInCategory() {
        int categoryId1 = productCategoryRepository.create(new ProductCategory(0, "Category for Product1"));
        int categoryId2 = productCategoryRepository.create(new ProductCategory(0, "Category for Product2"));
        productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId1
                )
        );

        productRepository.create(
                new Product(
                        0,
                        "product2",
                        10,
                        new BigDecimal(100),
                        categoryId1
                )
        );

        productRepository.create(
                new Product(
                        0,
                        "product3",
                        10,
                        new BigDecimal(100),
                        categoryId2
                )
        );

        List<Product> products = productRepository.getAllByCategoryId(categoryId1);
        assertNotNull(products);
        assertEquals(2, products.size());

        products = productRepository.getAllByCategoryId(categoryId2);
        assertNotNull(products);
        assertEquals(1, products.size());

        products = productRepository.getAllByCategoryId(categoryId2 + categoryId1);
        assertNotNull(products);
        assertEquals(0, products.size());
    }

    @Test
    public void shouldAddStock() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );

        assertEquals(10, productRepository.get(id).countInStock());

        productRepository.addStock(id, 10);
        assertEquals(20, productRepository.get(id).countInStock());

        productRepository.addStock(id, 30);
        assertEquals(50, productRepository.get(id).countInStock());
    }

    @Test
    public void shouldThrowErrorAndNotAddNegativeStock() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );

        assertThatThrownBy(() -> productRepository.addStock(id,-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 0");

        assertEquals(10, productRepository.get(id).countInStock());
    }

    @Test
    public void shouldDeductStock() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );

        assertTrue(productRepository.deductStock(id,5));
        assertEquals(5, productRepository.get(id).countInStock());

        assertTrue(productRepository.deductStock(id,5));
        assertEquals(0, productRepository.get(id).countInStock());
    }

    @Test
    public void shouldThrowErrorAndNotDeductNegativeStock() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );

        assertThatThrownBy(() -> productRepository.deductStock(id,-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 0");

        assertEquals(10, productRepository.get(id).countInStock());
    }

    @Test
    public void shouldNotDeductIfLessThenAmount() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );

        assertFalse(productRepository.deductStock(id,15));
        assertEquals(10, productRepository.get(id).countInStock());
    }
}
