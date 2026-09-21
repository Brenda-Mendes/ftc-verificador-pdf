package com.ftcverificador;

import javax.swing.border.AbstractBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class RoundedBorder extends AbstractBorder {

    private final Color color;
    private final int radius;
    private final int thickness;

    public RoundedBorder(Color color, int radius, int thickness) {
        this.color = color;
        this.radius = radius;
        this.thickness = thickness;
    }

    @Override
    public Insets getBorderInsets(Component component) {
        int padding = Math.max(8, radius / 2);
        return new Insets(padding, padding + 2, padding, padding + 2);
    }

    @Override
    public Insets getBorderInsets(Component component, Insets insets) {
        Insets calculated = getBorderInsets(component);
        insets.top = calculated.top;
        insets.left = calculated.left;
        insets.bottom = calculated.bottom;
        insets.right = calculated.right;
        return insets;
    }

    @Override
    public void paintBorder(
            Component component,
            Graphics graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
        graphics2D.setColor(color);
        graphics2D.setStroke(new BasicStroke(thickness));

        float offset = thickness / 2.0f;
        graphics2D.draw(new RoundRectangle2D.Float(
                x + offset,
                y + offset,
                width - thickness,
                height - thickness,
                radius,
                radius
        ));
        graphics2D.dispose();
    }
}

