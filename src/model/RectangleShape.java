package model;

import java.awt.*;

public class RectangleShape extends Shape {
    public int width, height;
    public Color color;

    public RectangleShape(int x, int y, int width, int height, Color color) {
        super(x, y);
        this.width = width;
        this.height = height;
        this.color = color;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, y, width, height);
    }
}