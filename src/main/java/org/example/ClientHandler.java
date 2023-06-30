package org.example;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private String password;
    private static final String CLIENTS = "clients.txt";
    private static final List<String> availableExtensions = List.of(".jpg", ".png", ".jpeg", ".txt");
    private boolean isLoggedIn = false;


    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            clientHandlers.add(this);

            this.username = bufferedReader.readLine();
            this.password = bufferedReader.readLine();

            if (!isClientValid(username, password)) {
                throw new IOException("Invalid credentials!");
            }

            System.out.println("USERNAME: " + username);
            System.out.println("PASSWORD: " + password);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            disconnectClient(socket, dataInputStream, dataOutputStream, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        ArrayList<File> clientFiles = new ArrayList<>();
        File uploadedFile;
        String msg;

        while (socket.isConnected()) {
            try {
                if (!isLoggedIn) {
                    // we get here after successfully login
                    sendResponse("230");
                    isLoggedIn = true;
                }

                int fileNameLength = dataInputStream.readInt();

                if (fileNameLength > 0) {
                    byte[] fileNameBytes = new byte[fileNameLength];
                    dataInputStream.readFully(fileNameBytes, 0, fileNameLength);  // we read whole file
                    String fileName = new String(fileNameBytes);

                    int fileContentLength = dataInputStream.readInt();

                    if (fileContentLength > 0) {
                        byte[] fileContentBytes = new byte[fileContentLength];
                        dataInputStream.readFully(fileContentBytes, 0, fileContentLength);

                        System.out.println("FILE RECEIVED");

                        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
                        if (availableExtensions.contains(extension)) {
                            System.out.println("PRZESLANY PLIK: " + fileName);
                            File dir = new File(username.trim());
                            if (!dir.exists()) {
                                dir.mkdirs();
                            }

                            // save file on server
                            File fileToSave = new File(dir.getAbsolutePath() + "/" + fileName);
                            if (fileToSave.exists()) {
                                String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
                                int numberOfFiles = 0;
                                for (File file: dir.listFiles()) {
                                    if (file.getName().contains(fileNameWithoutExtension)) {
                                        numberOfFiles++;
                                    }
                                }
                                fileToSave.renameTo(new File(dir.getAbsolutePath() + "/" + fileNameWithoutExtension
                                        + "(" + numberOfFiles + ")" + extension));
                            }
                            FileOutputStream fileOutputStream = new FileOutputStream(fileToSave);
                            fileOutputStream.write(fileContentBytes);
                            fileOutputStream.close();

                            clientFiles.add(fileToSave);
                        }
                    }
                }
            } catch (IOException e) {
                disconnectClient(socket, dataInputStream, dataOutputStream, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    private void disconnectClient(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream,
                                  BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (socket != null) socket.close();
            if (dataInputStream != null) this.dataInputStream.close();
            if (dataOutputStream != null) this.dataOutputStream.close();
            if (bufferedReader != null) this.bufferedReader.close();
            if (bufferedWriter != null) this.bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isClientValid(String username, String password) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(CLIENTS))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(":");
                String clientUsername = parts[0];
                String clientPassword = parts[1];

                if (clientUsername.equals(username) && clientPassword.equals(password)) return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void sendResponse(String response) {
        try {
            bufferedWriter.write(response);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            System.out.println("RESPONSE SENT " + response);
        } catch (IOException e) {
            disconnectClient(socket, dataInputStream, dataOutputStream, bufferedReader, bufferedWriter);
        }
    }
}
