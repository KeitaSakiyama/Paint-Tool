package model;

import java.awt.*;

public class LineShape extends Shape {
    public int x2, y2;
    public Color color;

    public LineShape(int x, int y, int x2, int y2, Color color) {
        super(x, y);
        this.x2 = x2;
        this.y2 = y2;
        this.color = color;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.drawLine(x, y, x2, y2);
    }
}