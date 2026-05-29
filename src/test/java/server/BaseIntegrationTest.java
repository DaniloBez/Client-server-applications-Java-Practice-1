package server;

import client.IClient;
import client.StoreClientTCP;
import client.StoreClientUDP;
import decryptor.IDecryptor;
import decryptor.MessageDecryptor;
import dto.Message;
import encryptor.MessageEncryptor;
import processor.IProcessor;
import processor.Processor;
import repository.ProductCategoryRepository;
import repository.ProductRepository;
import service.ProductCategoryService;
import service.ProductService;

import java.math.BigDecimal;
import java.util.function.Consumer;

public abstract class BaseIntegrationTest {
    protected int testCategoryId;
    protected int testProductId;

    protected Server initServer(int senderCount, int decryptorCount, int encryptorCount, int processorCount) {
        ProductCategoryRepository categoryRepository = new ProductCategoryRepository();
        testCategoryId = categoryRepository.create("Electronics");

        ProductRepository productRepository = new ProductRepository();
        testProductId = productRepository.create("Product", 10, new BigDecimal(10), testCategoryId);

        ProductCategoryService categoryService = new ProductCategoryService(categoryRepository, productRepository);
        ProductService productService = new ProductService(productRepository, categoryRepository);

        IDecryptor serverDecryptor = new MessageDecryptor();
        MessageEncryptor serverEncryptor = new MessageEncryptor();
        IProcessor serverProcessor = new Processor(productService, categoryService);

        return new Server(senderCount, serverDecryptor, decryptorCount, serverEncryptor, encryptorCount, serverProcessor, processorCount, 10000);
    }

    protected IClient getClient(int id, Consumer<Message> consumer) {
        if (id % 2 == 0)
            return new StoreClientTCP(new MessageEncryptor(), new MessageDecryptor(), consumer);
        else
            return new StoreClientUDP(new MessageEncryptor(), new MessageDecryptor(), consumer);
    }
}
