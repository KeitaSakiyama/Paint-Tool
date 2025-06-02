package model;

import java.awt.*;

public abstract class Shape {
    public int x, y;

    public Shape(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public abstract void draw(Graphics g);
}