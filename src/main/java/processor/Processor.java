package processor;

import dto.Message;
import dto.request.*;
import dto.response.*;
import service.ProductCategoryService;
import service.ProductService;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.function.Function;

public class Processor implements IProcessor {
    private final HashMap<Integer, Function<Message, Message>> router;
    private final ObjectMapper objectMapper;

    private final ProductService productService;
    private final ProductCategoryService categoryService;

    public Processor(ProductService productService, ProductCategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;

        objectMapper = new ObjectMapper();
        router = new HashMap<>();

        initRoutes();
    }

    private void initRoutes() {
        router.put(1, this::handleCreateCategory);
        router.put(2, this::handleCreateProduct);
        router.put(3, this::handleGetStockQuantity);
        router.put(4, this::handleAddStock);
        router.put(5, this::handleDeductStock);
        router.put(6, this::handleSetProductPrice);
        router.put(7, this::handleGetCategory);
        router.put(8, this::handleUpdateCategoryName);
        router.put(9, this::handleDeleteCategory);
        router.put(10, this::handleGetAllCategories);
        router.put(11, this::handleGetProduct);
        router.put(12, this::handleDeleteProduct);
        router.put(13, this::handleGetProductsByCategory);
    }

    @Override
    public Message process(Message message) {
        try {
            Function<Message, Message> handler = router.get(message.getCommandId());

            if (handler == null)
                return buildErrorMessage(message, 404, "Route Not Found", "Unknown command ID: " + message.getCommandId());

            return handler.apply(message);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return buildErrorMessage(message, 400, "Bad Request", e.getMessage());
        } catch (Exception e) {
            return buildErrorMessage(message, 500, "Internal Server Error", "An unexpected error occurred");
        }
    }

    private Message buildErrorMessage(Message originalMessage, int errorCommandId, String errorType, String errorMessageText) {
        try {
            ErrorResponse errorResponse = new ErrorResponse(errorType, errorMessageText);
            String jsonPayload = objectMapper.writeValueAsString(errorResponse);

            return new Message(
                    originalMessage.getClientApplicationId(),
                    originalMessage.getMessageId(),
                    errorCommandId,
                    originalMessage.getUserId(),
                    jsonPayload
            );
        } catch (Exception jsonEx) {
            return new Message(originalMessage.getClientApplicationId(), originalMessage.getMessageId(), 500, originalMessage.getUserId(), "{\"error\": \"Critical serialization error\"}");
        }
    }

    private Message handleCreateCategory(Message message) {
        CreateCategoryRequest request = objectMapper.readValue(message.getData(), CreateCategoryRequest.class);

        int id = categoryService.createCategory(request.name());
        CreateResourceResponse response = new CreateResourceResponse(id);

        return new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }

    private Message handleCreateProduct(Message message) {
        CreateProductRequest request = objectMapper.readValue(message.getData(), CreateProductRequest.class);

        int id = productService.createProduct(
                request.name(),
                request.initialStock(),
                request.price(),
                request.categoryId()
        );

        CreateResourceResponse response = new CreateResourceResponse(id);

        return new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }

    private Message handleGetStockQuantity(Message message) {
        GetStockRequest request = objectMapper.readValue(message.getData(), GetStockRequest.class);

        int quantity = productService.getStockQuantity(request.productId());
        StockQuantityResponse response = new StockQuantityResponse(request.productId(), quantity);

        return new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }

    private Message handleAddStock(Message message) {
        AddStockRequest request = objectMapper.readValue(message.getData(), AddStockRequest.class);

        productService.addStock(request.productId(), request.amount());
        ActionStatusResponse response = new ActionStatusResponse(true, "Stock added successfully");

        return new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }

    private Message handleDeductStock(Message message) {
        DeductStockRequest request = objectMapper.readValue(message.getData(), DeductStockRequest.class);

        productService.deductStock(request.productId(), request.amount());
        ActionStatusResponse response = new ActionStatusResponse(true, "Stock deducted successfully");

        return new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }

    private Message handleSetProductPrice(Message message) {
        SetPriceRequest request = objectMapper.readValue(message.getData(), SetPriceRequest.class);

        productService.setProductPrice(request.productId(), request.newPrice());
        ActionStatusResponse response = new ActionStatusResponse(true, "Price updated successfully");

        return new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }

    private Message handleGetCategory(Message message) {
        GetByIdRequest request = objectMapper.readValue(message.getData(), GetByIdRequest.class);

        var category = categoryService.getCategory(request.id());
        CategoryResponse response = new CategoryResponse(category.getId(), category.getName().get());

        return new Message(
                message.getClientApplicationId(), message.getMessageId(), 200, message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }

    private Message handleUpdateCategoryName(Message message) {
        UpdateCategoryRequest request = objectMapper.readValue(message.getData(), UpdateCategoryRequest.class);

        categoryService.updateCategoryName(request.id(), request.newName());
        ActionStatusResponse response = new ActionStatusResponse(true, "Category updated successfully");

        return new Message(
                message.getClientApplicationId(), message.getMessageId(), 200, message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }

    private Message handleDeleteCategory(Message message) {
        GetByIdRequest request = objectMapper.readValue(message.getData(), GetByIdRequest.class);

        categoryService.deleteCategory(request.id());
        ActionStatusResponse response = new ActionStatusResponse(true, "Category deleted successfully");

        return new Message(
                message.getClientApplicationId(), message.getMessageId(), 200, message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }

    private Message handleGetAllCategories(Message message) {
        var categories = categoryService.getAllCategories();

        var categoryResponses = categories.stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName().get()))
                .toList();

        AllCategoriesResponse response = new AllCategoriesResponse(categoryResponses);

        return new Message(
                message.getClientApplicationId(), message.getMessageId(), 200, message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }

    private Message handleGetProduct(Message message) {
        GetByIdRequest request = objectMapper.readValue(message.getData(), GetByIdRequest.class);

        var product = productService.getProduct(request.id());

        ProductResponse response = new ProductResponse(
                product.getId(),
                product.getName().get(),
                product.getCountInStock().get(),
                product.getPrice().get(),
                product.getProductCategoryId().get()
        );

        return new Message(
                message.getClientApplicationId(), message.getMessageId(), 200, message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }

    private Message handleDeleteProduct(Message message) {
        GetByIdRequest request = objectMapper.readValue(message.getData(), GetByIdRequest.class);

        boolean isSuccessful = productService.deleteProduct(request.id());
        ActionStatusResponse response = new ActionStatusResponse(true, "Product deleted successfully");

        if (!isSuccessful)
            response = new ActionStatusResponse(false, "Product was already deleted");

        return new Message(
                message.getClientApplicationId(), message.getMessageId(), 200, message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }

    private Message handleGetProductsByCategory(Message message) {
        GetByIdRequest request = objectMapper.readValue(message.getData(), GetByIdRequest.class);

        var products = productService.getProductsByCategory(request.id());

        var productResponses = products.stream()
                .map(product -> new ProductResponse(
                        product.getId(),
                        product.getName().get(),
                        product.getCountInStock().get(),
                        product.getPrice().get(),
                        product.getProductCategoryId().get()
                ))
                .toList();

        AllProductsResponse response = new AllProductsResponse(productResponses);

        return new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                objectMapper.writeValueAsString(response)
        );
    }
}
