package com.ftcverificador;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;

public final class AppTheme {

    public static final Color PRIMARY = new Color(164, 24, 48);
    public static final Color PRIMARY_DARK = new Color(112, 15, 32);
    public static final Color PRIMARY_HOVER = new Color(188, 35, 59);
    public static final Color PRIMARY_PRESSED = new Color(130, 18, 39);

    public static final Color BACKGROUND = new Color(245, 246, 248);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SURFACE_SOFT = new Color(250, 247, 248);
    public static final Color BORDER = new Color(223, 225, 230);

    public static final Color TEXT = new Color(38, 42, 49);
    public static final Color TEXT_SECONDARY = new Color(105, 110, 120);
    public static final Color SUCCESS = new Color(32, 132, 82);
    public static final Color ERROR = new Color(181, 37, 51);

    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_MEDIUM = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 25);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_NUMBER = new Font("Segoe UI", Font.BOLD, 28);

    private AppTheme() {
    }

    public static void apply() {
        UIManager.put("Label.font", FONT_REGULAR);
        UIManager.put("Button.font", FONT_MEDIUM);
        UIManager.put("TextField.font", FONT_REGULAR);
        UIManager.put("TextArea.font", new Font("Consolas", Font.PLAIN, 13));
        UIManager.put("ProgressBar.font", FONT_MEDIUM);
        UIManager.put("OptionPane.messageFont", FONT_REGULAR);
        UIManager.put("OptionPane.buttonFont", FONT_MEDIUM);

        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("TextField.selectionBackground", new Color(229, 184, 192));
        UIManager.put("ProgressBar.foreground", PRIMARY);
        UIManager.put("ProgressBar.background", new Color(234, 235, 238));
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                new RoundedBorder(BORDER, 18, 1),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)
        );
    }
}
