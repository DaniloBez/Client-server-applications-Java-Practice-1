package processor;

import dto.Message;
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

        Message response = processor.process(createRequest(1, "{\"name\":\"Electronics\"}"));

        assertEquals(200, response.getCommandId());
        assertTrue(response.getData().contains("\"id\":10"));
    }

    @Test
    public void shouldProcessGetCategory() {
        ProductCategory category = new ProductCategory("Food");
        when(categoryService.getCategory(1)).thenReturn(category);

        Message response = processor.process(createRequest(7, "{\"id\":1}"));

        assertEquals(200, response.getCommandId());
        assertTrue(response.getData().contains("Food"));
    }

    @Test
    public void shouldProcessUpdateCategoryName() {
        Message response = processor.process(createRequest(8, "{\"id\":1,\"newName\":\"Drinks\"}"));

        assertEquals(200, response.getCommandId());
        assertTrue(response.getData().contains("true"));
        verify(categoryService, times(1)).updateCategoryName(1, "Drinks");
    }

    @Test
    public void shouldProcessDeleteCategory() {
        when(categoryService.deleteCategory(1)).thenReturn(true);

        Message response = processor.process(createRequest(9, "{\"id\":1}"));

        assertEquals(200, response.getCommandId());
        verify(categoryService, times(1)).deleteCategory(1);
    }

    @Test
    public void shouldProcessGetAllCategories() {
        ProductCategory cat1 = new ProductCategory("Cat1");
        ProductCategory cat2 = new ProductCategory("Cat2");
        when(categoryService.getAllCategories()).thenReturn(List.of(cat1, cat2));

        Message response = processor.process(createRequest(10, "{}"));

        assertEquals(200, response.getCommandId());
        assertTrue(response.getData().contains("Cat1"));
        assertTrue(response.getData().contains("Cat2"));
    }

    @Test
    public void shouldProcessCreateProduct() {
        when(productService.createProduct(eq("Laptop"), eq(50), any(BigDecimal.class), eq(1))).thenReturn(5);

        Message response = processor.process(createRequest(2,
                "{\"name\":\"Laptop\",\"initialStock\":50,\"price\":1500.00,\"categoryId\":1}"));

        assertEquals(200, response.getCommandId());
        assertTrue(response.getData().contains("\"id\":5"));
    }

    @Test
    public void shouldProcessGetStockQuantity() {
        when(productService.getStockQuantity(10)).thenReturn(404);

        Message response = processor.process(createRequest(3, "{\"productId\":10}"));

        assertEquals(200, response.getCommandId());
        assertTrue(response.getData().contains("404"));
    }

    @Test
    public void shouldProcessAddStock() {
        Message response = processor.process(createRequest(4, "{\"productId\":10,\"amount\":20}"));

        assertEquals(200, response.getCommandId());
        verify(productService, times(1)).addStock(10, 20);
    }

    @Test
    public void shouldProcessDeductStock() {
        Message response = processor.process(createRequest(5, "{\"productId\":10,\"amount\":5}"));

        assertEquals(200, response.getCommandId());
        verify(productService, times(1)).deductStock(10, 5);
    }

    @Test
    public void shouldProcessSetProductPrice() {
        Message response = processor.process(createRequest(6, "{\"productId\":10,\"newPrice\":99.99}"));

        assertEquals(200, response.getCommandId());
        verify(productService, times(1)).setProductPrice(eq(10), any(BigDecimal.class));
    }

    @Test
    public void shouldProcessGetProduct() {
        Product mockProduct = new Product("Phone", 10, new BigDecimal("100"), 1);
        when(productService.getProduct(1)).thenReturn(mockProduct);

        Message response = processor.process(createRequest(11, "{\"id\":1}"));

        assertEquals(200, response.getCommandId());
        assertTrue(response.getData().contains("Phone"));
    }

    @Test
    public void shouldProcessDeleteProduct() {
        when(productService.deleteProduct(1)).thenReturn(true);

        Message response = processor.process(createRequest(12, "{\"id\":1}"));

        assertEquals(200, response.getCommandId());
        verify(productService, times(1)).deleteProduct(1);
    }

    @Test
    public void shouldProcessGetProductsByCategory() {
        Product mockProduct = new Product("Phone", 10, new BigDecimal("100"), 1);
        when(productService.getProductsByCategory(1)).thenReturn(List.of(mockProduct));

        Message response = processor.process(createRequest(13, "{\"id\":1}"));

        assertEquals(200, response.getCommandId());
        assertTrue(response.getData().contains("Phone"));
    }

    @Test
    public void shouldReturn404ForUnknownCommand() {
        Message response = processor.process(createRequest(999, "{}"));

        assertEquals(404, response.getCommandId());
        assertTrue(response.getData().contains("Route Not Found"));
    }

    @Test
    public void shouldReturn400OnBusinessLogicError() {
        doThrow(new IllegalStateException("Not enough items in stock"))
                .when(productService).deductStock(1, 100);

        Message response = processor.process(createRequest(5, "{\"productId\":1,\"amount\":100}"));

        assertEquals(400, response.getCommandId());
        assertTrue(response.getData().contains("Not enough items"));
    }

    @Test
    public void shouldReturn400OnIllegalArgumentException() {
        doThrow(new IllegalArgumentException("The category name cannot be empty"))
                .when(categoryService).createCategory("");

        Message response = processor.process(createRequest(1, "{\"name\":\"\"}"));

        assertEquals(400, response.getCommandId());
        assertTrue(response.getData().contains("cannot be empty"));
    }

    @Test
    public void shouldReturn500OnJsonParseError() {
        Message response = processor.process(createRequest(4, "{\"productId\": 1, \"amount\": 10 "));

        assertEquals(500, response.getCommandId());
        assertTrue(response.getData().contains("Internal Server Error"));
        verify(productService, never()).addStock(anyInt(), anyInt());
    }

    @Test
    public void shouldKeepOriginalMessageIdAndClientApplicationId() {
        Message requestMessage = new Message((byte) 5, 777L, 999, 42, "{}");
        Message response = processor.process(requestMessage);

        assertEquals(777L, response.getMessageId());
        assertEquals((byte) 5, response.getClientApplicationId());
    }
}
