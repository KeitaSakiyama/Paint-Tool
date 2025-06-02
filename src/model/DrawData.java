package model;

public class DrawData {
    public String type; // "RECTANGLE" or "TEXT" or "GIF"
    public int x, y, width, height, fontSize;
    public String text, color, url;

    public DrawData(String type, int x, int y, int width, int height, String color) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
    }

    public DrawData(String type, int x, int y, int fontSize, String text, String color) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.fontSize = fontSize;
        this.text = text;
        this.color = color;
    }

    public DrawData(String type, int x, int y, String url) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.url = url;
    }
}
