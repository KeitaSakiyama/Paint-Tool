package model;

import java.awt.*;

public class CircleShape extends Shape {
    public int radius;
    public Color color;

    public CircleShape(int x, int y, int radius, Color color) {
        super(x, y);
        this.radius = radius;
        this.color = color;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.fillOval(x, y, radius * 2, radius * 2);
    }
}