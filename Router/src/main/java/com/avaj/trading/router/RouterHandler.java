package com.avaj.trading.router;

import java.io.*;
import java.net.*;
import java.util.Map;

public class RouterHandler implements Runnable {
    private Socket socket;
    private Map<Integer, Socket> connections;

    public RouterHandler(Socket socket, Map<Integer, Socket> connections) {
        this.socket = socket;
        this.connections = connections;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);

                if (!ChecksumValidator.isValid(message)) {
                    System.out.println("Invalid checksum! Message rejected.");
                    continue;
                }

                String[] parts = message.split("\\|");
                int targetId = Integer.parseInt(parts[0]);
                Socket targetSocket = connections.get(targetId);

                if (targetSocket != null) {
                    PrintWriter out = new PrintWriter(targetSocket.getOutputStream(), true);
                    out.println(message);
                    System.out.println("Message forwarded to ID: " + targetId);
                } else {
                    System.out.println("Unknown target ID: " + targetId);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
