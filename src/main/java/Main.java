import decryptor.IDecryptor;
import decryptor.MessageDecryptor;
import encryptor.MessageEncryptor;
import processor.IProcessor;
import processor.Processor;
import receiver.FakeReceiver;
import receiver.IReceiver;
import repository.ProductCategoryRepository;
import repository.ProductRepository;
import sender.FakeSender;
import sender.ISender;
import server.Server;
import service.ProductCategoryService;
import service.ProductService;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Server server = initServer();

        server.start();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Type ‘stop’ to safely shut down the server.");

        while (true) {
            String command = scanner.nextLine();
            if ("stop".equalsIgnoreCase(command.trim())) {
                server.stop();
                break;
            }
        }

        System.out.println("The main branch has been completed.");
    }

    private static Server initServer() {
        ProductCategoryRepository categoryRepository = new ProductCategoryRepository();
        ProductRepository productRepository = new ProductRepository();

        ProductCategoryService categoryService = new ProductCategoryService(categoryRepository, productRepository);
        ProductService productService = new ProductService(productRepository, categoryRepository);

        IReceiver prodReceiver = new FakeReceiver();
        ISender prodSender = new FakeSender();
        IDecryptor serverDecryptor = new MessageDecryptor();
        MessageEncryptor serverEncryptor = new MessageEncryptor();
        IProcessor serverProcessor = new Processor(productService, categoryService);

        return new Server(
                prodReceiver,
                prodSender,
                serverDecryptor,
                serverEncryptor,
                serverProcessor
        );
    }
}
