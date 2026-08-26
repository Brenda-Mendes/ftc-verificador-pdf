package com.ftcverificador;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainWindow extends JFrame {

    /*
     * Campos de seleção.
     */
    private final JTextField pdfFolderField =
            createPathField();

    private final JTextField spreadsheetField =
            createPathField();

    private final JTextField xmlZipField =
            createPathField();

    private final JTextField outputFolderField =
            createPathField();

    /*
     * Botões de seleção.
     */
    private final RoundedButton selectPdfFolderButton =
            new RoundedButton(
                    "Selecionar",
                    RoundedButton.Style.SECONDARY
            );

    private final RoundedButton selectSpreadsheetButton =
            new RoundedButton(
                    "Selecionar",
                    RoundedButton.Style.SECONDARY
            );

    private final RoundedButton selectXmlZipButton =
            new RoundedButton(
                    "Selecionar",
                    RoundedButton.Style.SECONDARY
            );

    private final RoundedButton selectOutputFolderButton =
            new RoundedButton(
                    "Selecionar",
                    RoundedButton.Style.SECONDARY
            );

    /*
     * Botões principais.
     */
    private final RoundedButton startButton =
            new RoundedButton(
                    "Iniciar verificação",
                    RoundedButton.Style.PRIMARY
            );

    private final RoundedButton openSpreadsheetButton =
            new RoundedButton(
                    "Abrir planilha",
                    RoundedButton.Style.SECONDARY
            );

    private final RoundedButton openOutputButton =
            new RoundedButton(
                    "Abrir pasta de saída",
                    RoundedButton.Style.SECONDARY
            );

    private final RoundedButton toggleDetailsButton =
            new RoundedButton(
                    "Ver detalhes",
                    RoundedButton.Style.GHOST
            );

    /*
     * Área de processamento.
     */
    private final JProgressBar progressBar =
            new JProgressBar(
                    0,
                    100
            );

    private final JLabel statusTitle =
            new JLabel(
                    "Aguardando arquivos"
            );

    private final JLabel statusDetail =
            new JLabel(
                    "Selecione os quatro caminhos para iniciar a verificação."
            );

    private final JLabel statusBadge =
            createStatusBadge(
                    "PRONTO",
                    AppTheme.TEXT_SECONDARY
            );

    /*
     * Resumo.
     */
    private final JLabel filesCountValue =
            createSummaryValue(
                    "0"
            );

    private final JLabel matchesCountValue =
            createSummaryValue(
                    "0"
            );

    /*
     * Detalhes.
     */
    private final JTextArea statusArea =
            new JTextArea();

    private final JScrollPane detailsScrollPane =
            new JScrollPane(
                    statusArea
            );

    private final JPanel detailsContainer =
            new JPanel(
                    new BorderLayout()
            );

    /*
     * Caminhos selecionados.
     */
    private Path pdfFolder;
    private Path spreadsheet;
    private Path xmlZip;
    private Path outputFolder;

    public MainWindow() {

        super(
                "FTC Verificador PDF"
        );

        setIconImages(
                AppIcon.loadImages()
        );

        configureWindow();
        configureActions();
        configureInitialState();
    }

    private void configureWindow() {

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        setMinimumSize(
                new Dimension(
                        940,
                        760
                )
        );

        setSize(
                1020,
                850
        );

        setLocationRelativeTo(
                null
        );

        JPanel root =
                new JPanel(
                        new BorderLayout()
                );

        root.setBackground(
                AppTheme.BACKGROUND
        );

        setContentPane(
                root
        );

        root.add(
                createHeader(),
                BorderLayout.NORTH
        );

        root.add(
                createBody(),
                BorderLayout.CENTER
        );
    }

    private JPanel createHeader() {

        JPanel header =
                new JPanel(
                        new BorderLayout()
                ) {

                    @Override
                    protected void paintComponent(
                            Graphics graphics
                    ) {

                        Graphics2D graphics2D =
                                (Graphics2D) graphics.create();

                        graphics2D.setRenderingHint(
                                RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON
                        );

                        GradientPaint gradient =
                                new GradientPaint(
                                        0,
                                        0,
                                        AppTheme.PRIMARY_DARK,
                                        getWidth(),
                                        getHeight(),
                                        AppTheme.PRIMARY
                                );

                        graphics2D.setPaint(
                                gradient
                        );

                        graphics2D.fillRect(
                                0,
                                0,
                                getWidth(),
                                getHeight()
                        );

                        graphics2D.dispose();
                    }
                };

        header.setOpaque(
                false
        );

        header.setBorder(
                new EmptyBorder(
                        24,
                        34,
                        24,
                        34
                )
        );

        JPanel texts =
                new JPanel();

        texts.setOpaque(
                false
        );

        texts.setLayout(
                new BoxLayout(
                        texts,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title =
                new JLabel(
                        "FTC Verificador PDF"
                );

        title.setFont(
                AppTheme.FONT_TITLE
        );

        title.setForeground(
                Color.WHITE
        );

        JLabel subtitle =
                new JLabel(
                        "Cruze contas, relacione XMLs às DANFEs e organize os arquivos automaticamente."
                );

        subtitle.setFont(
                AppTheme.FONT_SUBTITLE
        );

        subtitle.setForeground(
                new Color(
                        247,
                        226,
                        231
                )
        );

        texts.add(
                title
        );

        texts.add(
                Box.createVerticalStrut(
                        5
                )
        );

        texts.add(
                subtitle
        );

        JLabel mark =
                new JLabel(
                        "FTC"
                );

        mark.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        mark.setVerticalAlignment(
                SwingConstants.CENTER
        );

        mark.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        18
                )
        );

        mark.setForeground(
                AppTheme.PRIMARY_DARK
        );

        mark.setOpaque(
                true
        );

        mark.setBackground(
                Color.WHITE
        );

        mark.setPreferredSize(
                new Dimension(
                        64,
                        46
                )
        );

        mark.setBorder(
                new RoundedBorder(
                        new Color(
                                255,
                                255,
                                255,
                                100
                        ),
                        14,
                        1
                )
        );

        header.add(
                texts,
                BorderLayout.CENTER
        );

        header.add(
                mark,
                BorderLayout.EAST
        );

        return header;
    }

    private Component createBody() {

        JPanel content =
                new JPanel();

        content.setBackground(
                AppTheme.BACKGROUND
        );

        content.setLayout(
                new BoxLayout(
                        content,
                        BoxLayout.Y_AXIS
                )
        );

        content.setBorder(
                new EmptyBorder(
                        24,
                        30,
                        28,
                        30
                )
        );

        JPanel selectionCard =
                createSelectionCard();

        JPanel processingCard =
                createProcessingCard();

        JPanel resultCard =
                createResultCard();

        JPanel actionBar =
                createActionBar();

        selectionCard.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        processingCard.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        resultCard.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        actionBar.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        content.add(
                selectionCard
        );

        content.add(
                Box.createVerticalStrut(
                        16
                )
        );

        content.add(
                processingCard
        );

        content.add(
                Box.createVerticalStrut(
                        16
                )
        );

        content.add(
                resultCard
        );

        content.add(
                Box.createVerticalStrut(
                        16
                )
        );

        content.add(
                actionBar
        );

        JScrollPane pageScroll =
                new JScrollPane(
                        content
                );

        pageScroll.setBorder(
                null
        );

        pageScroll.setBackground(
                AppTheme.BACKGROUND
        );

        pageScroll
                .getViewport()
                .setBackground(
                        AppTheme.BACKGROUND
                );

        pageScroll
                .getVerticalScrollBar()
                .setUnitIncrement(
                        14
                );

        return pageScroll;
    }

    private JPanel createSelectionCard() {

        RoundedPanel card =
                new RoundedPanel(
                        20,
                        AppTheme.SURFACE
                );

        card.setLayout(
                new BorderLayout(
                        0,
                        16
                )
        );

        card.setBorder(
                AppTheme.cardBorder()
        );

        JPanel heading =
                createSectionHeading(
                        "Arquivos da verificação",
                        "A planilha será utilizada somente para consulta e não será modificada."
                );

        card.add(
                heading,
                BorderLayout.NORTH
        );

        JPanel form =
                new JPanel(
                        new GridBagLayout()
                );

        form.setOpaque(
                false
        );

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.insets =
                new Insets(
                        8,
                        0,
                        8,
                        0
                );

        constraints.fill =
                GridBagConstraints.HORIZONTAL;

        constraints.weightx =
                1;

        constraints.gridx =
                0;

        constraints.gridy =
                0;

        /*
         * 1 - PDFs
         */
        form.add(
                createSelectorRow(
                        "1",
                        "Pasta dos PDFs",
                        "Todos os arquivos PDF da pasta e das subpastas serão analisados.",
                        pdfFolderField,
                        selectPdfFolderButton
                ),
                constraints
        );

        /*
         * 2 - Planilha
         */
        constraints.gridy++;

        form.add(
                createSelectorRow(
                        "2",
                        "Planilha Excel — somente leitura",
                        "Deve conter as colunas Conta e Test name (initial).",
                        spreadsheetField,
                        selectSpreadsheetButton
                ),
                constraints
        );

        /*
         * 3 - ZIP dos XMLs
         */
        constraints.gridy++;

        form.add(
                createSelectorRow(
                        "3",
                        "ZIP dos XMLs",
                        "Todos os XMLs do arquivo ZIP e de suas subpastas serão analisados.",
                        xmlZipField,
                        selectXmlZipButton
                ),
                constraints
        );

        /*
         * 4 - Saída
         */
        constraints.gridy++;

        form.add(
                createSelectorRow(
                        "4",
                        "Pasta de saída",
                        "Os PDFs e XMLs serão organizados em subpastas pelo Test name (initial).",
                        outputFolderField,
                        selectOutputFolderButton
                ),
                constraints
        );

        JPanel center =
                new JPanel();

        center.setOpaque(
                false
        );

        center.setLayout(
                new BoxLayout(
                        center,
                        BoxLayout.Y_AXIS
                )
        );

        center.add(
                form
        );

        center.add(
                Box.createVerticalStrut(
                        12
                )
        );

        center.add(
                createSpreadsheetRequirementsPanel()
        );

        card.add(
                center,
                BorderLayout.CENTER
        );

        return card;
    }

    private JPanel createSpreadsheetRequirementsPanel() {

        RoundedPanel panel =
                new RoundedPanel(
                        16,
                        new Color(
                                253,
                                244,
                                246
                        )
                );

        panel.setLayout(
                new BorderLayout(
                        14,
                        0
                )
        );

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        new RoundedBorder(
                                new Color(
                                        230,
                                        190,
                                        199
                                ),
                                16,
                                1
                        ),
                        BorderFactory.createEmptyBorder(
                                13,
                                15,
                                13,
                                15
                        )
                )
        );

        JLabel icon =
                new JLabel(
                        "i",
                        SwingConstants.CENTER
                );

        icon.setOpaque(
                true
        );

        icon.setBackground(
                AppTheme.PRIMARY
        );

        icon.setForeground(
                Color.WHITE
        );

        icon.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        14
                )
        );

        icon.setPreferredSize(
                new Dimension(
                        30,
                        30
                )
        );

        icon.setBorder(
                new RoundedBorder(
                        AppTheme.PRIMARY,
                        15,
                        1
                )
        );

        JLabel columns =
                new JLabel(
                        "<html><b>Colunas obrigatórias:</b> Conta e " +
                                "Test name (initial)</html>"
                );

        columns.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        13
                )
        );

        columns.setForeground(
                AppTheme.TEXT
        );

        panel.add(
                icon,
                BorderLayout.WEST
        );

        panel.add(
                columns,
                BorderLayout.CENTER
        );

        return panel;
    }

    private JPanel createSelectorRow(
            String step,
            String title,
            String description,
            JTextField field,
            RoundedButton button
    ) {

        RoundedPanel row =
                new RoundedPanel(
                        16,
                        AppTheme.SURFACE_SOFT
                );

        row.setLayout(
                new GridBagLayout()
        );

        row.setBorder(
                BorderFactory.createEmptyBorder(
                        13,
                        15,
                        13,
                        15
                )
        );

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.insets =
                new Insets(
                        0,
                        0,
                        0,
                        12
                );

        constraints.anchor =
                GridBagConstraints.WEST;

        JLabel stepLabel =
                new JLabel(
                        step,
                        SwingConstants.CENTER
                );

        stepLabel.setOpaque(
                true
        );

        stepLabel.setBackground(
                AppTheme.PRIMARY
        );

        stepLabel.setForeground(
                Color.WHITE
        );

        stepLabel.setFont(
                AppTheme.FONT_MEDIUM
        );

        stepLabel.setPreferredSize(
                new Dimension(
                        34,
                        34
                )
        );

        stepLabel.setBorder(
                new RoundedBorder(
                        AppTheme.PRIMARY,
                        17,
                        1
                )
        );

        constraints.gridx =
                0;

        constraints.gridy =
                0;

        constraints.gridheight =
                2;

        constraints.weightx =
                0;

        row.add(
                stepLabel,
                constraints
        );

        JLabel titleLabel =
                new JLabel(
                        title
                );

        titleLabel.setFont(
                AppTheme.FONT_MEDIUM
        );

        titleLabel.setForeground(
                AppTheme.TEXT
        );

        constraints.gridx =
                1;

        constraints.gridy =
                0;

        constraints.gridheight =
                1;

        constraints.weightx =
                0;

        constraints.insets =
                new Insets(
                        0,
                        0,
                        2,
                        14
                );

        row.add(
                titleLabel,
                constraints
        );

        JLabel descriptionLabel =
                new JLabel(
                        description
                );

        descriptionLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        12
                )
        );

        descriptionLabel.setForeground(
                AppTheme.TEXT_SECONDARY
        );

        constraints.gridx =
                1;

        constraints.gridy =
                1;

        constraints.weightx =
                0;

        constraints.insets =
                new Insets(
                        0,
                        0,
                        0,
                        14
                );

        row.add(
                descriptionLabel,
                constraints
        );

        constraints.gridx =
                2;

        constraints.gridy =
                0;

        constraints.gridheight =
                2;

        constraints.weightx =
                1;

        constraints.fill =
                GridBagConstraints.HORIZONTAL;

        constraints.insets =
                new Insets(
                        0,
                        0,
                        0,
                        10
                );

        row.add(
                field,
                constraints
        );

        constraints.gridx =
                3;

        constraints.weightx =
                0;

        constraints.fill =
                GridBagConstraints.NONE;

        constraints.insets =
                new Insets(
                        0,
                        0,
                        0,
                        0
                );

        row.add(
                button,
                constraints
        );

        return row;
    }

    private JPanel createProcessingCard() {

        RoundedPanel card =
                new RoundedPanel(
                        20,
                        AppTheme.SURFACE
                );

        card.setLayout(
                new BorderLayout(
                        0,
                        14
                )
        );

        card.setBorder(
                AppTheme.cardBorder()
        );

        JPanel top =
                new JPanel(
                        new BorderLayout(
                                10,
                                0
                        )
                );

        top.setOpaque(
                false
        );

        JPanel statusTexts =
                new JPanel();

        statusTexts.setOpaque(
                false
        );

        statusTexts.setLayout(
                new BoxLayout(
                        statusTexts,
                        BoxLayout.Y_AXIS
                )
        );

        statusTitle.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        16
                )
        );

        statusTitle.setForeground(
                AppTheme.TEXT
        );

        statusDetail.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        13
                )
        );

        statusDetail.setForeground(
                AppTheme.TEXT_SECONDARY
        );

        statusTexts.add(
                statusTitle
        );

        statusTexts.add(
                Box.createVerticalStrut(
                        3
                )
        );

        statusTexts.add(
                statusDetail
        );

        top.add(
                statusTexts,
                BorderLayout.CENTER
        );

        top.add(
                statusBadge,
                BorderLayout.EAST
        );

        progressBar.setValue(
                0
        );

        progressBar.setStringPainted(
                true
        );

        progressBar.setForeground(
                AppTheme.PRIMARY
        );

        progressBar.setBackground(
                new Color(
                        235,
                        236,
                        239
                )
        );

        progressBar.setBorderPainted(
                false
        );

        progressBar.setPreferredSize(
                new Dimension(
                        100,
                        18
                )
        );

        statusArea.setEditable(
                false
        );

        statusArea.setLineWrap(
                true
        );

        statusArea.setWrapStyleWord(
                true
        );

        statusArea.setBackground(
                new Color(
                        251,
                        251,
                        252
                )
        );

        statusArea.setForeground(
                AppTheme.TEXT
        );

        statusArea.setBorder(
                new EmptyBorder(
                        10,
                        12,
                        10,
                        12
                )
        );

        detailsScrollPane.setBorder(
                new RoundedBorder(
                        AppTheme.BORDER,
                        14,
                        1
                )
        );

        detailsScrollPane.setPreferredSize(
                new Dimension(
                        100,
                        150
                )
        );

        detailsContainer.setOpaque(
                false
        );

        detailsContainer.add(
                detailsScrollPane,
                BorderLayout.CENTER
        );

        detailsContainer.setVisible(
                false
        );

        JPanel center =
                new JPanel();

        center.setOpaque(
                false
        );

        center.setLayout(
                new BoxLayout(
                        center,
                        BoxLayout.Y_AXIS
                )
        );

        center.add(
                progressBar
        );

        center.add(
                Box.createVerticalStrut(
                        8
                )
        );

        JPanel detailControl =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                0,
                                0
                        )
                );

        detailControl.setOpaque(
                false
        );

        detailControl.add(
                toggleDetailsButton
        );

        center.add(
                detailControl
        );

        center.add(
                detailsContainer
        );

        card.add(
                top,
                BorderLayout.NORTH
        );

        card.add(
                center,
                BorderLayout.CENTER
        );

        return card;
    }

    private JPanel createResultCard() {

        RoundedPanel card =
                new RoundedPanel(
                        20,
                        AppTheme.SURFACE
                );

        card.setLayout(
                new BorderLayout(
                        0,
                        14
                )
        );

        card.setBorder(
                AppTheme.cardBorder()
        );

        card.add(
                createSectionHeading(
                        "Resumo da execução",
                        "Os indicadores serão atualizados quando o processamento terminar."
                ),
                BorderLayout.NORTH
        );

        JPanel cards =
                new JPanel(
                        new GridBagLayout()
                );

        cards.setOpaque(
                false
        );

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.gridy =
                0;

        constraints.weightx =
                1;

        constraints.fill =
                GridBagConstraints.HORIZONTAL;

        constraints.gridx =
                0;

        constraints.insets =
                new Insets(
                        0,
                        0,
                        0,
                        8
                );

        cards.add(
                createSummaryCard(
                        "PDFs analisados",
                        filesCountValue,
                        "Arquivos processados nesta execução"
                ),
                constraints
        );

        constraints.gridx =
                1;

        constraints.insets =
                new Insets(
                        0,
                        8,
                        0,
                        0
                );

        cards.add(
                createSummaryCard(
                        "Contas encontradas",
                        matchesCountValue,
                        "Correspondências localizadas na planilha"
                ),
                constraints
        );

        card.add(
                cards,
                BorderLayout.CENTER
        );

        return card;
    }

    private JPanel createSummaryCard(
            String title,
            JLabel value,
            String description
    ) {

        RoundedPanel panel =
                new RoundedPanel(
                        16,
                        AppTheme.SURFACE_SOFT
                );

        panel.setLayout(
                new BorderLayout(
                        12,
                        0
                )
        );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        16,
                        18,
                        16,
                        18
                )
        );

        JPanel texts =
                new JPanel();

        texts.setOpaque(
                false
        );

        texts.setLayout(
                new BoxLayout(
                        texts,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel titleLabel =
                new JLabel(
                        title
                );

        titleLabel.setFont(
                AppTheme.FONT_MEDIUM
        );

        titleLabel.setForeground(
                AppTheme.TEXT
        );

        JLabel descriptionLabel =
                new JLabel(
                        description
                );

        descriptionLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        12
                )
        );

        descriptionLabel.setForeground(
                AppTheme.TEXT_SECONDARY
        );

        texts.add(
                titleLabel
        );

        texts.add(
                Box.createVerticalStrut(
                        4
                )
        );

        texts.add(
                descriptionLabel
        );

        panel.add(
                texts,
                BorderLayout.CENTER
        );

        panel.add(
                value,
                BorderLayout.EAST
        );

        return panel;
    }

    private JPanel createActionBar() {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setOpaque(
                false
        );

        JPanel secondaryActions =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                8,
                                0
                        )
                );

        secondaryActions.setOpaque(
                false
        );

        secondaryActions.add(
                openSpreadsheetButton
        );

        secondaryActions.add(
                openOutputButton
        );

        JPanel primaryAction =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                0,
                                0
                        )
                );

        primaryAction.setOpaque(
                false
        );

        primaryAction.add(
                startButton
        );

        panel.add(
                secondaryActions,
                BorderLayout.WEST
        );

        panel.add(
                primaryAction,
                BorderLayout.EAST
        );

        return panel;
    }

    private JPanel createSectionHeading(
            String title,
            String subtitle
    ) {

        JPanel panel =
                new JPanel();

        panel.setOpaque(
                false
        );

        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel titleLabel =
                new JLabel(
                        title
                );

        titleLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        17
                )
        );

        titleLabel.setForeground(
                AppTheme.TEXT
        );

        JLabel subtitleLabel =
                new JLabel(
                        subtitle
                );

        subtitleLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        13
                )
        );

        subtitleLabel.setForeground(
                AppTheme.TEXT_SECONDARY
        );

        panel.add(
                titleLabel
        );

        panel.add(
                Box.createVerticalStrut(
                        3
                )
        );

        panel.add(
                subtitleLabel
        );

        return panel;
    }

    private void configureActions() {

        selectPdfFolderButton.addActionListener(
                event ->
                        selectPdfFolder()
        );

        selectSpreadsheetButton.addActionListener(
                event ->
                        selectSpreadsheet()
        );

        selectXmlZipButton.addActionListener(
                event ->
                        selectXmlZip()
        );

        selectOutputFolderButton.addActionListener(
                event ->
                        selectOutputFolder()
        );

        startButton.addActionListener(
                event ->
                        startProcessing()
        );

        openOutputButton.addActionListener(
                event ->
                        openPath(
                                outputFolder
                        )
        );

        openSpreadsheetButton.addActionListener(
                event ->
                        openPath(
                                spreadsheet
                        )
        );

        toggleDetailsButton.addActionListener(
                event ->
                        toggleDetails()
        );
    }

    private void configureInitialState() {

        openOutputButton.setEnabled(
                false
        );

        openSpreadsheetButton.setEnabled(
                false
        );

        updateStartButtonState();
    }

    /*
     * Seleção da pasta dos PDFs.
     */
    private void selectPdfFolder() {

        JFileChooser chooser =
                createDirectoryChooser(
                        "Selecione a pasta que contém os PDFs"
                );

        if (
                chooser.showOpenDialog(this)
                        == JFileChooser.APPROVE_OPTION
        ) {

            pdfFolder =
                    chooser
                            .getSelectedFile()
                            .toPath()
                            .toAbsolutePath()
                            .normalize();

            pdfFolderField.setText(
                    pdfFolder.toString()
            );

            /*
             * Mantém o comportamento anterior:
             * se ainda não houver saída selecionada,
             * sugere uma pasta "resultado".
             */
            if (outputFolder == null) {

                outputFolder =
                        pdfFolder.resolve(
                                "resultado"
                        );

                outputFolderField.setText(
                        outputFolder.toString()
                );
            }

            updateStartButtonState();
        }
    }

    /*
     * Seleção da planilha.
     */
    private void selectSpreadsheet() {

        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "Selecione a planilha Excel"
        );

        chooser.setFileSelectionMode(
                JFileChooser.FILES_ONLY
        );

        chooser.setAcceptAllFileFilterUsed(
                false
        );

        chooser.setFileFilter(
                new FileNameExtensionFilter(
                        "Planilhas Excel (*.xlsx)",
                        "xlsx"
                )
        );

        if (
                chooser.showOpenDialog(this)
                        == JFileChooser.APPROVE_OPTION
        ) {

            spreadsheet =
                    chooser
                            .getSelectedFile()
                            .toPath()
                            .toAbsolutePath()
                            .normalize();

            spreadsheetField.setText(
                    spreadsheet.toString()
            );

            updateStartButtonState();
        }
    }

    /*
     * NOVO:
     * Seleção do ZIP contendo os XMLs.
     */
    private void selectXmlZip() {

        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "Selecione o arquivo ZIP contendo os XMLs"
        );

        chooser.setFileSelectionMode(
                JFileChooser.FILES_ONLY
        );

        chooser.setAcceptAllFileFilterUsed(
                false
        );

        chooser.setFileFilter(
                new FileNameExtensionFilter(
                        "Arquivos ZIP (*.zip)",
                        "zip"
                )
        );

        if (
                chooser.showOpenDialog(this)
                        == JFileChooser.APPROVE_OPTION
        ) {

            xmlZip =
                    chooser
                            .getSelectedFile()
                            .toPath()
                            .toAbsolutePath()
                            .normalize();

            xmlZipField.setText(
                    xmlZip.toString()
            );

            updateStartButtonState();
        }
    }

    /*
     * Seleção da pasta de saída.
     */
    private void selectOutputFolder() {

        JFileChooser chooser =
                createDirectoryChooser(
                        "Selecione a pasta de saída"
                );

        if (
                chooser.showOpenDialog(this)
                        == JFileChooser.APPROVE_OPTION
        ) {

            outputFolder =
                    chooser
                            .getSelectedFile()
                            .toPath()
                            .toAbsolutePath()
                            .normalize();

            outputFolderField.setText(
                    outputFolder.toString()
            );

            updateStartButtonState();
        }
    }

    private JFileChooser createDirectoryChooser(
            String title
    ) {

        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                title
        );

        chooser.setFileSelectionMode(
                JFileChooser.DIRECTORIES_ONLY
        );

        chooser.setAcceptAllFileFilterUsed(
                false
        );

        return chooser;
    }

    /*
     * Início da execução.
     */
    private void startProcessing() {

        if (!hasAllSelections()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Selecione a pasta dos PDFs, a planilha Excel, " +
                            "o ZIP dos XMLs e a pasta de saída.",
                    "Campos obrigatórios",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        setProcessingState(
                true
        );

        resetExecutionView();

        setStatus(
                "Processamento em andamento",
                "Preparando os arquivos para análise.",
                "PROCESSANDO",
                AppTheme.PRIMARY
        );

        SwingWorker<RunResult, String> worker =
                new SwingWorker<RunResult, String>() {

                    @Override
                    protected RunResult doInBackground()
                            throws Exception {

                        AppRunner runner =
                                new AppRunner();

                        return runner.run(
                                pdfFolder,
                                outputFolder,
                                spreadsheet,
                                xmlZip,
                                new ProcessingListener() {

                                    @Override
                                    public void onStatus(
                                            String message
                                    ) {

                                        publish(
                                                message
                                        );
                                    }

                                    @Override
                                    public void onProgress(
                                            int current,
                                            int total
                                    ) {

                                        int progress =
                                                total == 0
                                                        ? 0
                                                        : (int) Math.round(
                                                        (
                                                                current
                                                                        * 100.0
                                                        )
                                                                / total
                                                );

                                        setProgress(
                                                progress
                                        );
                                    }
                                }
                        );
                    }

                    @Override
                    protected void process(
                            List<String> messages
                    ) {

                        for (
                                String message :
                                messages
                        ) {

                            appendStatus(
                                    message
                            );

                            updateCurrentStatus(
                                    message
                            );
                        }
                    }

                    @Override
                    protected void done() {

                        setProcessingState(
                                false
                        );

                        try {

                            RunResult result =
                                    get();

                            progressBar.setValue(
                                    100
                            );

                            filesCountValue.setText(
                                    String.valueOf(
                                            result.getArquivosLidos()
                                    )
                            );

                            matchesCountValue.setText(
                                    String.valueOf(
                                            result.getContasEncontradas()
                                    )
                            );

                            openOutputButton.setEnabled(
                                    true
                            );

                            openSpreadsheetButton.setEnabled(
                                    true
                            );

                            setStatus(
                                    "Verificação concluída",
                                    "A planilha foi consultada sem alterações e os arquivos foram organizados.",
                                    "CONCLUÍDO",
                                    AppTheme.SUCCESS
                            );

                            JOptionPane.showMessageDialog(
                                    MainWindow.this,
                                    "Processamento concluído.\n\n"
                                            + "PDFs analisados: "
                                            + result.getArquivosLidos()
                                            + "\n"
                                            + "Contas encontradas: "
                                            + result.getContasEncontradas(),
                                    "Verificação concluída",
                                    JOptionPane.INFORMATION_MESSAGE
                            );

                        } catch (
                                InterruptedException exception
                        ) {

                            Thread
                                    .currentThread()
                                    .interrupt();

                            showProcessingError(
                                    "O processamento foi interrompido."
                            );

                        } catch (
                                ExecutionException exception
                        ) {

                            Throwable cause =
                                    exception.getCause();

                            String message =
                                    cause == null
                                            ? exception.getMessage()
                                            : cause.getMessage();

                            showProcessingError(
                                    message
                            );
                        }
                    }
                };

        worker.addPropertyChangeListener(
                event -> {

                    if (
                            "progress".equals(
                                    event.getPropertyName()
                            )
                    ) {

                        progressBar.setValue(
                                (Integer)
                                        event.getNewValue()
                        );
                    }
                }
        );

        worker.execute();
    }

    private void resetExecutionView() {

        progressBar.setValue(
                0
        );

        filesCountValue.setText(
                "0"
        );

        matchesCountValue.setText(
                "0"
        );

        statusArea.setText(
                ""
        );

        openOutputButton.setEnabled(
                false
        );

        openSpreadsheetButton.setEnabled(
                false
        );
    }

    private void updateCurrentStatus(
            String message
    ) {

        if (
                message == null
                        || message.isBlank()
        ) {
            return;
        }

        if (
                message.startsWith(
                        "Lendo e indexando"
                )
        ) {

            statusTitle.setText(
                    "Lendo XMLs"
            );

            statusDetail.setText(
                    message
            );

        } else if (
                message.startsWith(
                        "XMLs encontrados"
                )
        ) {

            statusTitle.setText(
                    "XMLs preparados"
            );

            statusDetail.setText(
                    message
            );

        } else if (
                message.startsWith(
                        "Processando"
                )
        ) {

            statusTitle.setText(
                    "Analisando PDFs"
            );

            statusDetail.setText(
                    message
            );

        } else if (
                message.startsWith(
                        "Chaves de acesso"
                )
        ) {

            statusDetail.setText(
                    message
            );

        } else if (
                message.startsWith(
                        "Organizando"
                )
        ) {

            statusTitle.setText(
                    "Finalizando a verificação"
            );

            statusDetail.setText(
                    message
            );
        }
    }

    private void setStatus(
            String title,
            String detail,
            String badgeText,
            Color badgeColor
    ) {

        statusTitle.setText(
                title
        );

        statusDetail.setText(
                detail
        );

        statusBadge.setText(
                badgeText
        );

        statusBadge.setForeground(
                badgeColor
        );

        statusBadge.setBorder(
                BorderFactory.createCompoundBorder(
                        new RoundedBorder(
                                new Color(
                                        badgeColor.getRed(),
                                        badgeColor.getGreen(),
                                        badgeColor.getBlue(),
                                        90
                                ),
                                16,
                                1
                        ),
                        BorderFactory.createEmptyBorder(
                                3,
                                10,
                                3,
                                10
                        )
                )
        );
    }

    private void showProcessingError(
            String message
    ) {

        String safeMessage =
                message == null
                        || message.isBlank()
                        ? "Ocorreu um erro durante o processamento."
                        : message;

        appendStatus(
                "ERRO: "
                        + safeMessage
        );

        setStatus(
                "Não foi possível concluir",
                safeMessage,
                "ERRO",
                AppTheme.ERROR
        );

        JOptionPane.showMessageDialog(
                this,
                safeMessage,
                "Erro no processamento",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void setProcessingState(
            boolean processing
    ) {

        selectPdfFolderButton.setEnabled(
                !processing
        );

        selectSpreadsheetButton.setEnabled(
                !processing
        );

        selectXmlZipButton.setEnabled(
                !processing
        );

        selectOutputFolderButton.setEnabled(
                !processing
        );

        startButton.setEnabled(
                !processing
                        && hasAllSelections()
        );
    }

    private void updateStartButtonState() {

        startButton.setEnabled(
                hasAllSelections()
        );
    }

    /*
     * Agora são quatro seleções obrigatórias.
     */
    private boolean hasAllSelections() {

        return pdfFolder != null
                && spreadsheet != null
                && xmlZip != null
                && outputFolder != null;
    }

    private void appendStatus(
            String message
    ) {

        if (
                message == null
                        || message.isBlank()
        ) {
            return;
        }

        if (
                !statusArea
                        .getText()
                        .isBlank()
        ) {

            statusArea.append(
                    System.lineSeparator()
            );
        }

        statusArea.append(
                message
        );

        statusArea.setCaretPosition(
                statusArea
                        .getDocument()
                        .getLength()
        );
    }

    private void toggleDetails() {

        boolean visible =
                !detailsContainer.isVisible();

        detailsContainer.setVisible(
                visible
        );

        toggleDetailsButton.setText(
                visible
                        ? "Ocultar detalhes"
                        : "Ver detalhes"
        );

        detailsContainer
                .getParent()
                .revalidate();

        detailsContainer
                .getParent()
                .repaint();
    }

    private void openPath(
            Path path
    ) {

        if (
                path == null
                        || !Desktop.isDesktopSupported()
        ) {
            return;
        }

        try {

            if (
                    Files.exists(
                            path
                    )
            ) {

                Desktop
                        .getDesktop()
                        .open(
                                path.toFile()
                        );
            }

        } catch (
                Exception exception
        ) {

            JOptionPane.showMessageDialog(
                    this,
                    "Não foi possível abrir o caminho selecionado.",
                    "Erro",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static JTextField createPathField() {

        JTextField field =
                new JTextField();

        field.setEditable(
                false
        );

        field.setBackground(
                Color.WHITE
        );

        field.setForeground(
                AppTheme.TEXT
        );

        field.setCaretColor(
                AppTheme.PRIMARY
        );

        field.setBorder(
                new RoundedBorder(
                        AppTheme.BORDER,
                        13,
                        1
                )
        );

        field.setPreferredSize(
                new Dimension(
                        320,
                        42
                )
        );

        return field;
    }

    private static JLabel createSummaryValue(
            String text
    ) {

        JLabel label =
                new JLabel(
                        text
                );

        label.setFont(
                AppTheme.FONT_NUMBER
        );

        label.setForeground(
                AppTheme.PRIMARY_DARK
        );

        return label;
    }

    private static JLabel createStatusBadge(
            String text,
            Color color
    ) {

        JLabel label =
                new JLabel(
                        text
                );

        label.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        11
                )
        );

        label.setForeground(
                color
        );

        label.setBorder(
                BorderFactory.createCompoundBorder(
                        new RoundedBorder(
                                AppTheme.BORDER,
                                16,
                                1
                        ),
                        BorderFactory.createEmptyBorder(
                                3,
                                10,
                                3,
                                10
                        )
                )
        );

        return label;
    }
}