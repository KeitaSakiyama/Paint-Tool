package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DrawServer {
    // 定数の定義
    private static final Logger LOGGER = Logger.getLogger(DrawServer.class.getName());
    private static final int DEFAULT_PORT = 4000;
    private static final int MAX_CLIENTS = 100;
    private static final int THREAD_POOL_SIZE = 10;
    private static final int BUFFER_SIZE = 8192;
    private static final String CHARSET_NAME = "UTF-8";
    
    // サーバー状態管理
    private static final AtomicBoolean isRunning = new AtomicBoolean(false);
    private static final AtomicInteger clientCounter = new AtomicInteger(0);
    
    // クライアント管理
    private static final Set<ClientHandler> activeClients = ConcurrentHashMap.newKeySet();
    private static final Set<PrintWriter> clientWriters = ConcurrentHashMap.newKeySet();
    
    // スレッドプール
    private static ExecutorService clientThreadPool;
    private static ServerSocket serverSocket;

    /**
     * サーバーのメインエントリーポイント
     */
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        
        // シャットダウンフックを設定
        Runtime.getRuntime().addShutdownHook(new Thread(DrawServer::shutdown));
        
        try {
            startServer(port);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Server startup failed", e);
            System.exit(1);
        }
    }
    
    /**
     * サーバーを開始
     * @param port ポート番号
     */
    public static void startServer(int port) throws IOException {
        if (isRunning.get()) {
            LOGGER.warning("Server is already running");
            return;
        }
        
        LOGGER.info("Starting Drawing Server on port " + port);
        
        // スレッドプールを初期化
        clientThreadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        
        // サーバーソケットを作成
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        
        isRunning.set(true);
        LOGGER.info("Drawing Server started successfully on port " + port);
        
        try {
            // クライアント接続の受け入れループ
            while (isRunning.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    // 最大クライアント数チェック
                    if (activeClients.size() >= MAX_CLIENTS) {
                        LOGGER.warning("Maximum client limit reached. Rejecting connection from: " + 
                                     clientSocket.getRemoteSocketAddress());
                        clientSocket.close();
                        continue;
                    }
                    
                    // クライアントハンドラーを作成・実行
                    ClientHandler handler = new ClientHandler(clientSocket, clientCounter.incrementAndGet());
                    activeClients.add(handler);
                    clientThreadPool.execute(handler);
                    
                } catch (SocketException e) {
                    if (isRunning.get()) {
                        LOGGER.log(Level.WARNING, "Socket error while accepting connections", e);
                    }
                } catch (IOException e) {
                    if (isRunning.get()) {
                        LOGGER.log(Level.SEVERE, "IO error while accepting connections", e);
                    }
                }
            }
        } finally {
            shutdown();
        }
    }
    
    /**
     * サーバーをシャットダウン
     */
    public static void shutdown() {
        if (!isRunning.compareAndSet(true, false)) {
            return; // 既にシャットダウン済み
        }
        
        LOGGER.info("Shutting down Drawing Server...");
        
        // すべてのクライアントを切断
        disconnectAllClients();
        
        // スレッドプールをシャットダウン
        shutdownThreadPool();
        
        // サーバーソケットを閉じる
        closeServerSocket();
        
        LOGGER.info("Drawing Server shutdown completed");
    }
    
    /**
     * すべてのクライアントを切断
     */
    private static void disconnectAllClients() {
        LOGGER.info("Disconnecting " + activeClients.size() + " clients...");
        
        for (ClientHandler client : activeClients) {
            client.disconnect();
        }
        
        activeClients.clear();
        clientWriters.clear();
    }
    
    /**
     * スレッドプールをシャットダウン
     */
    private static void shutdownThreadPool() {
        if (clientThreadPool != null && !clientThreadPool.isShutdown()) {
            clientThreadPool.shutdown();
            try {
                if (!clientThreadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOGGER.warning("Thread pool did not terminate gracefully, forcing shutdown...");
                    clientThreadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                clientThreadPool.shutdownNow();
            }
        }
    }
    
    /**
     * サーバーソケットを閉じる
     */
    private static void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error closing server socket", e);
            }
        }
    }
    
    /**
     * サーバーの統計情報を取得
     * @return 統計情報の文字列
     */
    public static String getServerStats() {
        return String.format("Active clients: %d, Total connections: %d, Server running: %s",
                           activeClients.size(), clientCounter.get(), isRunning.get());
    }

    /**
     * メッセージをすべてのクライアントにブロードキャスト
     * @param message ブロードキャストするメッセージ
     * @param senderId 送信者のクライアントID
     */
    private static void broadcastMessage(String message, int senderId) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        // すべてのクライアントに送信
        for (PrintWriter writer : clientWriters) {
            try {
                writer.println(message);
                if (writer.checkError()) {
                    failCount++;
                } else {
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                LOGGER.log(Level.WARNING, "Failed to broadcast message to a client", e);
            }
        }
        
        LOGGER.fine(String.format("Broadcast from client #%d: %d successful, %d failed", 
                                 senderId, successCount, failCount));
    }
    
    /**
     * 特定のクライアントにメッセージを送信
     * @param message 送信するメッセージ
     * @param targetClientId 対象クライアントID
     * @return 送信が成功した場合true
     */
    public static boolean sendMessageToClient(String message, int targetClientId) {
        for (ClientHandler client : activeClients) {
            if (client.clientId == targetClientId && client.isConnected()) {
                try {
                    client.out.println(message);
                    return !client.out.checkError();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to send message to client #" + targetClientId, e);
                    return false;
                }
            }
        }
        return false;
    }
    
    /**
     * 管理者向け統計情報を出力
     */
    public static void printServerStatus() {
        LOGGER.info("=== Server Status ===");
        LOGGER.info("Server running: " + isRunning.get());
        LOGGER.info("Active clients: " + activeClients.size());
        LOGGER.info("Total connections served: " + clientCounter.get());
        LOGGER.info("Thread pool status: " + 
                   (clientThreadPool != null ? "Active" : "Inactive"));
        
        if (!activeClients.isEmpty()) {
            LOGGER.info("Connected clients:");
            for (ClientHandler client : activeClients) {
                LOGGER.info("  " + client.getClientInfo());
            }
        }
        LOGGER.info("====================");
    }
    
    /**
     * クライアント接続を処理するハンドラークラス
     */
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final int clientId;
        private PrintWriter out;
        private BufferedReader in;
        private final AtomicBoolean isConnected = new AtomicBoolean(false);
        private final long connectionTime;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
            this.connectionTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            String clientAddress = socket.getRemoteSocketAddress().toString();
            LOGGER.info("Client #" + clientId + " connected from: " + clientAddress);
            
            try {
                setupIOStreams();
                registerClient();
                handleClientMessages();
                
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "IO error for client #" + clientId, e);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error for client #" + clientId, e);
            } finally {
                cleanup();
            }
        }
        
        /**
         * 入出力ストリームを設定
         */
        private void setupIOStreams() throws IOException {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), CHARSET_NAME));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), CHARSET_NAME), true);
            isConnected.set(true);
        }
        
        /**
         * クライアントを登録
         */
        private void registerClient() {
            clientWriters.add(out);
            LOGGER.fine("Client #" + clientId + " registered successfully");
        }
        
        /**
         * クライアントメッセージを処理
         */
        private void handleClientMessages() throws IOException {
            String input;
            int messageCount = 0;
            
            while (isConnected.get() && (input = in.readLine()) != null) {
                messageCount++;
                LOGGER.fine("Client #" + clientId + " sent: " + input);
                
                // すべてのクライアントにブロードキャスト
                broadcastMessage(input, clientId);
                
                // 大量メッセージの制限（簡単なDDoS対策）
                if (messageCount > 1000) {
                    LOGGER.warning("Client #" + clientId + " exceeded message limit, disconnecting");
                    break;
                }
            }
        }
        
        /**
         * クライアントを切断
         */
        public void disconnect() {
            isConnected.set(false);
            closeIOStreams();
            closeSocket();
        }
        
        /**
         * リソースをクリーンアップ
         */
        private void cleanup() {
            long sessionDuration = System.currentTimeMillis() - connectionTime;
            LOGGER.info("Client #" + clientId + " disconnected (session duration: " + 
                       sessionDuration + "ms)");
            
            // クライアントリストから削除
            activeClients.remove(this);
            if (out != null) {
                clientWriters.remove(out);
            }
            
            disconnect();
        }
        
        /**
         * 入出力ストリームを閉じる
         */
        private void closeIOStreams() {
            if (out != null) {
                out.close();
                out = null;
            }
            
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error closing input stream for client #" + clientId, e);
                }
                in = null;
            }
        }
        
        /**
         * ソケットを閉じる
         */
        private void closeSocket() {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error closing socket for client #" + clientId, e);
                }
            }
        }
        
        /**
         * 接続状態を確認
         */
        public boolean isConnected() {
            return isConnected.get() && socket != null && !socket.isClosed();
        }
        
        /**
         * クライアント情報を取得
         */
        public String getClientInfo() {
            return String.format("Client #%d from %s (connected: %b)", 
                               clientId, socket.getRemoteSocketAddress(), isConnected());
        }
    }

    /**
     * ファイルをクライアントに送信する（改良版）
     * @param clientSocket クライアントのソケット
     * @param filePath 送信するファイルのパス
     * @return 送信が成功した場合true、失敗した場合false
     */
    public static boolean sendFileToClient(Socket clientSocket, String filePath) {
        File file = new File(filePath);
        
        // ファイルの検証
        if (!validateFile(file, filePath)) {
            return false;
        }
        
        try (
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE);
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            // ファイルサイズを先に送信
            long fileSize = file.length();
            dos.writeLong(fileSize);
            LOGGER.info("Sending file size: " + fileSize + " bytes");
            
            // ファイル名を送信
            String fileName = file.getName();
            dos.writeUTF(fileName);
            LOGGER.info("Sending file name: " + fileName);
            
            // ファイル内容を送信
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalSent = 0;
            long lastProgressTime = System.currentTimeMillis();
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
                totalSent += bytesRead;
                
                // 進捗ログ（1秒ごと）
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastProgressTime > 1000) {
                    int progress = (int) ((totalSent * 100) / fileSize);
                    LOGGER.info("File send progress: " + progress + "% (" + totalSent + "/" + fileSize + " bytes)");
                    lastProgressTime = currentTime;
                }
            }
            
            dos.flush();
            
            // 送信完了の確認
            boolean success = (totalSent == fileSize);
            if (success) {
                LOGGER.info("File sent successfully: " + filePath + " (" + totalSent + " bytes)");
            } else {
                LOGGER.severe("File send incomplete: " + filePath + " (sent: " + totalSent + "/" + fileSize + " bytes)");
            }
            return success;
            
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "File not found during send: " + filePath, e);
            return false;
        } catch (SecurityException e) {
            LOGGER.log(Level.SEVERE, "Security error accessing file: " + filePath, e);
            return false;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO error sending file " + filePath, e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error sending file " + filePath, e);
            return false;
        }
    }
    
    /**
     * ファイルの検証
     * @param file ファイルオブジェクト
     * @param filePath ファイルパス
     * @return 有効な場合true
     */
    private static boolean validateFile(File file, String filePath) {
        if (!file.exists()) {
            LOGGER.severe("File not found: " + filePath);
            return false;
        }
        
        if (!file.canRead()) {
            LOGGER.severe("Cannot read file: " + filePath);
            return false;
        }
        
        if (!file.isFile()) {
            LOGGER.severe("Path is not a file: " + filePath);
            return false;
        }
        
        // ファイルサイズ制限（例：100MB）
        long maxFileSize = 100 * 1024 * 1024; // 100MB
        if (file.length() > maxFileSize) {
            LOGGER.severe("File too large: " + filePath + " (" + file.length() + " bytes)");
            return false;
        }
        
        return true;
    }
    
    /**
     * 指定されたクライアントにファイルを送信
     * @param targetClientId 対象クライアントID
     * @param filePath 送信するファイルのパス
     * @return 送信が成功した場合true
     */
    public static boolean sendFileToClient(int targetClientId, String filePath) {
        for (ClientHandler client : activeClients) {
            if (client.clientId == targetClientId && client.isConnected()) {
                return sendFileToClient(client.socket, filePath);
            }
        }
        LOGGER.warning("Client #" + targetClientId + " not found or not connected");
        return false;
    }
}
