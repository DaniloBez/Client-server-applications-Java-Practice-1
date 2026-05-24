package repository;

import entity.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProductCategoryRepositoryTest {
    private ProductCategoryRepository productCategoryRepository;

    @BeforeEach
    public void beforeEach() {
        productCategoryRepository = new ProductCategoryRepository();
    }

    @Test
    public void shouldIncrementId() {
        int id = productCategoryRepository.create("Category1");

        assertEquals(id + 1, productCategoryRepository.create("Category2"));
        assertEquals(id + 2, productCategoryRepository.create("Category3"));
    }

    @Test
    public void shouldFindById() {
        int id = productCategoryRepository.create("Category1");
        ProductCategory productCategory = productCategoryRepository.get(id);

        assertNotNull(productCategory);
        assertEquals(id, productCategory.getId());
        assertEquals("Category1", productCategory.getName().get());
    }

    @Test
    public void shouldDeleteById() {
        int id = productCategoryRepository.create("Category1");
        ProductCategory productCategory = productCategoryRepository.delete(id);

        assertNotNull(productCategory);
        assertEquals(id, productCategory.getId());
        assertEquals("Category1", productCategory.getName().get());

        assertNull(productCategoryRepository.get(id));
        assertNull(productCategoryRepository.delete(id));
    }

    @Test
    public void shouldGetAll() {
        productCategoryRepository.create("Category1");
        productCategoryRepository.create("Category2");
        productCategoryRepository.create("Category3");

        assertEquals(3, productCategoryRepository.getAll().size());
    }
}
