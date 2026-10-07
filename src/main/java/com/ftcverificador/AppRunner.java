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
            Path pdfInput,
            Path outputBaseDir,
            Path xlsxPath,
            Path xmlZipPath,
            ProcessingListener listener
    ) throws IOException {

        validateInputs(
                pdfInput,
                outputBaseDir,
                xlsxPath,
                xmlZipPath
        );

        ProcessingListener safeListener = listener == null
                ? new SilentProcessingListener()
                : listener;

        Path processingDir =
                resolveProcessingDirectory(outputBaseDir);

        Path normalizedProcessingDir =
                processingDir.toAbsolutePath().normalize();

        /*
         * Esta validação só se aplica quando a entrada é uma pasta.
         * Para ZIP, os PDFs serão extraídos para uma pasta temporária.
         */
        if (Files.isDirectory(pdfInput)) {

            Path normalizedPdfInput =
                    pdfInput.toAbsolutePath().normalize();

            if (normalizedProcessingDir.startsWith(normalizedPdfInput)) {
                throw new IllegalArgumentException(
                        "A pasta final não pode ficar dentro da pasta de PDFs, "
                                + "pois ela seria lida novamente em uma próxima execução."
                );
            }
        }

        safeListener.onStatus("Novo processamento iniciado.");
        safeListener.onStatus("Pasta final: " + processingDir);

        boolean zipPdfInput =
                Files.isRegularFile(pdfInput)
                        && PdfService.isZipFile(pdfInput);

        RunResult result;

        try (
                PdfInputService pdfInputService =
                        PdfInputService.open(pdfInput)
        ) {

            Path pdfRoot =
                    pdfInputService.getPdfRoot();

            if (pdfInputService.isTemporary()) {
                safeListener.onStatus(
                        "ZIP de PDFs detectado. "
                                + "Conteúdo extraído temporariamente para processamento."
                );
            } else {
                safeListener.onStatus(
                        "Pasta de PDFs selecionada: "
                                + pdfRoot
                );
            }

            XmlZipService xmlZipService = null;

            if (xmlZipPath != null) {
                xmlZipService =
                        new XmlZipService(xmlZipPath);

                safeListener.onStatus(
                        "XMLs encontrados no ZIP: "
                                + xmlZipService.getXmlCount()
                                + " | Chaves indexadas: "
                                + xmlZipService.getIndexedKeyCount()
                );
            } else {
                safeListener.onStatus(
                        "Nenhum ZIP de XMLs selecionado. "
                                + "Os PDFs serão separados normalmente, sem copiar XMLs."
                );
            }

            SpreadsheetService spreadsheetService =
                    new SpreadsheetService(
                            xlsxPath,
                            processingDir
                    );

            PdfService pdfService =
                    new PdfService();

            List<Path> pdfFiles =
                    pdfService.listarArquivosPDF(pdfRoot);

            int contasEncontradas = 0;
            int danfesEncontradas = 0;

            safeListener.onStatus(
                    "Foram encontrados "
                            + pdfFiles.size()
                            + " arquivo(s) PDF."
            );

            for (
                    int index = 0;
                    index < pdfFiles.size();
                    index++
            ) {
                Path pdfPath =
                        pdfFiles.get(index);

                String nomeArquivo =
                        pdfRoot.relativize(pdfPath).toString();

                safeListener.onStatus(
                        "Processando "
                                + (index + 1)
                                + " de "
                                + pdfFiles.size()
                                + ": "
                                + nomeArquivo
                );

                PdfContaDados dadosConta =
                        pdfService.extrairDadosConta(pdfPath);

                int danfesNoPdf =
                        dadosConta.getChavesAcesso().size();

                danfesEncontradas += danfesNoPdf;

                contasEncontradas +=
                        spreadsheetService.markMatches(
                                dadosConta,
                                pdfPath
                        );

                safeListener.onProgress(
                        index + 1,
                        pdfFiles.size()
                );
            }

            safeListener.onStatus(
                    xmlZipService == null
                            ? "Organizando PDFs e gerando o relatório..."
                            : "Organizando PDFs e XMLs e gerando o relatório..."
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
                            + " | Contas sem cenário: "
                            + spreadsheetService.getNoScenarioAccountsCount()
            );

            safeListener.onStatus(
                    "Relatório principal: "
                            + spreadsheetService.getReportPath()
            );


            result = new RunResult(
                    pdfFiles.size(),
                    spreadsheetService.getFoundAccountsCount(),
                    spreadsheetService.getNotFoundAccountsCount(),
                    spreadsheetService.getNoScenarioAccountsCount(),
                    spreadsheetService.getTotalExpectedAccountsCount(),
                    danfesEncontradas,
                    processingDir,
                    spreadsheetService.getReportPath()
            );
        }

        if (zipPdfInput) {
            safeListener.onStatus(
                    "Arquivos temporários do ZIP de PDFs removidos."
            );
        }

        return result;
    }

    private Path resolveProcessingDirectory(
            Path outputBaseDir
    ) throws IOException {

        Path base =
                outputBaseDir.toAbsolutePath().normalize();

        Files.createDirectories(base);

        String baseFolderName =
                PROCESSING_FOLDER_PREFIX
                        + LocalDate.now()
                                .format(FOLDER_DATE_FORMAT);

        Path processingDir =
                base.resolve(baseFolderName);

        if (!Files.exists(processingDir)) {
            Files.createDirectories(processingDir);
            return processingDir;
        }

        int suffix = 2;

        while (true) {
            Path candidate =
                    base.resolve(
                            baseFolderName
                                    + "_"
                                    + suffix
                    );

            if (!Files.exists(candidate)) {
                Files.createDirectories(candidate);
                return candidate;
            }

            suffix++;
        }
    }

    private void validateInputs(
            Path pdfInput,
            Path outputBaseDir,
            Path xlsxPath,
            Path xmlZipPath
    ) throws IOException {

        boolean validPdfFolder =
                pdfInput != null
                        && Files.isDirectory(pdfInput);

        boolean validPdfZip =
                pdfInput != null
                        && Files.isRegularFile(pdfInput)
                        && PdfService.isZipFile(pdfInput);

        if (!validPdfFolder && !validPdfZip) {
            throw new IllegalArgumentException(
                    "Selecione uma pasta de PDFs ou um arquivo ZIP válido."
            );
        }

        if (xlsxPath == null
                || !Files.isRegularFile(xlsxPath)) {

            throw new IllegalArgumentException(
                    "Selecione uma planilha Excel válida."
            );
        }

        if (!PdfService.isXlsxFile(xlsxPath)) {
            throw new IllegalArgumentException(
                    "A planilha selecionada deve possuir extensão .xlsx."
            );
        }

        /*
         * O ZIP de XMLs é opcional.
         *
         * Sem XML:
         * - os PDFs continuam sendo separados normalmente por cenário;
         * - o relatório continua sendo gerado;
         * - a quantidade de XMLs no resumo por cenário ficará 0.
         *
         * Com XML:
         * - mantém o comportamento atual e copia os XMLs correspondentes.
         */
        if (xmlZipPath != null) {

            if (!Files.isRegularFile(xmlZipPath)) {
                throw new IllegalArgumentException(
                        "O ZIP de XMLs selecionado não é um arquivo válido."
                );
            }

            if (!XmlZipService.isZipFile(xmlZipPath)) {
                throw new IllegalArgumentException(
                        "O arquivo de XMLs deve possuir extensão .zip."
                );
            }
        }

        if (outputBaseDir == null) {
            throw new IllegalArgumentException(
                    "Selecione uma pasta base de saída."
            );
        }

        if (Files.exists(outputBaseDir)
                && !Files.isDirectory(outputBaseDir)) {

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
        public void onProgress(
                int current,
                int total
        ) {
            // Execução sem acompanhamento visual.
        }
    }
}
