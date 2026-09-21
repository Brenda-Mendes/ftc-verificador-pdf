package com.ftcverificador;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AppRunner {

    public static final String PROCESSING_FOLDER_PREFIX = "Processamento_NFCom_";

    private static final DateTimeFormatter FOLDER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);

    public RunResult run(
            Path pdfDir,
            Path outputBaseDir,
            Path xlsxPath,
            Path xmlZipPath,
            ProcessingListener listener
    ) throws IOException {

        validateInputs(
                pdfDir,
                outputBaseDir,
                xlsxPath,
                xmlZipPath
        );

        ProcessingListener safeListener = listener == null
                ? new SilentProcessingListener()
                : listener;

        Path processingDir = resolveProcessingDirectory(outputBaseDir);

        Path normalizedPdfDir = pdfDir.toAbsolutePath().normalize();
        Path normalizedProcessingDir = processingDir.toAbsolutePath().normalize();

        if (normalizedProcessingDir.startsWith(normalizedPdfDir)) {
            throw new IllegalArgumentException(
                    "A pasta final não pode ficar dentro da pasta de PDFs, "
                            + "pois ela seria lida novamente em uma próxima execução."
            );
        }

        safeListener.onStatus("Novo processamento iniciado.");
        safeListener.onStatus("Pasta final: " + processingDir);

        XmlZipService xmlZipService = new XmlZipService(xmlZipPath);

        safeListener.onStatus(
                "XMLs encontrados no ZIP: "
                        + xmlZipService.getXmlCount()
                        + " | Chaves indexadas: "
                        + xmlZipService.getIndexedKeyCount()
        );

        SpreadsheetService spreadsheetService =
                new SpreadsheetService(
                        xlsxPath,
                        processingDir
                );

        PdfService pdfService = new PdfService();
        List<Path> pdfFiles = pdfService.listarArquivosPDF(pdfDir);

        int contasEncontradas = 0;
        int danfesEncontradas = 0;

        safeListener.onStatus(
                "Foram encontrados "
                        + pdfFiles.size()
                        + " arquivo(s) PDF."
        );

        for (int index = 0; index < pdfFiles.size(); index++) {
            Path pdfPath = pdfFiles.get(index);
            String nomeArquivo = pdfDir.relativize(pdfPath).toString();

            safeListener.onStatus(
                    "Processando "
                            + (index + 1)
                            + " de "
                            + pdfFiles.size()
                            + ": "
                            + nomeArquivo
            );

            PdfContaDados dadosConta = pdfService.extrairDadosConta(pdfPath);

            int danfesNoPdf = dadosConta.getChavesAcesso().size();
            danfesEncontradas += danfesNoPdf;

            contasEncontradas += spreadsheetService.markMatches(
                    dadosConta,
                    pdfPath
            );

            safeListener.onProgress(index + 1, pdfFiles.size());
        }

        safeListener.onStatus(
                "Organizando PDFs e XMLs e gerando o relatório..."
        );

        spreadsheetService.writeResults(
                xmlZipService,
                pdfFiles.size(),
                contasEncontradas,
                danfesEncontradas
        );

        safeListener.onStatus(
                "Processamento concluído. Contas encontradas: "
                        + spreadsheetService.getFoundAccountsCount()
                        + " | Contas não encontradas: "
                        + spreadsheetService.getNotFoundAccountsCount()
        );

        safeListener.onStatus(
                "Relatório: " + spreadsheetService.getReportPath()
        );

        return new RunResult(
                pdfFiles.size(),
                spreadsheetService.getFoundAccountsCount(),
                spreadsheetService.getNotFoundAccountsCount(),
                spreadsheetService.getTotalExpectedAccountsCount(),
                danfesEncontradas,
                processingDir,
                spreadsheetService.getReportPath()
        );
    }

    private Path resolveProcessingDirectory(
            Path outputBaseDir
    ) throws IOException {

        Path base = outputBaseDir.toAbsolutePath().normalize();
        Files.createDirectories(base);

        String baseFolderName = PROCESSING_FOLDER_PREFIX
                + LocalDate.now().format(FOLDER_DATE_FORMAT);

        Path processingDir = base.resolve(baseFolderName);

        if (!Files.exists(processingDir)) {
            Files.createDirectories(processingDir);
            return processingDir;
        }

        int suffix = 2;

        while (true) {
            Path candidate = base.resolve(baseFolderName + "_" + suffix);

            if (!Files.exists(candidate)) {
                Files.createDirectories(candidate);
                return candidate;
            }

            suffix++;
        }
    }

    private void validateInputs(
            Path pdfDir,
            Path outputBaseDir,
            Path xlsxPath,
            Path xmlZipPath
    ) throws IOException {

        if (pdfDir == null || !Files.isDirectory(pdfDir)) {
            throw new IllegalArgumentException(
                    "Selecione uma pasta de PDFs válida."
            );
        }

        if (xlsxPath == null || !Files.isRegularFile(xlsxPath)) {
            throw new IllegalArgumentException(
                    "Selecione uma planilha Excel válida."
            );
        }

        if (!PdfService.isXlsxFile(xlsxPath)) {
            throw new IllegalArgumentException(
                    "A planilha selecionada deve possuir extensão .xlsx."
            );
        }

        if (xmlZipPath == null || !Files.isRegularFile(xmlZipPath)) {
            throw new IllegalArgumentException(
                    "Selecione um ZIP de XMLs válido."
            );
        }

        if (!XmlZipService.isZipFile(xmlZipPath)) {
            throw new IllegalArgumentException(
                    "O arquivo de XMLs deve possuir extensão .zip."
            );
        }

        if (outputBaseDir == null) {
            throw new IllegalArgumentException(
                    "Selecione uma pasta base de saída."
            );
        }

        if (Files.exists(outputBaseDir) && !Files.isDirectory(outputBaseDir)) {
            throw new IllegalArgumentException(
                    "O caminho de saída selecionado não é uma pasta."
            );
        }

        Files.createDirectories(outputBaseDir);
    }

    private static class SilentProcessingListener
            implements ProcessingListener {

        @Override
        public void onStatus(String message) {
            // Execução sem acompanhamento visual.
        }

        @Override
        public void onProgress(int current, int total) {
            // Execução sem acompanhamento visual.
        }
    }
}
