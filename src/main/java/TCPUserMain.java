import client.StoreClientTCP;
import decryptor.MessageDecryptor;
import dto.Message;
import encryptor.MessageEncryptor;

import java.net.InetAddress;
import java.util.Scanner;

public class TCPUserMain {
    public static void main(String[] args) {
        StoreClientTCP client = new StoreClientTCP(
                new MessageEncryptor(),
                new MessageDecryptor(),
                System.out::println
        );

        client.connect(InetAddress.getLoopbackAddress(), 10000);
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type ‘stop’ to safely shut down the tcp client.");

        boolean stop = false;
        while (!stop) {
            String command = scanner.nextLine();

            switch (command.trim()) {
                case "stop":
                    stop = true;
                    break;
                case "1":
                    client.sendCommand(new Message((byte)0, 1L, 1, 1, "{\"name\":\"Electronics\"}"));
                    break;
                case "2":
                    client.sendCommand(new Message((byte)0, 2L, 1, 1, "{\"name\":\"-\"}"));
                    break;
                case "3":
                    client.sendCommand(new Message((byte)0, 3L, 10, 1, "{}"));
            }

        }

        client.disconnect();

        System.out.println("The tcp client has been completed.");
    }
}
