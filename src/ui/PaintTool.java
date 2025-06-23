package ui;

import model.DrawData;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PaintTool extends JFrame {
    
    // 定数の定義
    private static final Logger LOGGER = Logger.getLogger(PaintTool.class.getName());
    private static final String WINDOW_TITLE = "SoftDev Paint Tool";
    private static final String DATA_FILE_PATH = "draw_data.txt";
    private static final String INITIAL_FILE_CONTENT = "SHAPES\nTEXTS\nGIFS\n";
    
    // ウィンドウサイズ定数
    private static final int PREFERRED_WIDTH = 1000;
    private static final int PREFERRED_HEIGHT = 600;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int WINDOW_X = 100;
    private static final int WINDOW_Y = 100;
    
    // レイアウト定数
    private static final int BORDER_SIZE = 5;
    private static final int GRID_ROWS = 2;
    private static final int GRID_COLS = 0;
    private static final int GRID_HGAP = 5;
    private static final int GRID_VGAP = 5;
    
    // デフォルト位置定数
    private static final int DEFAULT_TEXT_X = 50;
    private static final int DEFAULT_TEXT_Y = 50;
    private static final int DEFAULT_GIF_X = 100;
    private static final int DEFAULT_GIF_Y = 100;

    private JPanel contentPane;
    private CustomView customView;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                PaintTool frame = new PaintTool();
                frame.setVisible(true);
            } catch (Exception e) {
                Logger.getLogger(PaintTool.class.getName()).log(Level.SEVERE, "Failed to start application", e);
            }
        });
    }

    /**
     * Create the frame.
     */
    public PaintTool() {
        initializeDataFile();
        setupShutdownHooks();
        initializeWindow();
        initializeComponents();
    }
    
    /**
     * データファイルを初期化する
     */
    private void initializeDataFile() {
        try {
            Files.write(Paths.get(DATA_FILE_PATH), INITIAL_FILE_CONTENT.getBytes());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize data file", e);
            showErrorDialog("データファイルの初期化に失敗しました: " + e.getMessage());
        }
    }
    
    /**
     * シャットダウンフックとウィンドウリスナーを設定
     */
    private void setupShutdownHooks() {
        // シャットダウンした際にファイルを初期化
        Runtime.getRuntime().addShutdownHook(new Thread(this::resetDataFile));

        // ウィンドウが閉じられる際にファイルを初期化
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                resetDataFile();
            }
        });
    }
    
    /**
     * データファイルをリセット
     */
    private void resetDataFile() {
        try {
            Files.write(Paths.get(DATA_FILE_PATH), INITIAL_FILE_CONTENT.getBytes());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to reset data file", e);
        }
    }
    
    /**
     * ウィンドウの基本設定を初期化
     */
    private void initializeWindow() {
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        setTitle(WINDOW_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(WINDOW_X, WINDOW_Y, WINDOW_WIDTH, WINDOW_HEIGHT);
    }
    
    /**
     * UIコンポーネントを初期化
     */
    private void initializeComponents() {
        setupContentPane();
        setupCustomView();
        setupControlPanel();
    }
    
    /**
     * コンテンツペインを設定
     */
    private void setupContentPane() {
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);
    }
    
    /**
     * カスタムビューを設定
     */
    private void setupCustomView() {
        customView = new CustomView();
        setupMouseListeners();
        contentPane.add(customView, BorderLayout.CENTER);
    }
    
    /**
     * マウスリスナーを設定
     */
    private void setupMouseListeners() {
        customView.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                customView.mousePressed(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                customView.mouseReleased(e.getPoint());
            }
        });
        
        customView.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                customView.mouseDragged(e.getPoint());
            }
        });
    }
    
    /**
     * コントロールパネルを設定
     */
    private void setupControlPanel() {
        JPanel setupPanel = new JPanel();
        setupPanel.setLayout(new GridLayout(GRID_ROWS, GRID_COLS, GRID_HGAP, GRID_VGAP));
        contentPane.add(setupPanel, BorderLayout.SOUTH);
        
        addBasicButtons(setupPanel);
        addContentButtons(setupPanel);
        addFileButtons(setupPanel);
        addNetworkButtons(setupPanel);
        addAnimationButtons(setupPanel);
    }
    
    /**
     * エラーダイアログを表示
     */
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "エラー", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * 成功ダイアログを表示
     */
    private void showSuccessDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "成功", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * 基本操作ボタンを追加
     */
    private void addBasicButtons(JPanel panel) {
        JButton btnReset = new JButton("Reset");
        btnReset.addActionListener(_ -> customView.reset());
        panel.add(btnReset);

        JButton btnUndo = new JButton("Undo");
        btnUndo.addActionListener(_ -> customView.undo());
        panel.add(btnUndo);

        JButton btnToFront = new JButton("To Front");
        btnToFront.addActionListener(_ -> customView.bringToFront());
        panel.add(btnToFront);

        JButton btnToBack = new JButton("To Back");
        btnToBack.addActionListener(_ -> customView.sendToBack());
        panel.add(btnToBack);
    }
    
    /**
     * コンテンツ追加ボタンを追加
     */
    private void addContentButtons(JPanel panel) {
        JButton btnAddText = new JButton("Add Text");
        btnAddText.addActionListener(_ -> handleAddText());
        panel.add(btnAddText);

        JButton btnAddGIF = new JButton("Add GIF");
        btnAddGIF.addActionListener(_ -> handleAddGIF());
        panel.add(btnAddGIF);
    }
    
    /**
     * ファイル操作ボタンを追加
     */
    private void addFileButtons(JPanel panel) {
        JButton btnSave = new JButton("Save");
        btnSave.addActionListener(_ -> handleSave());
        panel.add(btnSave);

        JButton btnLoad = new JButton("Load");
        btnLoad.addActionListener(_ -> handleLoad());
        panel.add(btnLoad);
    }
    
    /**
     * ネットワーク操作ボタンを追加
     */
    private void addNetworkButtons(JPanel panel) {
        JButton btnConnect = new JButton("Connect to Server");
        btnConnect.addActionListener(_ -> handleConnect());
        panel.add(btnConnect);

        JButton btnSendData = new JButton("Send Data to Server");
        btnSendData.addActionListener(_ -> handleSendData());
        panel.add(btnSendData);
    }
    
    /**
     * アニメーション操作ボタンを追加
     */
    private void addAnimationButtons(JPanel panel) {
        JButton btnStartAnimation = new JButton("Start Animation");
        btnStartAnimation.addActionListener(_ -> customView.startAnimation());
        panel.add(btnStartAnimation);

        JButton btnStopAnimation = new JButton("Stop Animation");
        btnStopAnimation.addActionListener(_ -> customView.stopAnimation());
        panel.add(btnStopAnimation);
    }
    
    /**
     * テキスト追加処理
     */
    private void handleAddText() {
        String text = JOptionPane.showInputDialog("Enter text:");
        if (text != null && !text.isEmpty()) {
            customView.addText(text, DEFAULT_TEXT_X, DEFAULT_TEXT_Y);
        }
    }
    
    /**
     * GIF追加処理
     */
    private void handleAddGIF() {
        String url = JOptionPane.showInputDialog("Enter GIF URL:");
        if (url != null && !url.isEmpty()) {
            try {
                URL gifUrl = URI.create(url).toURL();
                customView.addGIF(gifUrl, DEFAULT_GIF_X, DEFAULT_GIF_Y);
            } catch (Exception ex) {
                showErrorDialog("GIFの読み込みに失敗しました: " + ex.getMessage());
            }
        }
    }
    
    /**
     * 保存処理
     */
    private void handleSave() {
        try {
            customView.saveDrawData(DATA_FILE_PATH);
            showSuccessDialog("データが正常に保存されました。");
        } catch (IOException ex) {
            showErrorDialog("データの保存に失敗しました: " + ex.getMessage());
        }
    }
    
    /**
     * 読み込み処理
     */
    private void handleLoad() {
        try {
            customView.loadDrawData(DATA_FILE_PATH);
            showSuccessDialog("データが正常に読み込まれました。");
        } catch (IOException ex) {
            showErrorDialog("データの読み込みに失敗しました: " + ex.getMessage());
        }
    }
    
    /**
     * サーバー接続処理
     */
    private void handleConnect() {
        String serverAddress = JOptionPane.showInputDialog("Enter Server Address:");
        if (serverAddress != null && !serverAddress.isEmpty()) {
            customView.connectToServer(serverAddress);
        }
    }
    
    /**
     * データ送信処理
     */
    private void handleSendData() {
        String serverAddress = JOptionPane.showInputDialog("Enter Server Address:");
        if (serverAddress != null && !serverAddress.isEmpty()) {
            // Example data - 実際の使用時はより適切なデータを生成
            DrawData data = new DrawData("RECTANGLE", 10, 20, 100, 50, "red");
            customView.sendDrawData(data);
        }
    }
}