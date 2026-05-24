package service;

import entity.ProductCategory;
import repository.ProductCategoryRepository;
import repository.ProductRepository;

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

        return categoryRepository.create(name);
    }

    public ProductCategory getCategory(int id) {
        ProductCategory category = categoryRepository.get(id);
        if (category == null)
            throw new IllegalArgumentException("The category with ID " + id + " was not found");

        return category;
    }

    public void updateCategoryName(int id, String newName) {
        categoryRepository.update(id, newName);
    }

    public boolean deleteCategory(int id) {
        getCategory(id);

        if (productRepository.hasProductsInCategory(id))
            throw new IllegalStateException("You cannot delete the group: it still contains items");

        return categoryRepository.delete(id) != null;
    }

    public List<ProductCategory> getAllCategories() {
        return categoryRepository.getAll();
    }
}