package org.example;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket controlSocket;
    private Socket dataSocket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private String password;
    private static final String CLIENTS = "clients.txt";
    private static final List<String> availableExtensions = List.of(".jpg", ".png", ".jpeg", ".txt", ".pdf");
    private boolean isLoggedIn = false;
    private ArrayList<File> clientFiles = new ArrayList<>();


    public ClientHandler(Socket controlSocket, Socket dataSocket) {
        this.controlSocket = controlSocket;
        this.dataSocket = dataSocket;

        try {
            this.dataInputStream = new DataInputStream(dataSocket.getInputStream());
            this.dataOutputStream = new DataOutputStream(dataSocket.getOutputStream());

            this.bufferedReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));

            clientHandlers.add(this);

            this.username = bufferedReader.readLine();
            this.password = bufferedReader.readLine();

            if (!isClientValid(username, password)) {
                String errResponseMsg = "530 Login Failed!";

                sendResponse(errResponseMsg);
                throw new IOException(errResponseMsg);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            disconnectClient(this.controlSocket, this.dataSocket, dataInputStream, dataOutputStream, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        while (controlSocket.isConnected()) {
            try {
                if (!isLoggedIn) {
                    // we get here after successfully login
                    sendResponse("230 Successfully LoggedIn");
                    isLoggedIn = true;
                } else {
                    String clientRequest = bufferedReader.readLine();
                    System.out.println("Request received " + clientRequest);

                    if (clientRequest.equalsIgnoreCase("list")) {
                        listFiles();
                    }

                    if (clientRequest.equalsIgnoreCase("stor")) {
                        getFileFromClient();
                    }

                    if (clientRequest.equalsIgnoreCase("retr")) {
                        String fileName = bufferedReader.readLine();
                        System.out.println("Name of downloaded file: " + fileName);
                        File dir = new File(username.trim());

                        if (dir.exists()) {
                            File[] files = dir.listFiles();

                            for (File file : files) {
                                if (fileName.equals(file.getName())) {
                                    sendFileToClient(file);

                                    sendResponse("226 Transfer Completed");
                                }
                            }
                        }
                    }

                    if (clientRequest.equalsIgnoreCase("dele")) {
                        String fileName = bufferedReader.readLine();
                        System.out.println("Name to delete: " + fileName);

                        File dir = new File(username.trim());

                        if (dir.exists()) {
                            File[] files = dir.listFiles();

                            for (File file : files) {
                                if (fileName.equals(file.getName())) {
                                    file.delete();
                                    clientFiles.remove(file);
                                    sendResponse("250 File Deleted");
                                }
                            }
                        }
                    }
                }

            } catch (IOException e) {
                disconnectClient(controlSocket, dataSocket, dataInputStream, dataOutputStream, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    private void getFileFromClient() throws IOException {
        int fileNameLength = dataInputStream.readInt();

        if (fileNameLength > 0) {
            byte[] fileNameBytes = new byte[fileNameLength];
            dataInputStream.readFully(fileNameBytes, 0, fileNameLength);
            String fileName = new String(fileNameBytes);  // decode file name from bytes to string

            int fileContentLength = dataInputStream.readInt();

            if (fileContentLength > 0) {
                byte[] fileContentBytes = new byte[fileContentLength];
                dataInputStream.readFully(fileContentBytes, 0, fileContentLength); // we read whole file

                System.out.println("FILE RECEIVED");

                String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
                if (availableExtensions.contains(extension)) {
                    System.out.println("SENT FILE NAME: " + fileName);
                    File dir = new File(username.trim());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    // save file on server
                    File fileToSave = new File(dir.getAbsolutePath() + "/" + fileName);
                    if (fileToSave.exists()) {
                        String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
                        int numberOfFiles = 0;
                        for (File file : Objects.requireNonNull(dir.listFiles())) {
                            if (file.getName().contains(fileNameWithoutExtension)) {
                                numberOfFiles++;
                            }
                        }
                        String newName = renameFile(extension, dir, fileNameWithoutExtension, numberOfFiles);
                        fileToSave.renameTo(new File(newName));
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(fileToSave);
                    fileOutputStream.write(fileContentBytes);
                    fileOutputStream.close();

                    clientFiles.add(fileToSave);

                    sendResponse("226 File On Server");
                }
            }
        }
    }

    private static String renameFile(String extension, File dir, String fileNameWithoutExtension, int numberOfFiles) {
        return new StringBuilder(dir.getAbsolutePath())
                .append("/")
                .append(fileNameWithoutExtension)
                .append("(").append(numberOfFiles).append(")").append(extension).toString();
    }

    private void listFiles() throws IOException {
        File dir = new File(username.trim());
        if (dir.exists()) {
            File[] files = dir.listFiles();

            if (files != null) {
                dataOutputStream.writeInt(files.length);

                for (File file : files) {
                    sendFileToClient(file);
                }

                sendResponse("226 Files Sent");  // files loaded
            }
        }
    }

    private void sendFileToClient(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());

            byte[] fileNameBytes = file.getName().getBytes();
            byte[] fileContent = new byte[(int) file.length()];

            fileInputStream.read(fileContent);  // now we have our file in this stream
            fileInputStream.close();

            dataOutputStream.writeInt(fileNameBytes.length);  // we are telling server size of sending data
            dataOutputStream.write(fileNameBytes);

            dataOutputStream.writeInt(fileContent.length);
            dataOutputStream.write(fileContent);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void disconnectClient(Socket controlSocket, Socket dataSocket, DataInputStream dataInputStream, DataOutputStream dataOutputStream,
                                  BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (controlSocket != null) this.controlSocket.close();
            if (dataSocket != null) this.dataSocket.close();
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
            disconnectClient(controlSocket, dataSocket, dataInputStream, dataOutputStream, bufferedReader, bufferedWriter);
        }
    }
}
