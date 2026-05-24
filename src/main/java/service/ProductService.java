package service;

import entity.Product;
import repository.ProductCategoryRepository;
import repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

public class ProductService {
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, ProductCategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public int createProduct(String name, int initialStock, BigDecimal price, int categoryId) {
        if (categoryRepository.get(categoryId) == null)
            throw new IllegalArgumentException("Unable to create a product: the group with ID " + categoryId + " does not exist");

        if (initialStock < 0)
            throw new IllegalArgumentException("The initial quantity cannot be negative");

        return productRepository.create(name, initialStock, price, categoryId);
    }

    public Product getProduct(int productId) {
        Product product = productRepository.get(productId);
        if (product == null)
            throw new IllegalArgumentException("The product with ID " + productId + " was not found");

        return product;
    }

    public void setProductPrice(int productId, BigDecimal newPrice) {
        if (newPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("A price cannot be negative");

        Product product = getProduct(productId);
        product.setPrice(newPrice);
    }

    public int getStockQuantity(int productId) {
        Product product = getProduct(productId);
        return product.getCountInStock().get();
    }

    public void addStock(int productId, int amount) {
        Product product = getProduct(productId);
        product.addStock(amount);
    }

    public void deductStock(int productId, int amount) {
        Product product = getProduct(productId);

        boolean success = product.deductStock(amount);

        if (!success)
            throw new IllegalStateException("There are not enough items in stock to deduct " + amount + " units");
    }

    public boolean deleteProduct(int productId) {
        return productRepository.delete(productId) != null;
    }

    public List<Product> getProductsByCategory(int categoryId) {
        if (categoryRepository.get(categoryId) == null) {
            throw new IllegalArgumentException("Unable to get products: the group with ID " + categoryId + " does not exist");
        }

        return productRepository.getAllByCategoryId(categoryId);
    }
}
