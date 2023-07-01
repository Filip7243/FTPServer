package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket serverControlSocket;
    private ServerSocket serverDataSocket;

    public Server(ServerSocket serverSocket, ServerSocket serverDataSocket) {
        this.serverControlSocket = serverSocket;
        this.serverDataSocket = serverDataSocket;
    }

    public void startServer() {
        try {
            while (!serverControlSocket.isClosed()) {
                System.out.println("Waiting for clients...");
                Socket controleSocket = serverControlSocket.accept();  // lock, waiting for new user, then released
                System.out.println("New client connected!");
                Socket dataSocket = serverDataSocket.accept();
                System.out.println("DATA CHANEL OPENED!");

                ClientHandler clientHandler = new ClientHandler(controleSocket, dataSocket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeServerSocket();
        }
    }

    private void closeServerSocket() {
        try {
            if (serverControlSocket != null) serverControlSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
