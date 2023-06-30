package org.example;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(21);
            Server server = new Server(serverSocket);
            server.startServer();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}