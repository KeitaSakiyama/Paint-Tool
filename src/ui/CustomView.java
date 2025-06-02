package ui;

import client.DrawClient;
import model.CircleShape;
import model.DrawData;
import model.LineShape;
import model.RectangleShape;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class CustomView extends JPanel {
    private List<ColoredRectangle> shapes = new ArrayList<>();
    private List<TextShape> texts = new ArrayList<>();
    private List<GIFShape> gifs = new ArrayList<>();
    private Point dragBegin, dragCurrent;
    private ColoredRectangle selectedShape = null;
    private TextShape selectedText = null;
    private GIFShape selectedGIF = null;
    private boolean isAnimating = false;
    private final List<Thread> animationThreads = new CopyOnWriteArrayList<>();
    private int dx = 1, dy = 1;
    private DrawClient drawClient;
    private List<Object> elements = new ArrayList<>();

    /**
     * Create the panel.
     */
    public CustomView() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                } else {
                    CustomView.this.mousePressed(e.getPoint());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                } else {
                    CustomView.this.mouseReleased(e.getPoint());
                }
            }
        });
    }

    @Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, getWidth(), getHeight());
        for (ColoredRectangle rect : shapes) {
            g.setColor(rect.color);
            g.fill(rect);
        }
        for (TextShape text : texts) {
            g.setFont(new Font("Arial", Font.PLAIN, text.fontSize));
            g.setColor(text.color);
            g.drawString(text.text, text.x, text.y);
        }
        for (GIFShape gif : gifs) {
            g.drawImage(gif.image, gif.x, gif.y, this);
        }
        if (selectedShape != null) {
            g.setColor(Color.GREEN);
            g.draw(selectedShape);
        }
        if (selectedText != null) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.PLAIN, selectedText.fontSize));
            g.drawString(selectedText.text, selectedText.x, selectedText.y);
        }
        g.setColor(Color.RED);
        if (dragCurrent != null && selectedShape == null && selectedText == null && dragBegin != null)
            g.fill(genRect(dragBegin, dragCurrent));
    }

    public static ColoredRectangle genRect(Point a, Point b) {
        if (a == null || b == null) return null; // Prevent null-pointer exception
        return new ColoredRectangle(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.abs(a.x - b.x), Math.abs(a.y - b.y), Color.BLUE);
    }

    public void addShape(Point a, Point b) {
        ColoredRectangle rect = genRect(a, b);
        if (rect != null) {
            shapes.add(rect);
            elements.add(rect);
            repaint();
        }
    }

    public void addText(String text, int x, int y) {
        texts.add(new TextShape(text, x, y));
        elements.add(new TextShape(text, x, y));
        repaint();
    }

    public void addGIF(URL url, int x, int y) throws IOException {
        Image image = new ImageIcon(url).getImage();
        gifs.add(new GIFShape(image, x, y, url));
        elements.add(new GIFShape(image, x, y, url));
        repaint();
    }

    public void undo() {
        if (elements.isEmpty()) return;
        Object lastElement = elements.remove(elements.size() - 1);
        if (lastElement instanceof TextShape) {
            texts.remove(lastElement);
        } else if (lastElement instanceof ColoredRectangle) {
            shapes.remove(lastElement);
        } else if (lastElement instanceof GIFShape) {
            gifs.remove(lastElement);
        }
        clearSelection();
        repaint();
    }

    public void reset() {
        shapes.clear();
        texts.clear();
        gifs.clear();
        clearSelection();
        repaint();
    }

    public void bringToFront() {
        if (selectedShape != null) {
            shapes.remove(selectedShape);
            shapes.add(selectedShape);
            elements.remove(selectedShape);
            elements.add(selectedShape);
            repaint();
        } else if (selectedText != null) {
            texts.remove(selectedText);
            texts.add(selectedText);
            elements.remove(selectedText);
            elements.add(selectedText);
            repaint();
        } else if (selectedGIF != null) {
            gifs.remove(selectedGIF);
            gifs.add(selectedGIF);
            elements.remove(selectedGIF);
            elements.add(selectedGIF);
            repaint();
        }
    }

    public void sendToBack() {
        if (selectedShape != null) {
            shapes.remove(selectedShape);
            shapes.add(0, selectedShape);
            elements.remove(selectedShape);
            elements.add(0, selectedShape);
            repaint();
        } else if (selectedText != null) {
            texts.remove(selectedText);
            texts.add(0, selectedText);
            elements.remove(selectedText);
            elements.add(0, selectedText);
            repaint();
        } else if (selectedGIF != null) {
            gifs.remove(selectedGIF);
            gifs.add(0, selectedGIF);
            elements.remove(selectedGIF);
            elements.add(0, selectedGIF);
            repaint();
        }
    }

    public void mousePressed(Point point) {
        // Check if a text is clicked
        for (int i = texts.size() - 1; i >= 0; i--) {
            TextShape text = texts.get(i);
            FontMetrics metrics = getFontMetrics(new Font("Arial", Font.PLAIN, text.fontSize));
            int textWidth = metrics.stringWidth(text.text);
            int textHeight = metrics.getHeight();
            if (new Rectangle(text.x, text.y - textHeight, textWidth, textHeight).contains(point)) {
                clearSelection();
                selectedText = text;
                dragBegin = point;
                return;
            }
        }

        // Check if a shape is clicked
        for (int i = shapes.size() - 1; i >= 0; i--) {
            ColoredRectangle shape = shapes.get(i);
            if (shape.contains(point)) {
                clearSelection();
                selectedShape = shape;
                dragBegin = point;
                return;
            }
        }

        // Check if a GIF is clicked
        for (int i = gifs.size() - 1; i >= 0; i--) {
            GIFShape gif = gifs.get(i);
            if (new Rectangle(gif.x, gif.y, gif.image.getWidth(this), gif.image.getHeight(this)).contains(point)) {
                clearSelection();
                selectedGIF = gif;
                dragBegin = point;
                return;
            }
        }
        // No shape or text selected, reset selection
        clearSelection();
        dragBegin = point;
    }

    public void mouseDragged(Point point) {
        if (selectedShape != null && dragBegin != null) {
            int dx = point.x - dragBegin.x;
            int dy = point.y - dragBegin.y;
            selectedShape.translate(dx, dy);
            dragBegin = point;
            repaint();
        } else if (selectedText != null && dragBegin != null) {
            int dx = point.x - dragBegin.x;
            int dy = point.y - dragBegin.y;
            selectedText.x += dx;
            selectedText.y += dy;
            dragBegin = point;
            repaint();
        } else if (selectedGIF != null && dragBegin != null) {
            int dx = point.x - dragBegin.x;
            int dy = point.y - dragBegin.y;
            selectedGIF.x += dx;
            selectedGIF.y += dy;
            dragBegin = point;
            repaint();
        } else {
            dragCurrent = point;
            repaint();
        }
    }

    public void mouseReleased(Point point) {
        if (selectedShape != null || selectedText != null || selectedGIF != null) {
            dragBegin = dragCurrent = null;
            repaint();
        } else if (dragBegin != null && !point.equals(dragBegin)) {
            addShape(dragBegin, point);
        }
        dragBegin = dragCurrent = null;
    }

    private void showPopupMenu(MouseEvent e) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem colorPicker = new JMenuItem("Change Color");

        if (selectedShape != null) {
            colorPicker.addActionListener(ev -> {
                Color newColor = JColorChooser.showDialog(this, "Choose Shape Color", selectedShape.color);
                if (newColor != null) {
                    selectedShape.color = newColor;
                    repaint();
                }
            });
        } else if (selectedText != null) {
            colorPicker.addActionListener(ev -> {
                Color newColor = JColorChooser.showDialog(this, "Choose Text Color", selectedText.color);
                if (newColor != null) {
                    selectedText.color = newColor;
                    repaint();
                }
            });
        } else {
            colorPicker.setEnabled(false); // Disable the menu item if nothing is selected
        }

        popupMenu.add(colorPicker);
        popupMenu.show(this, e.getX(), e.getY());
    }


    private void clearSelection() {
        selectedShape = null;
        selectedText = null;
        selectedGIF = null;
    }

    private static class TextShape {
        String text;
        int x, y;
        int fontSize;
        Color color;

        public TextShape(String text, int x, int y) {
            this(text, x, y, 20, Color.BLACK); // Default font size and color
        }

        public TextShape(String text, int x, int y, int fontSize, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.fontSize = fontSize;
            this.color = color;
        }
    }

    private static class ColoredRectangle extends Rectangle {
        Color color;

        public ColoredRectangle(int x, int y, int width, int height, Color color) {
            super(x, y, width, height);
            this.color = color;
        }
    }

    public void saveDrawData(String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Save shapes
            writer.write("SHAPES\n");
            for (ColoredRectangle rect : shapes) {
                writer.write(String.format("%d,%d,%d,%d,%d\n",
                        rect.x, rect.y, rect.width, rect.height, rect.color.getRGB()));
            }
            // Save texts
            writer.write("TEXTS\n");
            for (TextShape text : texts) {
                writer.write(String.format("%s,%d,%d,%d,%d\n",
                        text.text.replace(",", "\\,"), text.x, text.y, text.fontSize, text.color.getRGB()));
            }
            // Save GIFs (URL only, positions)
            writer.write("GIFS\n");
            for (GIFShape gif : gifs) {
                writer.write(String.format("%s,%d,%d\n", gif.url.toString(), gif.x, gif.y));
            }
        }
    }

    public void loadDrawData(String filePath) throws IOException {
        List<ColoredRectangle> loadedShapes = new ArrayList<>();
        List<TextShape> loadedTexts = new ArrayList<>();
        List<GIFShape> loadedGIFs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean readingShapes = false;
            boolean readingTexts = false;
            boolean readingGIFs = false;

            while ((line = reader.readLine()) != null) {
                if (line.equals("SHAPES")) {
                    readingShapes = true;
                    readingTexts = false;
                    readingGIFs = false;
                    continue;
                } else if (line.equals("TEXTS")) {
                    readingShapes = false;
                    readingTexts = true;
                    readingGIFs = false;
                    continue;
                } else if (line.equals("GIFS")) {
                    readingShapes = false;
                    readingTexts = false;
                    readingGIFs = true;
                    continue;
                }

                if (readingShapes) {
                    String[] parts = line.split(",");
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int width = Integer.parseInt(parts[2]);
                    int height = Integer.parseInt(parts[3]);
                    Color color = new Color(Integer.parseInt(parts[4]));
                    loadedShapes.add(new ColoredRectangle(x, y, width, height, color));
                } else if (readingTexts) {
                    String[] parts = line.split(",", 5);
                    String text = parts[0].replace("\\,", ",");
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int fontSize = Integer.parseInt(parts[3]);
                    Color color = new Color(Integer.parseInt(parts[4]));
                    loadedTexts.add(new TextShape(text, x, y, fontSize, color));
                } else if (readingGIFs) {
                    String[] parts = line.split(",", 3);
                    String urlString = parts[0];
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    URL url = new URL(urlString);
                    Image image = new ImageIcon(url).getImage();
                    loadedGIFs.add(new GIFShape(image, x, y, url));
                }
            }
        }

        this.shapes = loadedShapes;
        this.texts = loadedTexts;
        this.gifs = loadedGIFs;
        repaint();
    }

    public static class GIFShape {
        public Image image;
        public int x, y;
        public URL url;

        public GIFShape(Image image, int x, int y, URL url) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.url = url;
        }
    }

    public void startAnimation() {
        if (isAnimating) {
            return; // Prevent starting new threads if already animating
        }
        isAnimating = true;
        for (ColoredRectangle rect : shapes) {
            Thread thread = new Thread(() -> animateShape(rect));
            animationThreads.add(thread);
            thread.start();
        }
        for (TextShape text : texts) {
            Thread thread = new Thread(() -> animateText(text));
            animationThreads.add(thread);
            thread.start();
        }
        for (GIFShape gif : gifs) {
            Thread thread = new Thread(() -> animateGIF(gif));
            animationThreads.add(thread);
            thread.start();
        }
    }

    public void stopAnimation() {
        isAnimating = false;
        for (Thread thread : animationThreads) {
            thread.interrupt();
        }
        animationThreads.clear();
    }

    private void animateShape(ColoredRectangle rect) {
        while (isAnimating) {
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            if (rect.x + rect.width >= panelWidth || rect.x <= 0) dx = -dx;
            if (rect.y + rect.height >= panelHeight || rect.y <= 0) dy = -dy;
            rect.x += dx;
            rect.y += dy;
            repaint();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void animateText(TextShape text) {
        while (isAnimating) {
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            if (text.x >= panelWidth || text.x <= 0) dx = -dx;
            if (text.y >= panelHeight || text.y <= 0) dy = -dy;
            text.x += dx;
            text.y += dy;
            repaint();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void animateGIF(GIFShape gif) {
        while (isAnimating) {
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            if (gif.x + gif.image.getWidth(null) >= panelWidth || gif.x <= 0) dx = -dx;
            if (gif.y + gif.image.getHeight(null) >= panelHeight || gif.y <= 0) dy = -dy;
            gif.x += dx;
            gif.y += dy;
            repaint();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


    public void connectToServer(String serverAddress) {
        try {
            drawClient = new DrawClient(serverAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendDrawData(DrawData data) {
        if (drawClient != null) {
            drawClient.sendDrawData(data.toString()); // JSONに変換する処理が必要
        }
    }

    public static void applyRemoteDrawData(String data) {
        // JSONをパースして、描画処理を実装
    }

}
