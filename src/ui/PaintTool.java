package ui;

import model.DrawData;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;


public class PaintTool extends JFrame {

    private final JPanel contentPane;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    PaintTool frame = new PaintTool();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public PaintTool() {
        // 起動時にファイルを初期化
        try {
            Files.write(Paths.get("draw_data.txt"), "SHAPES\nTEXTS\nGIFS\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // シャットダウンした際にファイルを初期化
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.write(Paths.get("draw_data.txt"), "SHAPES\nTEXTS\nGIFS\n".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        // ウィンドウが閉じられる際にファイルを初期化
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    Files.write(Paths.get("draw_data.txt"), "SHAPES\nTEXTS\nGIFS\n".getBytes());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        setPreferredSize(new Dimension(1000, 600));
        setTitle("SoftDev Paint Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1200, 800);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        CustomView customView = new CustomView();
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
        contentPane.add(customView, BorderLayout.CENTER);

        JPanel setupPanel = new JPanel();
        setupPanel.setLayout(new GridLayout(2, 0, 5, 5));
        contentPane.add(setupPanel, BorderLayout.SOUTH);

        JButton btnReset = new JButton("Reset");
        btnReset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                customView.reset();
            }
        });
        setupPanel.add(btnReset);

        JButton btnUndo = new JButton("Undo");
        btnUndo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                customView.undo();
            }
        });
        setupPanel.add(btnUndo);

        JButton btnToFront = new JButton("To Front");
        btnToFront.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                customView.bringToFront();
            }
        });
        setupPanel.add(btnToFront);

        JButton btnToBack = new JButton("To Back");
        btnToBack.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                customView.sendToBack();
            }
        });
        setupPanel.add(btnToBack);

        JButton btnAddText = new JButton("Add Text");
        btnAddText.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = JOptionPane.showInputDialog("Enter text:");
                if (text != null && !text.isEmpty()) {
                    customView.addText(text, 50, 50); // Default position, can be updated
                }
            }
        });
        setupPanel.add(btnAddText);

        JButton btnAddGIF = new JButton("Add GIF");
        btnAddGIF.addActionListener(e -> {
            String url = JOptionPane.showInputDialog("Enter GIF URL:");
            if (url != null && !url.isEmpty()) {
                try {
                    customView.addGIF(new URL(url), 100, 100); // Default position
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to load GIF.", "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });
        setupPanel.add(btnAddGIF);

        JButton btnSave = new JButton("Save");
        btnSave.addActionListener(e -> {
            try {
                customView.saveDrawData("draw_data.txt");
                JOptionPane.showMessageDialog(this, "Data saved successfully.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to save data.", "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
        setupPanel.add(btnSave);

        JButton btnLoad = new JButton("Load");
        btnLoad.addActionListener(e -> {
            try {
                customView.loadDrawData("draw_data.txt");
                JOptionPane.showMessageDialog(this, "Data loaded successfully.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to load data.", "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
        setupPanel.add(btnLoad);

        JButton btnConnect = new JButton("Connect to Server");
        btnConnect.addActionListener(e -> {
            String serverAddress = JOptionPane.showInputDialog("Enter Server Address:");
            if (serverAddress != null && !serverAddress.isEmpty()) {
                customView.connectToServer(serverAddress);
            }
        });
        setupPanel.add(btnConnect);

        JButton btnSendData = new JButton("Send Data to Server");
        btnSendData.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String serverAddress = JOptionPane.showInputDialog("Enter Server Address:");
                if (serverAddress != null && !serverAddress.isEmpty()) {
                    DrawData data = new DrawData("RECTANGLE", 10, 20, 100, 50, "red"); // Example data
                    customView.sendDrawData(data);
                }
            }
        });
        setupPanel.add(btnSendData);

        JButton btnStartAnimation = new JButton("Start Animation");
        btnStartAnimation.addActionListener(e -> customView.startAnimation());
        setupPanel.add(btnStartAnimation);

        JButton btnStopAnimation = new JButton("Stop Animation");
        btnStopAnimation.addActionListener(e -> customView.stopAnimation());
        setupPanel.add(btnStopAnimation);
    }
}