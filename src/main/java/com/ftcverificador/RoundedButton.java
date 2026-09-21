package com.ftcverificador;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class RoundedButton extends JButton {

    public enum Style {
        PRIMARY,
        SECONDARY,
        GHOST
    }

    private final Style style;
    private final int radius;

    public RoundedButton(String text, Style style) {
        super(text);
        this.style = style;
        this.radius = 14;

        setFont(AppTheme.FONT_MEDIUM);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setBorderPainted(false);
        setMargin(new Insets(11, 18, 11, 18));
        setForeground(style == Style.PRIMARY ? Color.WHITE : AppTheme.PRIMARY_DARK);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        Color background = getBackgroundForState();

        if (style != Style.GHOST || getModel().isRollover() || getModel().isPressed()) {
            graphics2D.setColor(background);
            graphics2D.fill(new RoundRectangle2D.Float(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    radius,
                    radius
            ));
        }

        if (style == Style.SECONDARY) {
            graphics2D.setColor(AppTheme.BORDER);
            graphics2D.draw(new RoundRectangle2D.Float(
                    0.5f,
                    0.5f,
                    getWidth() - 1,
                    getHeight() - 1,
                    radius,
                    radius
            ));
        }

        graphics2D.dispose();
        super.paintComponent(graphics);
    }

    private Color getBackgroundForState() {
        if (!isEnabled()) {
            return style == Style.PRIMARY
                    ? new Color(198, 153, 161)
                    : new Color(241, 242, 244);
        }

        if (style == Style.PRIMARY) {
            if (getModel().isPressed()) {
                return AppTheme.PRIMARY_PRESSED;
            }
            if (getModel().isRollover()) {
                return AppTheme.PRIMARY_HOVER;
            }
            return AppTheme.PRIMARY;
        }

        if (getModel().isPressed()) {
            return new Color(239, 225, 228);
        }
        if (getModel().isRollover()) {
            return new Color(248, 239, 241);
        }
        return style == Style.SECONDARY ? Color.WHITE : new Color(0, 0, 0, 0);
    }
}

