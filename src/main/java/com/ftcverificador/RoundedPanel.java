package com.ftcverificador;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class RoundedPanel extends JPanel {

    private final int radius;
    private Color fillColor;

    public RoundedPanel(int radius, Color fillColor) {
        this.radius = radius;
        this.fillColor = fillColor;
        setOpaque(false);
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
        graphics2D.setColor(fillColor);
        graphics2D.fill(new RoundRectangle2D.Float(
                0,
                0,
                getWidth(),
                getHeight(),
                radius,
                radius
        ));
        graphics2D.dispose();
        super.paintComponent(graphics);
    }
}

