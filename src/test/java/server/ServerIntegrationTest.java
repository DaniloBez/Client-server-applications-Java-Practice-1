package server;

import decryptor.MessageDecryptor;
import dto.Message;
import encryptor.MessageEncryptor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import processor.IProcessor;
import processor.Processor;
import service.ProductCategoryService;
import service.ProductService;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

public class ServerIntegrationTest {

    /*@Test
    public void testSuccessfulPathLoad() throws InterruptedException {
        AtomicInteger actuallySentCounter = new AtomicInteger(0);

        TestableReceiver spyReceiver = new TestableReceiver(
                () -> {
                    actuallySentCounter.incrementAndGet();
                    return new Message((byte) 1, 999L, 10, 42, "{}");
                },
                1
        );
        TestableSender spySender = new TestableSender();

        IProcessor mockProcessor = Mockito.mock(IProcessor.class);

        Mockito.when(mockProcessor.process(any(Message.class))).thenAnswer(invocation -> {
            Message inputMessage = invocation.getArgument(0);
            Thread.sleep(2);

            return new Message(
                    inputMessage.getClientApplicationId(),
                    inputMessage.getMessageId(),
                    200,
                    inputMessage.getUserId(),
                    "{\"status\":\"ok\"}"
            );
        });

        Server server = new Server(
                spyReceiver,
                2,
                spySender,
                5,
                new MessageDecryptor(),
                2,
                new MessageEncryptor(),
                3,
                mockProcessor,
                4
        );

        server.start();
        Thread.sleep(3000);
        server.stop();

        int expectedMessages = actuallySentCounter.get();
        int actualResponses = spySender.getReceivedMessages().size();

        assertEquals(expectedMessages, actualResponses);

        Message firstResponse = spySender.getReceivedMessages().peek();
        assertNotNull(firstResponse);
        assertEquals(200, firstResponse.getCommandId());
    }

    @Test
    public void testErrorResilience() throws InterruptedException {
        AtomicInteger actuallySentCounter = new AtomicInteger(0);

        Supplier<Message> mixedMessageGenerator = () -> {
            actuallySentCounter.incrementAndGet();

            double rand = Math.random();
            if (rand < 0.25)
                return new Message((byte) 1, 100L, 11, 42, "{\"id\":1}");
            else if (rand < 0.50)
                return new Message((byte) 1, 200L, 11, 42, "{\"id\":999}");
            else if (rand < 0.75)
                return new Message((byte) 1, 300L, 11, 42, "{\"id\":2}");
            else
                return new Message((byte) 1, 400L, 9999, 42, "{}");

        };

        TestableReceiver spyReceiver = new TestableReceiver(mixedMessageGenerator, 1);
        TestableSender spySender = new TestableSender();

        ProductService mockProductService = Mockito.mock(ProductService.class);
        ProductCategoryService mockCategoryService = Mockito.mock(ProductCategoryService.class);

        Mockito.when(mockProductService.getProduct(1))
                .thenReturn(new entity.Product("Test Laptop", 50, java.math.BigDecimal.valueOf(1500), 1));

        Mockito.when(mockProductService.getProduct(999))
                .thenThrow(new IllegalArgumentException("The product with ID 999 was not found"));

        Mockito.when(mockProductService.getProduct(2))
                .thenThrow(new RuntimeException("Database timeout exception!"));

        Processor realProcessor = new Processor(mockProductService, mockCategoryService);

        Server server = new Server(
                spyReceiver,
                2,
                spySender,
                5,
                new MessageDecryptor(),
                2,
                new MessageEncryptor(),
                3,
                realProcessor,
                4
        );

        server.start();
        Thread.sleep(3000);
        server.stop();

        int expectedMessages = actuallySentCounter.get();
        int actualResponses = spySender.getReceivedMessages().size();
        assertEquals(expectedMessages, actualResponses);

        long successCount = spySender.getReceivedMessages().stream()
                .filter(m -> m.getCommandId() == 200)
                .count();

        long errorCount = spySender.getReceivedMessages().stream()
                .filter(m -> m.getCommandId() == 400 || m.getCommandId() == 404 || m.getCommandId() == 500)
                .count();

        assertEquals(expectedMessages, successCount + errorCount);
    }

    @Test
    public void testGracefulShutdownUnderLoad() throws InterruptedException {
        AtomicInteger actuallySentCounter = new AtomicInteger(0);

        Supplier<Message> fastMessageGenerator = () -> {
            actuallySentCounter.incrementAndGet();
            return new Message((byte) 1, 100L, 11, 42, "{\"id\":1}");
        };

        TestableReceiver spyReceiver = new TestableReceiver(fastMessageGenerator, 1);
        TestableSender spySender = new TestableSender();

        ProductService mockProductService = Mockito.mock(ProductService.class);
        ProductCategoryService mockCategoryService = Mockito.mock(ProductCategoryService.class);

        Mockito.when(mockProductService.getProduct(1)).thenAnswer(_ -> {
            Thread.sleep(10);
            return new entity.Product("MacBook Pro", 50, java.math.BigDecimal.valueOf(2500), 1);
        });

        Processor realProcessor = new Processor(mockProductService, mockCategoryService);

        Server server = new Server(
                spyReceiver,
                2,
                spySender,
                5,
                new MessageDecryptor(),
                2,
                new MessageEncryptor(),
                3,
                realProcessor,
                4
        );

        server.start();
        Thread.sleep(500);
        server.stop();

        int expectedMessages = actuallySentCounter.get();
        int actualResponses = spySender.getReceivedMessages().size();

        assertEquals(expectedMessages, actualResponses);
    }*/
}
