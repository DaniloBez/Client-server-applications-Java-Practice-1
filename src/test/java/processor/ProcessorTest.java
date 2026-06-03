package processor;

import dto.Message;
import dto.response.PageResponse;
import entity.Product;
import entity.ProductCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.ProductCategoryService;
import service.ProductService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProcessorTest {
    @Mock
    private ProductService productService;

    @Mock
    private ProductCategoryService categoryService;

    @InjectMocks
    private Processor processor;

    private Message createRequest(int commandId, String json) {
        return new Message((byte) 1, 999L, commandId, 42, json);
    }

    @Test
    public void shouldProcessCreateCategory() {
        when(categoryService.createCategory("Electronics")).thenReturn(10);

        List<Message> responses = processor.process(createRequest(1, "{\"name\":\"Electronics\"}"));

        assertEquals(2, responses.size());

        assertEquals(200, responses.get(0).getCommandId());
        assertTrue(responses.get(0).getData().contains("\"id\":10"));

        Message broadcast = responses.get(1);
        assertEquals(Processor.BROADCAST_USER_ID, broadcast.getUserId());
        assertEquals(1, broadcast.getCommandId());
        assertTrue(broadcast.getData().contains("10"));
    }

    @Test
    public void shouldProcessUpdateCategoryName() {
        List<Message> responses = processor.process(createRequest(8, "{\"id\":1,\"newName\":\"Drinks\"}"));

        assertEquals(2, responses.size());

        assertEquals(200, responses.getFirst().getCommandId());
        assertTrue(responses.get(0).getData().contains("true"));
        verify(categoryService, times(1)).updateCategoryName(1, "Drinks");

        Message broadcast = responses.get(1);
        assertEquals(Processor.BROADCAST_USER_ID, broadcast.getUserId());
        assertEquals(8, broadcast.getCommandId());
        assertTrue(broadcast.getData().contains("Drinks"));
    }

    @Test
    public void shouldProcessDeleteCategory() {
        when(categoryService.deleteCategory(1)).thenReturn(true);

        List<Message> responses = processor.process(createRequest(9, "{\"id\":1}"));

        assertEquals(2, responses.size());

        assertEquals(200, responses.get(0).getCommandId());
        verify(categoryService, times(1)).deleteCategory(1);

        Message broadcast = responses.get(1);
        assertEquals(Processor.BROADCAST_USER_ID, broadcast.getUserId());
        assertEquals(9, broadcast.getCommandId());
    }

    @Test
    public void shouldProcessCreateProduct() {
        when(productService.createProduct(eq("Laptop"), eq(50), any(BigDecimal.class), eq(1))).thenReturn(5);

        List<Message> responses = processor.process(createRequest(2,
                "{\"name\":\"Laptop\",\"initialStock\":50,\"price\":1500.00,\"categoryId\":1}"));

        assertEquals(2, responses.size());

        assertEquals(200, responses.get(0).getCommandId());
        assertTrue(responses.get(0).getData().contains("\"id\":5"));

        Message broadcast = responses.get(1);
        assertEquals(Processor.BROADCAST_USER_ID, broadcast.getUserId());
        assertEquals(2, broadcast.getCommandId());
        assertTrue(broadcast.getData().contains("Laptop"));
    }

    @Test
    public void shouldProcessAddStock() {
        List<Message> responses = processor.process(createRequest(4, "{\"productId\":10,\"amount\":20}"));

        assertEquals(2, responses.size());

        assertEquals(200, responses.get(0).getCommandId());
        verify(productService, times(1)).addStock(10, 20);

        Message broadcast = responses.get(1);
        assertEquals(Processor.BROADCAST_USER_ID, broadcast.getUserId());
        assertEquals(4, broadcast.getCommandId());
        assertTrue(broadcast.getData().contains("20")); // amount added
    }

    @Test
    public void shouldProcessDeductStock() {
        List<Message> responses = processor.process(createRequest(5, "{\"productId\":10,\"amount\":5}"));

        assertEquals(2, responses.size());

        assertEquals(200, responses.get(0).getCommandId());
        verify(productService, times(1)).deductStock(10, 5);

        Message broadcast = responses.get(1);
        assertEquals(Processor.BROADCAST_USER_ID, broadcast.getUserId());
        assertEquals(5, broadcast.getCommandId());
        assertTrue(broadcast.getData().contains("5")); // amount deducted
    }

    @Test
    public void shouldProcessSetProductPrice() {
        List<Message> responses = processor.process(createRequest(6, "{\"productId\":10,\"newPrice\":99.99}"));

        assertEquals(2, responses.size());

        assertEquals(200, responses.get(0).getCommandId());
        verify(productService, times(1)).setProductPrice(eq(10), any(BigDecimal.class));

        Message broadcast = responses.get(1);
        assertEquals(Processor.BROADCAST_USER_ID, broadcast.getUserId());
        assertEquals(6, broadcast.getCommandId());
        assertTrue(broadcast.getData().contains("99.99"));
    }

    @Test
    public void shouldProcessDeleteProduct() {
        when(productService.deleteProduct(1)).thenReturn(true);

        List<Message> responses = processor.process(createRequest(12, "{\"id\":1}"));

        assertEquals(2, responses.size());

        assertEquals(200, responses.get(0).getCommandId());
        verify(productService, times(1)).deleteProduct(1);

        Message broadcast = responses.get(1);
        assertEquals(Processor.BROADCAST_USER_ID, broadcast.getUserId());
        assertEquals(12, broadcast.getCommandId());
    }

    @Test
    public void shouldProcessGetCategory() {
        ProductCategory category = new ProductCategory(0,"Food");
        when(categoryService.getCategory(1)).thenReturn(category);

        List<Message> responses = processor.process(createRequest(7, "{\"id\":1}"));

        assertEquals(1, responses.size());
        assertEquals(200, responses.getFirst().getCommandId());
        assertTrue(responses.getFirst().getData().contains("Food"));
    }

    @Test
    public void shouldProcessSearchCategories() {
        ProductCategory cat1 = new ProductCategory(0,"Cat1");
        ProductCategory cat2 = new ProductCategory(0,"Cat2");
        PageResponse<ProductCategory> mockResponse = new PageResponse<>(List.of(cat1, cat2), 2, 1, 1);

        when(categoryService.searchCategories(any())).thenReturn(mockResponse);

        List<Message> responses = processor.process(createRequest(10, "{}"));

        assertEquals(1, responses.size());
        assertEquals(200, responses.getFirst().getCommandId());

        String jsonResponse = responses.getFirst().getData();
        assertTrue(jsonResponse.contains("Cat1"));
        assertTrue(jsonResponse.contains("Cat2"));
        assertTrue(jsonResponse.contains("totalElements"));
        assertTrue(jsonResponse.contains("items"));
    }

    @Test
    public void shouldProcessGetStockQuantity() {
        when(productService.getStockQuantity(10)).thenReturn(404);

        List<Message> responses = processor.process(createRequest(3, "{\"productId\":10}"));

        assertEquals(1, responses.size());
        assertEquals(200, responses.getFirst().getCommandId());
        assertTrue(responses.getFirst().getData().contains("404"));
    }

    @Test
    public void shouldProcessGetProduct() {
        Product mockProduct = new Product(0,"Phone", 10, new BigDecimal("100"), 1);
        when(productService.getProduct(1)).thenReturn(mockProduct);

        List<Message> responses = processor.process(createRequest(11, "{\"id\":1}"));

        assertEquals(1, responses.size());
        assertEquals(200, responses.getFirst().getCommandId());
        assertTrue(responses.getFirst().getData().contains("Phone"));
    }

    @Test
    public void shouldProcessSearchProducts() {
        Product mockProduct = new Product(0,"Phone", 10, new BigDecimal("100"), 1);
        PageResponse<Product> mockResponse = new PageResponse<>(List.of(mockProduct), 1, 1, 1);

        when(productService.searchProducts(any())).thenReturn(mockResponse);

        List<Message> responses = processor.process(createRequest(13, "{\"filter\":{\"categoryId\":1}}"));

        assertEquals(1, responses.size());
        assertEquals(200, responses.getFirst().getCommandId());

        String jsonResponse = responses.getFirst().getData();
        assertTrue(jsonResponse.contains("Phone"));
        assertTrue(jsonResponse.contains("totalElements"));
    }

    @Test
    public void shouldReturn404ForUnknownCommand() {
        List<Message> responses = processor.process(createRequest(999, "{}"));

        assertEquals(1, responses.size());
        assertEquals(404, responses.getFirst().getCommandId());
        assertTrue(responses.getFirst().getData().contains("Route Not Found"));
    }

    @Test
    public void shouldReturn400OnBusinessLogicError() {
        doThrow(new IllegalStateException("Not enough items in stock"))
                .when(productService).deductStock(1, 100);

        List<Message> responses = processor.process(createRequest(5, "{\"productId\":1,\"amount\":100}"));

        assertEquals(1, responses.size());
        assertEquals(400, responses.getFirst().getCommandId());
        assertTrue(responses.getFirst().getData().contains("Not enough items"));
    }

    @Test
    public void shouldReturn400OnIllegalArgumentException() {
        doThrow(new IllegalArgumentException("The category name cannot be empty"))
                .when(categoryService).createCategory("");

        List<Message> responses = processor.process(createRequest(1, "{\"name\":\"\"}"));

        assertEquals(1, responses.size());
        assertEquals(400, responses.getFirst().getCommandId());
        assertTrue(responses.getFirst().getData().contains("cannot be empty"));
    }

    @Test
    public void shouldReturn500OnJsonParseError() {
        List<Message> responses = processor.process(createRequest(4, "{\"productId\": 1, \"amount\": 10 "));

        assertEquals(1, responses.size());
        assertEquals(500, responses.getFirst().getCommandId());
        assertTrue(responses.getFirst().getData().contains("Internal Server Error"));
        verify(productService, never()).addStock(anyInt(), anyInt());
    }

    @Test
    public void shouldKeepOriginalMessageIdAndClientApplicationId() {
        Message requestMessage = new Message((byte) 5, 777L, 999, 42, "{}");
        List<Message> responses = processor.process(requestMessage);

        assertEquals(1, responses.size());
        Message response = responses.getFirst();

        assertEquals(777L, response.getMessageId());
        assertEquals((byte) 5, response.getClientApplicationId());
    }
}