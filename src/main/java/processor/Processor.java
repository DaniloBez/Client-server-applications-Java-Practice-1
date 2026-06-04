package processor;

import dto.Message;
import dto.broadcast.*;
import dto.request.*;
import dto.response.*;
import entity.Product;
import entity.ProductCategory;
import service.ProductCategoryService;
import service.ProductService;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class Processor implements IProcessor {
    private final HashMap<Integer, Function<Message, List<Message>>> router;
    private final JsonMapper mapper;

    private final ProductService productService;
    private final ProductCategoryService categoryService;

    public static final int BROADCAST_USER_ID = -1;

    public Processor(ProductService productService, ProductCategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;

        mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

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
        router.put(10, this::handleSearchCategories);
        router.put(11, this::handleGetProduct);
        router.put(12, this::handleDeleteProduct);
        router.put(13, this::handleSearchProducts);
    }

    @Override
    public List<Message> process(Message message) {
        try {
            Function<Message, List<Message>> handler = router.get(message.getCommandId());

            if (handler == null)
                return List.of(buildErrorMessage(message, 404, "Route Not Found", "Unknown command ID: " + message.getCommandId()));

            return handler.apply(message);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return List.of(buildErrorMessage(message, 400, "Bad Request", e.getMessage()));
        } catch (Exception e) {
            return List.of(buildErrorMessage(message, 500, "Internal Server Error", e.getMessage()));
        }
    }

    private Message buildErrorMessage(Message originalMessage, int errorCommandId, String errorType, String errorMessageText) {
        try {
            ErrorResponse errorResponse = new ErrorResponse(errorType, errorMessageText);
            String jsonPayload = mapper.writeValueAsString(errorResponse);

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

    private List<Message> handleCreateCategory(Message message) {
        CreateCategoryRequest request = mapper.readValue(message.getData(), CreateCategoryRequest.class);

        int id = categoryService.createCategory(request.name());

        CreateResourceResponse directResponse = new CreateResourceResponse(id);
        Message directMessage = new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                mapper.writeValueAsString(directResponse)
        );

        CategoryCreatedBroadcast broadcastResponse = new CategoryCreatedBroadcast(id, request.name());
        Message broadcastMessage = new Message(
                message.getClientApplicationId(),
                0,
                message.getCommandId(),
                BROADCAST_USER_ID,
                mapper.writeValueAsString(broadcastResponse)
        );

        return List.of(directMessage, broadcastMessage);
    }

    private List<Message> handleCreateProduct(Message message) {
        CreateProductRequest request = mapper.readValue(message.getData(), CreateProductRequest.class);

        int id = productService.createProduct(
                request.name(),
                request.initialStock(),
                request.price(),
                request.categoryId()
        );

        CreateResourceResponse directResponse = new CreateResourceResponse(id);
        Message directMessage = new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                mapper.writeValueAsString(directResponse)
        );

        ProductCreatedBroadcast broadcastResponse = new ProductCreatedBroadcast(
                id,
                request.name(),
                request.initialStock(),
                request.price(),
                request.categoryId()
        );
        Message broadcastMessage = new Message(
                message.getClientApplicationId(),
                0,
                message.getCommandId(),
                BROADCAST_USER_ID,
                mapper.writeValueAsString(broadcastResponse)
        );

        return List.of(directMessage, broadcastMessage);
    }

    private List<Message> handleGetStockQuantity(Message message) {
        GetStockRequest request = mapper.readValue(message.getData(), GetStockRequest.class);

        int quantity = productService.getStockQuantity(request.productId());
        StockQuantityResponse response = new StockQuantityResponse(request.productId(), quantity);

        return List.of( new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                mapper.writeValueAsString(response)
        ));
    }

    private List<Message> handleAddStock(Message message) {
        AddStockRequest request = mapper.readValue(message.getData(), AddStockRequest.class);

        productService.addStock(request.productId(), request.amount());

        ActionStatusResponse directResponse = new ActionStatusResponse(true, "Stock added successfully");
        Message directMessage =  new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                mapper.writeValueAsString(directResponse)
        );

        StockAddedBroadcast broadcastResponse = new StockAddedBroadcast(request.productId(), request.amount());
        Message broadcastMessage = new Message(
                message.getClientApplicationId(),
                0,
                message.getCommandId(),
                BROADCAST_USER_ID,
                mapper.writeValueAsString(broadcastResponse)
        );

        return List.of(directMessage, broadcastMessage);
    }

    private List<Message> handleDeductStock(Message message) {
        DeductStockRequest request = mapper.readValue(message.getData(), DeductStockRequest.class);

        productService.deductStock(request.productId(), request.amount());

        ActionStatusResponse directResponse = new ActionStatusResponse(true, "Stock deducted successfully");
        Message directMessage =  new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                mapper.writeValueAsString(directResponse)
        );

        StockDeductedBroadcast broadcastResponse = new StockDeductedBroadcast(request.productId(), request.amount());
        Message broadcastMessage = new Message(
                message.getClientApplicationId(),
                0,
                message.getCommandId(),
                BROADCAST_USER_ID,
                mapper.writeValueAsString(broadcastResponse)
        );

        return List.of(directMessage, broadcastMessage);
    }

    private List<Message> handleSetProductPrice(Message message) {
        SetPriceRequest request = mapper.readValue(message.getData(), SetPriceRequest.class);

        productService.setProductPrice(request.productId(), request.newPrice());

        ActionStatusResponse directResponse = new ActionStatusResponse(true, "Price updated successfully");
        Message directMessage = new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                mapper.writeValueAsString(directResponse)
        );

        ProductPriceUpdatedBroadcast broadcastResponse = new ProductPriceUpdatedBroadcast(request.productId(), request.newPrice());
        Message broadcastMessage = new Message(
                message.getClientApplicationId(),
                0,
                message.getCommandId(),
                BROADCAST_USER_ID,
                mapper.writeValueAsString(broadcastResponse)
        );

        return List.of(directMessage, broadcastMessage);
    }

    private List<Message> handleGetCategory(Message message) {
        GetByIdRequest request = mapper.readValue(message.getData(), GetByIdRequest.class);

        var category = categoryService.getCategory(request.id());
        CategoryResponse response = new CategoryResponse(category.id(), category.name());

        return List.of( new Message(
                message.getClientApplicationId(), message.getMessageId(), 200, message.getUserId(),
                mapper.writeValueAsString(response)
        ));
    }

    private List<Message> handleUpdateCategoryName(Message message) {
        UpdateCategoryRequest request = mapper.readValue(message.getData(), UpdateCategoryRequest.class);

        categoryService.updateCategoryName(request.id(), request.newName());

        ActionStatusResponse directResponse = new ActionStatusResponse(true, "Category updated successfully");
        Message directMessage = new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                mapper.writeValueAsString(directResponse)
        );

        CategoryNameUpdatedBroadcast broadcastResponse = new CategoryNameUpdatedBroadcast(request.id(), request.newName());
        Message broadcastMessage = new Message(
                message.getClientApplicationId(),
                0,
                message.getCommandId(),
                BROADCAST_USER_ID,
                mapper.writeValueAsString(broadcastResponse)
        );

        return List.of(directMessage, broadcastMessage);
    }

    private List<Message> handleDeleteCategory(Message message) {
        GetByIdRequest request = mapper.readValue(message.getData(), GetByIdRequest.class);

        categoryService.deleteCategory(request.id());

        ActionStatusResponse directResponse = new ActionStatusResponse(true, "Category deleted successfully");
        Message directMessage = new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                mapper.writeValueAsString(directResponse)
        );

        CategoryDeletedBroadcast broadcastResponse = new CategoryDeletedBroadcast(request.id());
        Message broadcastMessage = new Message(
                message.getClientApplicationId(),
                0,
                message.getCommandId(),
                BROADCAST_USER_ID,
                mapper.writeValueAsString(broadcastResponse)
        );

        return List.of(directMessage, broadcastMessage);
    }

    private List<Message> handleSearchProducts(Message message) {
        SearchProductsRequest request;

        if (message.getData() == null || message.getData().trim().isEmpty())
            request = new SearchProductsRequest(null, null, null);
        else
            request = mapper.readValue(message.getData(), SearchProductsRequest.class);

        PageResponse<Product> pageResponse = productService.searchProducts(request);

        return List.of(new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                mapper.writeValueAsString(pageResponse)
        ));
    }

    private List<Message> handleGetProduct(Message message) {
        GetByIdRequest request = mapper.readValue(message.getData(), GetByIdRequest.class);

        var product = productService.getProduct(request.id());

        ProductResponse response = new ProductResponse(
                product.id(),
                product.name(),
                product.countInStock(),
                product.price(),
                product.productCategoryId()
        );

        return List.of( new Message(
                message.getClientApplicationId(), message.getMessageId(), 200, message.getUserId(),
                mapper.writeValueAsString(response)
        ));
    }

    private List<Message> handleDeleteProduct(Message message) {
        GetByIdRequest request = mapper.readValue(message.getData(), GetByIdRequest.class);

        boolean isSuccessful = productService.deleteProduct(request.id());
        ActionStatusResponse directResponse = new ActionStatusResponse(true, "Product deleted successfully");

        if (!isSuccessful)
            directResponse = new ActionStatusResponse(false, "Product was already deleted");

        List<Message> messages = new ArrayList<>(2);

        Message directMessage =  new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                mapper.writeValueAsString(directResponse)
        );

        messages.add(directMessage);

        if (isSuccessful) {
            ProductDeletedBroadcast broadcastResponse = new ProductDeletedBroadcast(request.id());
            Message broadcastMessage = new Message(
                    message.getClientApplicationId(),
                    0,
                    message.getCommandId(),
                    BROADCAST_USER_ID,
                    mapper.writeValueAsString(broadcastResponse)
            );

            messages.add(broadcastMessage);
        }

       return messages;
    }

    private List<Message> handleSearchCategories(Message message) {
        SearchCategoriesRequest request;

        if (message.getData() == null || message.getData().trim().isEmpty())
            request = new SearchCategoriesRequest(null, null, null);
        else
            request = mapper.readValue(message.getData(), SearchCategoriesRequest.class);

        PageResponse<ProductCategory> pageResponse = categoryService.searchCategories(request);

        return List.of(new Message(
                message.getClientApplicationId(),
                message.getMessageId(),
                200,
                message.getUserId(),
                mapper.writeValueAsString(pageResponse)
        ));
    }
}
