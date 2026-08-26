package com.ftcverificador;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AppRunner {

    public RunResult run(
            Path pdfDir,
            Path outputDir,
            Path xlsxPath,
            Path xmlZipPath,
            ProcessingListener listener
    ) throws IOException {

        validateInputs(
                pdfDir,
                outputDir,
                xlsxPath,
                xmlZipPath
        );

        ProcessingListener safeListener =
                listener == null
                        ? new SilentProcessingListener()
                        : listener;

        PdfService pdfService =
                new PdfService();

        /*
         * Lê e indexa todos os XMLs
         * existentes dentro do arquivo ZIP.
         */
        safeListener.onStatus(
                "Lendo e indexando os XMLs do arquivo ZIP..."
        );

        XmlZipService xmlZipService =
                new XmlZipService(
                        xmlZipPath
                );

        safeListener.onStatus(
                "XMLs encontrados no ZIP: "
                        + xmlZipService.getXmlCount()
                        + ". Chaves de acesso indexadas: "
                        + xmlZipService.getIndexedKeyCount()
                        + "."
        );

        /*
         * Carrega a planilha e cria o índice:
         *
         * conta -> cenário.
         */
        SpreadsheetService spreadsheetService =
                new SpreadsheetService(
                        xlsxPath,
                        outputDir
                );

        /*
         * Localiza todos os PDFs
         * da pasta e das subpastas.
         */
        List<Path> pdfFiles =
                pdfService.listarArquivosPDF(
                        pdfDir
                );

        int contasEncontradas = 0;
        int danfesEncontradas = 0;

        safeListener.onStatus(
                "Foram encontrados "
                        + pdfFiles.size()
                        + " arquivo(s) PDF."
        );

        /*
         * Analisa cada PDF.
         */
        for (
                int index = 0;
                index < pdfFiles.size();
                index++
        ) {

            Path pdfPath =
                    pdfFiles.get(
                            index
                    );

            String nomeArquivo =
                    pdfDir
                            .relativize(
                                    pdfPath
                            )
                            .toString();

            safeListener.onStatus(
                    "Processando "
                            + (index + 1)
                            + " de "
                            + pdfFiles.size()
                            + ": "
                            + nomeArquivo
            );

            /*
             * Extrai:
             *
             * - contas;
             * - chaves de acesso;
             * - NFCom;
             * - tipo;
             * - finalidade.
             */
            PdfContaDados dadosConta =
                    pdfService.extrairDadosConta(
                            pdfPath
                    );

            /*
             * Cada chave única representa
             * uma DANFE encontrada.
             */
            int danfesNoPdf =
                    dadosConta
                            .getChavesAcesso()
                            .size();

            danfesEncontradas +=
                    danfesNoPdf;

            safeListener.onStatus(
                    "DANFEs encontradas neste PDF: "
                            + danfesNoPdf
            );

            /*
             * Cruza as contas encontradas
             * no PDF com a planilha.
             */
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
                "Total de DANFEs encontradas: "
                        + danfesEncontradas
        );

        /*
         * Organização final.
         */
        safeListener.onStatus(
                "Organizando PDFs e XMLs por cenário..."
        );

        /*
         * Organiza os arquivos e gera
         * Contas_Encontradas.xlsx.
         *
         * O Excel também recebe:
         *
         * - PDFs avaliados;
         * - DANFEs encontradas;
         * - Contas encontradas.
         */
        spreadsheetService.writeResults(
                xmlZipService,
                pdfFiles.size(),
                contasEncontradas,
                danfesEncontradas
        );

        safeListener.onStatus(
                "Relatório Contas_Encontradas.xlsx gerado na pasta de saída."
        );

        safeListener.onStatus(
                "Processamento concluído. "
                        + pdfFiles.size()
                        + " PDF(s), "
                        + danfesEncontradas
                        + " DANFE(s) e "
                        + contasEncontradas
                        + " conta(s) encontrada(s)."
        );

        return new RunResult(
                pdfFiles.size(),
                contasEncontradas,
                danfesEncontradas
        );
    }

    private void validateInputs(
            Path pdfDir,
            Path outputDir,
            Path xlsxPath,
            Path xmlZipPath
    ) throws IOException {

        /*
         * Pasta dos PDFs.
         */
        if (
                pdfDir == null
                        || !Files.isDirectory(
                                pdfDir
                        )
        ) {

            throw new IllegalArgumentException(
                    "Selecione uma pasta de PDFs válida."
            );
        }

        /*
         * Planilha Excel.
         */
        if (
                xlsxPath == null
                        || !Files.isRegularFile(
                                xlsxPath
                        )
        ) {

            throw new IllegalArgumentException(
                    "Selecione uma planilha Excel válida."
            );
        }

        if (
                !PdfService.isXlsxFile(
                        xlsxPath
                )
        ) {

            throw new IllegalArgumentException(
                    "O arquivo selecionado deve possuir extensão .xlsx."
            );
        }

        /*
         * ZIP dos XMLs.
         */
        if (
                xmlZipPath == null
                        || !Files.isRegularFile(
                                xmlZipPath
                        )
        ) {

            throw new IllegalArgumentException(
                    "Selecione um arquivo ZIP contendo os XMLs."
            );
        }

        if (
                !XmlZipService.isZipFile(
                        xmlZipPath
                )
        ) {

            throw new IllegalArgumentException(
                    "O arquivo de XMLs deve possuir extensão .zip."
            );
        }

        /*
         * Pasta de saída.
         */
        if (
                outputDir == null
        ) {

            throw new IllegalArgumentException(
                    "Selecione uma pasta de saída."
            );
        }

        if (
                Files.exists(
                        outputDir
                )
                        && !Files.isDirectory(
                                outputDir
                        )
        ) {

            throw new IllegalArgumentException(
                    "O caminho de saída selecionado não é uma pasta."
            );
        }

        Files.createDirectories(
                outputDir
        );
    }

    private static class SilentProcessingListener
            implements ProcessingListener {

        @Override
        public void onStatus(
                String message
        ) {
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