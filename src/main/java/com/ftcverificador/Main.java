package com.ftcverificador;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Mantém o visual padrão quando o tema do sistema não estiver disponível.
            }

            AppTheme.apply();
            new MainWindow().setVisible(true);
        });
    }
}

