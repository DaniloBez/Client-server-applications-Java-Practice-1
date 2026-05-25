import server.Server;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Server server = new Server();
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
}
