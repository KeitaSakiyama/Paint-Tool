package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class DrawServer {
    private static final int PORT = 4000;
    private static final Set<PrintWriter> clientWriters = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Drawing Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            System.out.println("Client connected: " + socket);
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                String input;
                while ((input = in.readLine()) != null) {
                    System.out.println("Received: " + input);
                    synchronized (clientWriters) {
                        for (PrintWriter writer : clientWriters) {
                            writer.println(input);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Client disconnected: " + socket);
            }
        }
    }

    private static void sendFile(Socket clientSocket, String filePath) {
        try (
                FileInputStream fis = new FileInputStream(filePath);
                BufferedInputStream bis = new BufferedInputStream(fis);
                OutputStream os = clientSocket.getOutputStream()
        ) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            System.out.println("File sent: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
