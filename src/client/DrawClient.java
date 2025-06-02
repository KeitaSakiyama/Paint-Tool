package client;

import ui.CustomView;
import java.io.*;
import java.net.*;

public class DrawClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public DrawClient(String serverAddress) throws IOException {
        socket = new Socket(serverAddress, 4000);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        new Thread(() -> {
            try {
                String input;
                while ((input = in.readLine()) != null) {
                    System.out.println("Received: " + input);
                    CustomView.applyRemoteDrawData(input);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendDrawData(String data) {
        out.println(data);
    }

    public void close() throws IOException {
        socket.close();
    }

    private static final String SERVER_ADDRESS = "127.0.0.1"; // サーバーのIPアドレス
    private static final int PORT = 4000;
    private static final String SAVE_PATH = "draw_data.txt";

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT)) {
            System.out.println("Connected to server.");

            // ファイル受信処理
            receiveFile(socket, SAVE_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveFile(Socket socket, String savePath) {
        try (
                InputStream is = socket.getInputStream();
                FileOutputStream fos = new FileOutputStream(savePath);
                BufferedOutputStream bos = new BufferedOutputStream(fos)
        ) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            bos.flush();
            System.out.println("File received and saved as: " + savePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
