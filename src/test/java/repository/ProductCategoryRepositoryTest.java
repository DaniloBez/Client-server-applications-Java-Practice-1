package repository;

import entity.ProductCategory;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class ProductCategoryRepositoryTest extends BaseRepositoryTest {

    @Test
    public void shouldFindById() {
        int id = productCategoryRepository.create(new ProductCategory(0, "Category1"));

        ProductCategory productCategory = productCategoryRepository.get(id);

        assertNotNull(productCategory);
        assertEquals(id, productCategory.id());
        assertEquals("Category1", productCategory.name());
    }

    @Test
    public void shouldDeleteById() {
        int id = productCategoryRepository.create(new ProductCategory(0, "CategoryToDelete"));

        boolean isDeleted = productCategoryRepository.delete(id);
        assertTrue(isDeleted);

        assertNull(productCategoryRepository.get(id));

        boolean isDeletedAgain = productCategoryRepository.delete(id);
        assertFalse(isDeletedAgain);
    }

    @Test
    public void shouldGetAll() {
        productCategoryRepository.create(new ProductCategory(0, "Category1"));
        productCategoryRepository.create(new ProductCategory(0, "Category2"));
        productCategoryRepository.create(new ProductCategory(0, "Category3"));

        int size = productCategoryRepository.getAll().size();

        assertEquals(3, size);
    }

    @Test
    public void shouldFailToCreateDuplicateName() {
        productCategoryRepository.create(new ProductCategory(0, "UniqueName"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> productCategoryRepository.create(new ProductCategory(0, "UniqueName")));

        Throwable cause = exception.getCause();
        assertNotNull(cause);
        assertInstanceOf(SQLException.class, cause);

        assertEquals("23505", ((SQLException) cause).getSQLState(),
                "SQL state should match PostgreSQL unique violation code");
    }
}
