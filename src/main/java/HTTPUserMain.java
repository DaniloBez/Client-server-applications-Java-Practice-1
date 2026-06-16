import client.StoreClientHTTP;

import java.math.BigDecimal;
import java.util.Scanner;

public class HTTPUserMain {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;

        StoreClientHTTP client = new StoreClientHTTP(host, port);
        Scanner scanner = new Scanner(System.in);

        System.out.println("Connected to HTTP Server at http://" + host + ":" + port);
        System.out.println("Available commands:");
        System.out.println("1 - Register");
        System.out.println("2 - Login");
        System.out.println("3 - Get Product by ID");
        System.out.println("4 - Create Product");
        System.out.println("5 - Update Product by ID");
        System.out.println("6 - Delete Product by ID");
        System.out.println("7 - Delete User");
        System.out.println("stop - Exit");

        boolean stop = false;
        while (!stop) {
            System.out.print("> ");
            String command = scanner.nextLine().trim();

            switch (command) {
                case "stop":
                    stop = true;
                    break;
                case "1":
                    System.out.print("Username: ");
                    String regUser = scanner.nextLine().trim();
                    System.out.print("Password: ");
                    String regPass = scanner.nextLine().trim();
                    client.register(regUser, regPass);
                    break;
                case "2":
                    System.out.print("Username: ");
                    String logUser = scanner.nextLine().trim();
                    System.out.print("Password: ");
                    String logPass = scanner.nextLine().trim();
                    client.login(logUser, logPass);
                    break;
                case "3":
                    System.out.print("Product ID: ");
                    int getId = Integer.parseInt(scanner.nextLine().trim());
                    client.getProduct(getId);
                    break;
                case "4":
                    System.out.print("Name: ");
                    String createName = scanner.nextLine().trim();
                    System.out.print("Initial Stock: ");
                    int createStock = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("Price: ");
                    BigDecimal createPrice = new BigDecimal(scanner.nextLine().trim());
                    System.out.print("Category ID: ");
                    int createCategoryId = Integer.parseInt(scanner.nextLine().trim());
                    client.createProduct(createName, createStock, createPrice, createCategoryId);
                    break;
                case "5":
                    System.out.print("Product ID to update: ");
                    int updateId = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("New Name: ");
                    String updateName = scanner.nextLine().trim();
                    System.out.print("New Stock: ");
                    int updateStock = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("New Price: ");
                    BigDecimal updatePrice = new BigDecimal(scanner.nextLine().trim());
                    System.out.print("New Category ID: ");
                    int updateCategoryId = Integer.parseInt(scanner.nextLine().trim());
                    client.updateProduct(updateId, updateName, updateStock, updatePrice, updateCategoryId);
                    break;
                case "6":
                    System.out.print("Product ID to delete: ");
                    int deleteId = Integer.parseInt(scanner.nextLine().trim());
                    client.deleteProduct(deleteId);
                    break;
                case "7":
                    System.out.println("Deleting current logged-in user...");
                    client.deleteUser();
                    break;
                default:
                    System.out.println("Unknown command.");
            }
        }

        System.out.println("HTTP client has been closed.");
    }
}
