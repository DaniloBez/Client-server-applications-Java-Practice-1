package service;

import entity.ProductCategory;
import repository.ProductCategoryRepository;
import repository.ProductRepository;

import java.sql.SQLException;
import java.util.List;

public class ProductCategoryService {
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public ProductCategoryService(ProductCategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public int createCategory(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("The category name cannot be empty");

        try {
            return categoryRepository.create(new ProductCategory(0, name));

        } catch (RuntimeException e) {
            if (isUniqueConstraintViolation(e))
                throw new IllegalArgumentException(String.format("The category with name '%s' already exists", name));

            throw e;
        }
    }

    public ProductCategory getCategory(int id) {
        ProductCategory category = categoryRepository.get(id);
        if (category == null)
            throw new IllegalArgumentException("The category with ID " + id + " was not found");

        return category;
    }

    public void updateCategoryName(int id, String newName) {
        if (newName == null || newName.trim().isEmpty())
            throw new IllegalArgumentException("The category name cannot be empty");

        try {
            categoryRepository.update(id, new ProductCategory(0, newName));

        } catch (RuntimeException e) {
            if (isUniqueConstraintViolation(e))
                throw new IllegalArgumentException(String.format("The category with name '%s' already exists", newName));
            throw e;
        }
    }

    public boolean deleteCategory(int id) {
        if (!productRepository.getAllByCategoryId(id).isEmpty())
            throw new IllegalStateException("You cannot delete the group: it still contains items");

        return categoryRepository.delete(id);
    }

    public List<ProductCategory> getAllCategories() {
        return categoryRepository.getAll();
    }

    private boolean isUniqueConstraintViolation(RuntimeException e) {
        Throwable cause = e.getCause();
        if (cause instanceof SQLException sqlException)
            return "23505".equals(sqlException.getSQLState());

        return false;
    }
}