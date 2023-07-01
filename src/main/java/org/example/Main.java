package org.example;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) {
        try {
            ServerSocket controlSocket = new ServerSocket(21);
            ServerSocket serverDataSocket = new ServerSocket(20);
            Server server = new Server(controlSocket, serverDataSocket);
            server.startServer();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}