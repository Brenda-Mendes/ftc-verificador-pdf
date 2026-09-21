package com.ftcverificador;

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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

public class MainWindow extends JFrame {

    private final JTextField pdfFolderField = createPathField();
    private final JTextField spreadsheetField = createPathField();
    private final JTextField xmlZipField = createPathField();
    private final JTextField outputFolderField = createPathField();

    private final RoundedButton selectPdfFolderButton =
            new RoundedButton("Selecionar", RoundedButton.Style.SECONDARY);

    private final RoundedButton selectSpreadsheetButton =
            new RoundedButton("Selecionar", RoundedButton.Style.SECONDARY);

    private final RoundedButton selectXmlZipButton =
            new RoundedButton("Selecionar", RoundedButton.Style.SECONDARY);

    private final RoundedButton selectOutputFolderButton =
            new RoundedButton("Selecionar", RoundedButton.Style.SECONDARY);

    private final RoundedButton processButton =
            new RoundedButton("Processar", RoundedButton.Style.PRIMARY);

    private final RoundedButton openReportButton =
            new RoundedButton("Abrir relatório", RoundedButton.Style.SECONDARY);

    private final RoundedButton openOutputButton =
            new RoundedButton("Abrir pasta final", RoundedButton.Style.SECONDARY);

    private final RoundedButton toggleDetailsButton =
            new RoundedButton("Ver detalhes", RoundedButton.Style.GHOST);

    private final JProgressBar progressBar = new JProgressBar(0, 100);

    private final JLabel statusTitle = new JLabel("Aguardando arquivos");
    private final JLabel statusDetail = new JLabel(
            "Selecione os quatro caminhos para iniciar."
    );

    private final JLabel statusBadge = createStatusBadge(
            "PRONTO",
            AppTheme.TEXT_SECONDARY
    );

    private final JLabel filesCountValue = createSummaryValue("0");
    private final JLabel danfesCountValue = createSummaryValue("0");
    private final JLabel matchesCountValue = createSummaryValue("0");
    private final JLabel notFoundCountValue = createSummaryValue("0");

    private final JTextArea statusArea = new JTextArea();
    private final JScrollPane detailsScrollPane = new JScrollPane(statusArea);
    private final JPanel detailsContainer = new JPanel(new BorderLayout());

    private Path pdfFolder;
    private Path spreadsheet;
    private Path xmlZip;
    private Path outputBaseFolder;

    private Path lastProcessingFolder;
    private Path lastReportPath;

    public MainWindow() {
        super("FTC Verificador PDF");

        setIconImages(AppIcon.loadImages());
        configureWindow();
        configureActions();
        configureInitialState();
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 760));
        setSize(1100, 860);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppTheme.BACKGROUND);
        setContentPane(root);

        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createBody(), BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D graphics2D = (Graphics2D) graphics.create();

                graphics2D.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                GradientPaint gradient = new GradientPaint(
                        0,
                        0,
                        AppTheme.PRIMARY_DARK,
                        getWidth(),
                        getHeight(),
                        AppTheme.PRIMARY
                );

                graphics2D.setPaint(gradient);
                graphics2D.fillRect(0, 0, getWidth(), getHeight());
                graphics2D.dispose();
            }
        };

        header.setOpaque(false);
        header.setBorder(new EmptyBorder(22, 34, 22, 34));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("FTC Verificador PDF");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(
                "Organize PDFs e XMLs por cenário e gere o relatório da execução."
        );
        subtitle.setFont(AppTheme.FONT_SUBTITLE);
        subtitle.setForeground(new Color(247, 226, 231));

        texts.add(title);
        texts.add(Box.createVerticalStrut(5));
        texts.add(subtitle);

        JLabel mark = new JLabel("FTC");
        mark.setHorizontalAlignment(SwingConstants.CENTER);
        mark.setVerticalAlignment(SwingConstants.CENTER);
        mark.setFont(new Font("Segoe UI", Font.BOLD, 18));
        mark.setForeground(AppTheme.PRIMARY_DARK);
        mark.setOpaque(true);
        mark.setBackground(Color.WHITE);
        mark.setPreferredSize(new Dimension(64, 46));
        mark.setBorder(
                new RoundedBorder(
                        new Color(255, 255, 255, 100),
                        14,
                        1
                )
        );

        header.add(texts, BorderLayout.CENTER);
        header.add(mark, BorderLayout.EAST);

        return header;
    }

    private Component createBody() {
        JPanel content = new JPanel();
        content.setBackground(AppTheme.BACKGROUND);
        content.setLayout(
                new BoxLayout(
                        content,
                        BoxLayout.Y_AXIS
                )
        );
        content.setBorder(
                new EmptyBorder(
                        22,
                        30,
                        28,
                        30
                )
        );

        content.add(createInputsCard());
        content.add(Box.createVerticalStrut(16));
        content.add(createProcessingCard());
        content.add(Box.createVerticalStrut(16));
        content.add(createSummaryCard());
        content.add(Box.createVerticalStrut(16));
        content.add(createActionBar());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(
                BorderFactory.createEmptyBorder()
        );
        scrollPane.getVerticalScrollBar()
                .setUnitIncrement(16);
        scrollPane.getViewport()
                .setBackground(AppTheme.BACKGROUND);

        return scrollPane;
    }

    private JPanel createInputsCard() {
        RoundedPanel card = new RoundedPanel(
                20,
                AppTheme.SURFACE
        );

        card.setLayout(
                new BorderLayout(0, 14)
        );

        card.setBorder(
                AppTheme.cardBorder()
        );

        card.add(
                createSectionHeading(
                        "Arquivos e saída",
                        "A pasta final será criada automaticamente com a data da execução."
                ),
                BorderLayout.NORTH
        );

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(
                new BoxLayout(
                        rows,
                        BoxLayout.Y_AXIS
                )
        );

        rows.add(
                createSelectorRow(
                        "1",
                        "Pasta dos PDFs",
                        "A leitura inclui subpastas.",
                        pdfFolderField,
                        selectPdfFolderButton
                )
        );

        rows.add(Box.createVerticalStrut(8));

        rows.add(
                createSelectorRow(
                        "2",
                        "Planilha de cenários",
                        "Usada apenas como base de consulta desta execução.",
                        spreadsheetField,
                        selectSpreadsheetButton
                )
        );

        rows.add(Box.createVerticalStrut(8));

        rows.add(
                createSelectorRow(
                        "3",
                        "ZIP dos XMLs",
                        "Os XMLs são relacionados pelas chaves NFCom.",
                        xmlZipField,
                        selectXmlZipButton
                )
        );

        rows.add(Box.createVerticalStrut(8));

        rows.add(
                createSelectorRow(
                        "4",
                        "Pasta base de saída",
                        "Selecione onde a pasta do processamento será criada.",
                        outputFolderField,
                        selectOutputFolderButton
                )
        );

        card.add(
                rows,
                BorderLayout.CENTER
        );

        return card;
    }

    private JPanel createProcessingCard() {
        RoundedPanel card = new RoundedPanel(
                20,
                AppTheme.SURFACE
        );

        card.setLayout(
                new BorderLayout(0, 14)
        );

        card.setBorder(
                AppTheme.cardBorder()
        );

        JPanel top = new JPanel(
                new BorderLayout(10, 0)
        );

        top.setOpaque(false);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(
                new BoxLayout(
                        texts,
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

        texts.add(statusTitle);
        texts.add(Box.createVerticalStrut(3));
        texts.add(statusDetail);

        top.add(
                texts,
                BorderLayout.CENTER
        );

        top.add(
                statusBadge,
                BorderLayout.EAST
        );

        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setPreferredSize(
                new Dimension(100, 22)
        );

        detailsContainer.setOpaque(false);
        detailsContainer.add(
                detailsScrollPane,
                BorderLayout.CENTER
        );
        detailsContainer.setVisible(false);

        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setRows(8);
        statusArea.setBackground(
                AppTheme.SURFACE_SOFT
        );
        statusArea.setForeground(
                AppTheme.TEXT
        );
        statusArea.setBorder(
                new EmptyBorder(
                        10,
                        10,
                        10,
                        10
                )
        );

        detailsScrollPane.setBorder(
                BorderFactory.createLineBorder(
                        AppTheme.BORDER
                )
        );

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(
                new BoxLayout(
                        center,
                        BoxLayout.Y_AXIS
                )
        );

        center.add(progressBar);
        center.add(Box.createVerticalStrut(8));
        center.add(toggleDetailsButton);
        center.add(Box.createVerticalStrut(8));
        center.add(detailsContainer);

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

    private JPanel createSummaryCard() {
        RoundedPanel card = new RoundedPanel(
                20,
                AppTheme.SURFACE
        );

        card.setLayout(
                new BorderLayout(0, 14)
        );

        card.setBorder(
                AppTheme.cardBorder()
        );

        card.add(
                createSectionHeading(
                        "Resumo",
                        "Os números abaixo correspondem apenas à execução atual."
                ),
                BorderLayout.NORTH
        );

        JPanel cards = new JPanel(
                new GridBagLayout()
        );

        cards.setOpaque(false);

        GridBagConstraints c =
                new GridBagConstraints();

        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.insets =
                new Insets(0, 0, 0, 6);

        cards.add(
                createMetricCard(
                        "PDFs",
                        filesCountValue
                ),
                c
        );

        c.gridx = 1;
        c.insets =
                new Insets(0, 6, 0, 6);

        cards.add(
                createMetricCard(
                        "DANFEs",
                        danfesCountValue
                ),
                c
        );

        c.gridx = 2;

        cards.add(
                createMetricCard(
                        "Contas encontradas",
                        matchesCountValue
                ),
                c
        );

        c.gridx = 3;
        c.insets =
                new Insets(0, 6, 0, 0);

        cards.add(
                createMetricCard(
                        "Não encontradas",
                        notFoundCountValue
                ),
                c
        );

        card.add(
                cards,
                BorderLayout.CENTER
        );

        return card;
    }

    private JPanel createMetricCard(
            String title,
            JLabel value
    ) {
        RoundedPanel panel = new RoundedPanel(
                16,
                AppTheme.SURFACE_SOFT
        );

        panel.setLayout(
                new BorderLayout(10, 0)
        );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        14,
                        16,
                        14,
                        16
                )
        );

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setFont(
                AppTheme.FONT_MEDIUM
        );

        titleLabel.setForeground(
                AppTheme.TEXT
        );

        panel.add(
                titleLabel,
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
                new JPanel(new BorderLayout());

        panel.setOpaque(false);

        JPanel left =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                8,
                                0
                        )
                );

        left.setOpaque(false);
        left.add(openReportButton);
        left.add(openOutputButton);

        JPanel right =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                8,
                                0
                        )
                );

        right.setOpaque(false);
        right.add(processButton);

        panel.add(
                left,
                BorderLayout.WEST
        );

        panel.add(
                right,
                BorderLayout.EAST
        );

        return panel;
    }

    private JPanel createSectionHeading(
            String title,
            String subtitle
    ) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel titleLabel =
                new JLabel(title);

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
                new JLabel(subtitle);

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

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(3));
        panel.add(subtitleLabel);

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

        GridBagConstraints c =
                new GridBagConstraints();

        c.anchor = GridBagConstraints.WEST;

        JLabel stepLabel =
                new JLabel(
                        step,
                        SwingConstants.CENTER
                );

        stepLabel.setOpaque(true);
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
                new Dimension(34, 34)
        );
        stepLabel.setBorder(
                new RoundedBorder(
                        AppTheme.PRIMARY,
                        17,
                        1
                )
        );

        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 2;
        c.weightx = 0;
        c.insets =
                new Insets(0, 0, 0, 12);
        row.add(stepLabel, c);

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setFont(
                AppTheme.FONT_MEDIUM
        );
        titleLabel.setForeground(
                AppTheme.TEXT
        );

        c.gridx = 1;
        c.gridy = 0;
        c.gridheight = 1;
        c.insets =
                new Insets(0, 0, 2, 14);
        row.add(titleLabel, c);

        JLabel descriptionLabel =
                new JLabel(description);

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

        c.gridx = 1;
        c.gridy = 1;
        c.insets =
                new Insets(0, 0, 0, 14);
        row.add(descriptionLabel, c);

        c.gridx = 2;
        c.gridy = 0;
        c.gridheight = 2;
        c.weightx = 1;
        c.fill =
                GridBagConstraints.HORIZONTAL;
        c.insets =
                new Insets(0, 0, 0, 10);
        row.add(field, c);

        c.gridx = 3;
        c.weightx = 0;
        c.fill =
                GridBagConstraints.NONE;
        c.insets =
                new Insets(0, 0, 0, 0);
        row.add(button, c);

        return row;
    }

    private void configureActions() {
        selectPdfFolderButton.addActionListener(
                event -> selectPdfFolder()
        );

        selectSpreadsheetButton.addActionListener(
                event -> selectSpreadsheet()
        );

        selectXmlZipButton.addActionListener(
                event -> selectXmlZip()
        );

        selectOutputFolderButton.addActionListener(
                event -> selectOutputFolder()
        );

        processButton.addActionListener(
                event -> startProcessing()
        );

        openReportButton.addActionListener(
                event -> openPath(lastReportPath)
        );

        openOutputButton.addActionListener(
                event -> openPath(lastProcessingFolder)
        );

        toggleDetailsButton.addActionListener(
                event -> toggleDetails()
        );
    }

    private void configureInitialState() {
        openReportButton.setEnabled(false);
        openOutputButton.setEnabled(false);
        updateActionButtonsState();
    }

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
                    chooser.getSelectedFile()
                            .toPath()
                            .toAbsolutePath()
                            .normalize();

            pdfFolderField.setText(
                    pdfFolder.toString()
            );

            if (outputBaseFolder == null) {
                Path parent =
                        pdfFolder.getParent();

                outputBaseFolder =
                        parent != null
                                ? parent
                                : pdfFolder;

                outputFolderField.setText(
                        outputBaseFolder.toString()
                );
            }

            updateActionButtonsState();
        }
    }

    private void selectSpreadsheet() {
        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "Selecione a planilha Excel de cenários"
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
                    chooser.getSelectedFile()
                            .toPath()
                            .toAbsolutePath()
                            .normalize();

            spreadsheetField.setText(
                    spreadsheet.toString()
            );

            updateActionButtonsState();
        }
    }

    private void selectXmlZip() {
        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "Selecione o ZIP com os XMLs"
        );

        chooser.setFileSelectionMode(
                JFileChooser.FILES_ONLY
        );

        chooser.setAcceptAllFileFilterUsed(
                false
        );

        chooser.setFileFilter(
                new FileNameExtensionFilter(
                        "Arquivo ZIP (*.zip)",
                        "zip"
                )
        );

        if (
                chooser.showOpenDialog(this)
                        == JFileChooser.APPROVE_OPTION
        ) {
            xmlZip =
                    chooser.getSelectedFile()
                            .toPath()
                            .toAbsolutePath()
                            .normalize();

            xmlZipField.setText(
                    xmlZip.toString()
            );

            updateActionButtonsState();
        }
    }

    private void selectOutputFolder() {
        JFileChooser chooser =
                createDirectoryChooser(
                        "Selecione a pasta base de saída"
                );

        if (
                chooser.showOpenDialog(this)
                        == JFileChooser.APPROVE_OPTION
        ) {
            outputBaseFolder =
                    chooser.getSelectedFile()
                            .toPath()
                            .toAbsolutePath()
                            .normalize();

            outputFolderField.setText(
                    outputBaseFolder.toString()
            );

            updateActionButtonsState();
        }
    }

    private JFileChooser createDirectoryChooser(
            String title
    ) {
        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(
                JFileChooser.DIRECTORIES_ONLY
        );
        chooser.setAcceptAllFileFilterUsed(
                false
        );

        return chooser;
    }

    private void startProcessing() {
        if (!hasAllSelections()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Selecione a pasta dos PDFs, a planilha, "
                            + "o ZIP dos XMLs e a pasta base de saída.",
                    "Campos obrigatórios",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        setProcessingState(true);
        resetExecutionView();

        setStatus(
                "Processamento em andamento",
                "Preparando os arquivos para análise.",
                "PROCESSANDO",
                AppTheme.PRIMARY
        );

        SwingWorker<RunResult, String> worker =
                new SwingWorker<>() {

                    @Override
                    protected RunResult doInBackground()
                            throws Exception {

                        AppRunner runner =
                                new AppRunner();

                        return runner.run(
                                pdfFolder,
                                outputBaseFolder,
                                spreadsheet,
                                xmlZip,
                                new ProcessingListener() {
                                    @Override
                                    public void onStatus(
                                            String message
                                    ) {
                                        publish(message);
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
                                                                (current * 100.0)
                                                                        / total
                                                        );

                                        setProgress(progress);
                                    }
                                }
                        );
                    }

                    @Override
                    protected void process(
                            List<String> messages
                    ) {
                        for (String message : messages) {
                            appendStatus(message);
                            updateCurrentStatus(message);
                        }
                    }

                    @Override
                    protected void done() {
                        setProcessingState(false);

                        try {
                            RunResult result = get();

                            progressBar.setValue(100);

                            filesCountValue.setText(
                                    String.valueOf(
                                            result.getArquivosLidos()
                                    )
                            );

                            danfesCountValue.setText(
                                    String.valueOf(
                                            result.getDanfesEncontradas()
                                    )
                            );

                            matchesCountValue.setText(
                                    String.valueOf(
                                            result.getContasEncontradas()
                                    )
                            );

                            notFoundCountValue.setText(
                                    String.valueOf(
                                            result.getContasNaoEncontradas()
                                    )
                            );

                            lastProcessingFolder =
                                    result.getOutputDir();

                            lastReportPath =
                                    result.getReportPath();

                            openOutputButton.setEnabled(
                                    Files.isDirectory(
                                            lastProcessingFolder
                                    )
                            );

                            openReportButton.setEnabled(
                                    Files.isRegularFile(
                                            lastReportPath
                                    )
                            );

                            setStatus(
                                    "Processamento concluído",
                                    "Contas encontradas: "
                                            + result.getContasEncontradas()
                                            + " | Não encontradas: "
                                            + result.getContasNaoEncontradas()
                                            + ".",
                                    "CONCLUÍDO",
                                    AppTheme.SUCCESS
                            );

                            appendStatus(
                                    "Total de contas únicas na planilha: "
                                            + result.getTotalContasPlanilha()
                            );

                            appendStatus(
                                    "Pasta final: "
                                            + lastProcessingFolder
                            );

                            appendStatus(
                                    "Relatório: "
                                            + lastReportPath
                            );

                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            showProcessingError(exception);

                        } catch (ExecutionException exception) {
                            Throwable cause =
                                    exception.getCause() == null
                                            ? exception
                                            : exception.getCause();

                            showProcessingError(cause);
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
                                (Integer) event.getNewValue()
                        );
                    }
                }
        );

        worker.execute();
    }

    private void showProcessingError(
            Throwable throwable
    ) {
        String message =
                throwable.getMessage();

        if (message == null || message.isBlank()) {
            message =
                    throwable.getClass()
                            .getSimpleName();
        }

        appendStatus(
                "ERRO: " + message
        );

        setStatus(
                "Falha no processamento",
                message,
                "ERRO",
                AppTheme.ERROR
        );

        JOptionPane.showMessageDialog(
                this,
                message,
                "Erro",
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

        if (processing) {
            processButton.setEnabled(false);
            openReportButton.setEnabled(false);
            openOutputButton.setEnabled(false);
        } else {
            updateActionButtonsState();

            openReportButton.setEnabled(
                    lastReportPath != null
                            && Files.isRegularFile(
                                    lastReportPath
                            )
            );

            openOutputButton.setEnabled(
                    lastProcessingFolder != null
                            && Files.isDirectory(
                                    lastProcessingFolder
                            )
            );
        }
    }

    private void updateActionButtonsState() {
        processButton.setEnabled(
                hasAllSelections()
        );
    }

    private boolean hasAllSelections() {
        return pdfFolder != null
                && spreadsheet != null
                && xmlZip != null
                && outputBaseFolder != null;
    }

    private void resetExecutionView() {
        progressBar.setValue(0);
        filesCountValue.setText("0");
        danfesCountValue.setText("0");
        matchesCountValue.setText("0");
        notFoundCountValue.setText("0");
        statusArea.setText("");
        lastProcessingFolder = null;
        lastReportPath = null;
    }

    private void updateCurrentStatus(
            String message
    ) {
        statusDetail.setText(message);
    }

    private void appendStatus(
            String message
    ) {
        if (!statusArea.getText().isEmpty()) {
            statusArea.append(
                    System.lineSeparator()
            );
        }

        statusArea.append(message);

        statusArea.setCaretPosition(
                statusArea.getDocument()
                        .getLength()
        );
    }

    private void setStatus(
            String title,
            String detail,
            String badge,
            Color badgeColor
    ) {
        statusTitle.setText(title);
        statusDetail.setText(detail);
        statusBadge.setText(badge);
        statusBadge.setForeground(badgeColor);
    }

    private void toggleDetails() {
        boolean visible =
                !detailsContainer.isVisible();

        detailsContainer.setVisible(visible);

        toggleDetailsButton.setText(
                visible
                        ? "Ocultar detalhes"
                        : "Ver detalhes"
        );

        revalidate();
        repaint();
    }

    private void openPath(
            Path path
    ) {
        if (path == null || !Files.exists(path)) {
            JOptionPane.showMessageDialog(
                    this,
                    "O caminho ainda não está disponível.",
                    "Arquivo não encontrado",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(
                    this,
                    "O sistema não suporta abertura automática deste caminho.",
                    "Abertura indisponível",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            Desktop.getDesktop()
                    .open(path.toFile());

        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Não foi possível abrir: "
                            + path,
                    "Erro",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static JTextField createPathField() {
        JTextField field =
                new JTextField();

        field.setEditable(false);
        field.setBackground(Color.WHITE);
        field.setForeground(AppTheme.TEXT);
        field.setPreferredSize(
                new Dimension(380, 34)
        );

        return field;
    }

    private static JLabel createSummaryValue(
            String value
    ) {
        JLabel label =
                new JLabel(value);

        label.setFont(
                AppTheme.FONT_NUMBER
        );

        label.setForeground(
                AppTheme.PRIMARY
        );

        return label;
    }

    private static JLabel createStatusBadge(
            String text,
            Color color
    ) {
        JLabel label =
                new JLabel(
                        text,
                        SwingConstants.CENTER
                );

        label.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        label.setForeground(color);

        label.setBorder(
                BorderFactory.createCompoundBorder(
                        new RoundedBorder(
                                AppTheme.BORDER,
                                12,
                                1
                        ),
                        new EmptyBorder(
                                6,
                                10,
                                6,
                                10
                        )
                )
        );

        return label;
    }
}
