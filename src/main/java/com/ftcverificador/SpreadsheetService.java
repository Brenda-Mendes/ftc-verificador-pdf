package com.ftcverificador;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class SpreadsheetService {

    private static final String ACCOUNT_COLUMN =
            "Conta";

    private static final String TEST_NAME_COLUMN =
            "Test name (initial)";

    private static final String REPORT_FILE_NAME =
            "Contas_Encontradas.xlsx";

    private final Path xlsxPath;
    private final Path outputRootDir;

    private final DataFormatter formatter =
            new DataFormatter();

    /*
     * Conta -> destinos da planilha.
     */
    private final Map<String, List<AccountDestination>>
            accountIndex;

    /*
     * Correspondências:
     *
     * conta + PDF + cenário.
     */
    private final List<PendingCopy> pendingCopies =
            new ArrayList<>();

    /*
     * Impede correspondências duplicadas.
     */
    private final Set<String> pendingCopyKeys =
            new LinkedHashSet<>();

    /*
     * Linhas que serão gravadas
     * no relatório.
     */
    private final List<FoundAccountRecord> foundAccounts =
            new ArrayList<>();

    public SpreadsheetService(
            Path xlsxPath,
            Path outputRootDir
    ) throws IOException {

        this.xlsxPath =
                xlsxPath
                        .toAbsolutePath()
                        .normalize();

        this.outputRootDir =
                outputRootDir
                        .toAbsolutePath()
                        .normalize();

        this.accountIndex =
                loadAccountIndex();
    }

    /*
     * Faz:
     *
     * Conta do PDF
     *      ↓
     * Conta da planilha
     *      ↓
     * Test name (initial)
     *
     * Também guarda as chaves
     * encontradas naquele PDF.
     */
    public int markMatches(
            PdfContaDados dadosConta,
            Path pdfPath
    ) {

        Path normalizedPdfPath =
                pdfPath
                        .toAbsolutePath()
                        .normalize();

        int matched = 0;

        for (
                String account :
                dadosConta.getContas()
        ) {

            String accountKey =
                    normalizeAccountForMatching(
                            account
                    );

            if (
                    accountKey.isEmpty()
            ) {
                continue;
            }

            List<AccountDestination> destinations =
                    accountIndex.get(
                            accountKey
                    );

            if (
                    destinations == null
            ) {
                continue;
            }

            for (
                    AccountDestination destination :
                    destinations
            ) {

                String pendingKey =
                        buildPendingCopyKey(
                                destination,
                                normalizedPdfPath
                        );

                if (
                        pendingCopyKeys.add(
                                pendingKey
                        )
                ) {

                    pendingCopies.add(
                            new PendingCopy(
                                    destination.testName,
                                    destination.originalAccount,
                                    normalizedPdfPath,
                                    destination.targetDir,
                                    dadosConta.getChavesAcesso()
                            )
                    );

                    matched++;
                }
            }
        }

        return matched;
    }

    /*
     * Organiza os arquivos e gera
     * o relatório final.
     */
    public void writeResults(
            XmlZipService xmlZipService,
            int pdfsAvaliados,
            int contasEncontradas,
            int danfesEncontradas
    ) throws IOException {

        if (
                xmlZipService == null
        ) {

            throw new IllegalArgumentException(
                    "O serviço de XMLs não foi informado."
            );
        }

        foundAccounts.clear();

        /*
         * Organiza:
         *
         * cenário
         *   └── PDF
         *       ├── PDF
         *       ├── XML
         *       └── XML
         */
        copyMatchedFilesByTestName(
                xmlZipService
        );

        /*
         * Gera o Excel com:
         *
         * - aba Resumo;
         * - aba Contas Encontradas.
         */
        writeFoundAccountsReport(
                pdfsAvaliados,
                contasEncontradas,
                danfesEncontradas
        );
    }

    /*
     * Carrega a planilha original.
     */
    private Map<String, List<AccountDestination>>
    loadAccountIndex() throws IOException {

        Map<String, List<AccountDestination>> index =
                new HashMap<>();

        try (
                FileInputStream input =
                        new FileInputStream(
                                xlsxPath.toFile()
                        );

                XSSFWorkbook workbook =
                        new XSSFWorkbook(
                                input
                        )
        ) {

            Sheet sheet =
                    workbook.getSheetAt(
                            0
                    );

            if (
                    sheet == null
            ) {

                throw new IllegalStateException(
                        "A primeira aba da planilha está vazia: "
                                + xlsxPath
                );
            }

            Row header =
                    sheet.getRow(
                            0
                    );

            if (
                    header == null
            ) {

                throw new IllegalStateException(
                        "A planilha não possui linha de cabeçalho."
                );
            }

            int accountColumn =
                    findRequiredColumn(
                            header,
                            ACCOUNT_COLUMN
                    );

            int testNameColumn =
                    findRequiredColumn(
                            header,
                            TEST_NAME_COLUMN
                    );

            for (
                    int rowIndex = 1;
                    rowIndex <= sheet.getLastRowNum();
                    rowIndex++
            ) {

                Row row =
                        sheet.getRow(
                                rowIndex
                        );

                if (
                        row == null
                ) {
                    continue;
                }

                String accountCellValue =
                        formatter.formatCellValue(
                                row.getCell(
                                        accountColumn
                                )
                        );

                List<String> accounts =
                        extractAccounts(
                                accountCellValue
                        );

                if (
                        accounts.isEmpty()
                ) {
                    continue;
                }

                String testName =
                        formatter.formatCellValue(
                                row.getCell(
                                        testNameColumn
                                )
                        );

                if (
                        testName != null
                ) {

                    testName =
                            testName.trim();
                }

                String folderName =
                        normalizeFolderName(
                                testName
                        );

                if (
                        folderName.isEmpty()
                ) {

                    throw new IllegalStateException(
                            "A coluna \""
                                    + TEST_NAME_COLUMN
                                    + "\" está vazia na linha "
                                    + (rowIndex + 1)
                                    + "."
                    );
                }

                Path targetDir =
                        outputRootDir
                                .resolve(
                                        folderName
                                )
                                .normalize();

                for (
                        String account :
                        accounts
                ) {

                    String accountKey =
                            normalizeAccountForMatching(
                                    account
                            );

                    if (
                            accountKey.isEmpty()
                    ) {
                        continue;
                    }

                    index
                            .computeIfAbsent(
                                    accountKey,
                                    ignored ->
                                            new ArrayList<>()
                            )
                            .add(
                                    new AccountDestination(
                                            targetDir,
                                            account,
                                            testName
                                    )
                            );
                }
            }
        }

        if (
                index.isEmpty()
        ) {

            throw new IllegalStateException(
                    "Nenhuma conta válida foi encontrada na coluna \""
                            + ACCOUNT_COLUMN
                            + "\"."
            );
        }

        return index;
    }

    private List<String> extractAccounts(
            String rawValue
    ) {

        List<String> accounts =
                new ArrayList<>();

        if (
                rawValue == null
                        || rawValue.isBlank()
        ) {

            return accounts;
        }

        for (
                String part :
                rawValue.split(
                        "\\R"
                )
        ) {

            String normalized =
                    normalizeAccount(
                            part
                    );

            if (
                    !normalized.isEmpty()
            ) {

                accounts.add(
                        normalized
                );
            }
        }

        return accounts;
    }

    /*
     * Agrupa por:
     *
     * cenário + PDF
     *
     * Assim o mesmo PDF não é copiado
     * várias vezes no mesmo cenário.
     */
    private void copyMatchedFilesByTestName(
            XmlZipService xmlZipService
    ) throws IOException {

        Map<String, PdfPackage> packages =
                new LinkedHashMap<>();

        for (
                PendingCopy pendingCopy :
                pendingCopies
        ) {

            String packageKey =
                    buildPdfPackageKey(
                            pendingCopy
                    );

            PdfPackage pdfPackage =
                    packages.computeIfAbsent(
                            packageKey,
                            ignored ->
                                    new PdfPackage(
                                            pendingCopy.testName,
                                            pendingCopy.targetDir,
                                            pendingCopy.sourcePath,
                                            pendingCopy.accessKeys
                                    )
                    );

            pdfPackage.matches.add(
                    pendingCopy
            );

            pdfPackage.accessKeys.addAll(
                    pendingCopy.accessKeys
            );
        }

        /*
         * Controla nomes de subpastas
         * dentro de cada cenário.
         */
        Map<Path, Set<String>> usedFolderNamesByScenario =
                new HashMap<>();

        for (
                PdfPackage pdfPackage :
                packages.values()
        ) {

            Path scenarioDir =
                    pdfPackage.targetDir;

            Files.createDirectories(
                    scenarioDir
            );

            Set<String> usedFolderNames =
                    usedFolderNamesByScenario
                            .computeIfAbsent(
                                    scenarioDir,
                                    ignored ->
                                            new LinkedHashSet<>()
                            );

            /*
             * Cria uma subpasta
             * com o nome do PDF.
             */
            String pdfFolderName =
                    buildPdfFolderName(
                            pdfPackage.sourcePath,
                            usedFolderNames
                    );

            Path pdfOutputDir =
                    scenarioDir.resolve(
                            pdfFolderName
                    );

            Files.createDirectories(
                    pdfOutputDir
            );

            /*
             * Mantém o nome original do PDF.
             */
            String pdfFileName =
                    pdfPackage
                            .sourcePath
                            .getFileName()
                            .toString();

            Path pdfTarget =
                    pdfOutputDir.resolve(
                            pdfFileName
                    );

            Files.copy(
                    pdfPackage.sourcePath,
                    pdfTarget,
                    StandardCopyOption.REPLACE_EXISTING
            );

            /*
             * Copia os XMLs associados
             * às chaves do PDF.
             *
             * Envia também o testName
             * para montar:
             *
             * CT001.02
             *
             * no nome do XML.
             */
            xmlZipService.copyXmlsForKeys(
                    pdfPackage.accessKeys,
                    pdfOutputDir,
                    pdfPackage.testName
            );

            /*
             * Caminho mostrado no relatório.
             */
            String reportPdfPath =
                    pdfFolderName
                            + "\\"
                            + pdfFileName;

            for (
                    PendingCopy match :
                    pdfPackage.matches
            ) {

                foundAccounts.add(
                        new FoundAccountRecord(
                                match.testName,
                                match.account,
                                reportPdfPath
                        )
                );
            }
        }
    }

    /*
     * Nome da subpasta baseado
     * no nome original do PDF.
     */
    private String buildPdfFolderName(
            Path sourcePath,
            Set<String> usedFolderNames
    ) {

        String originalName =
                sourcePath
                        .getFileName()
                        .toString();

        int dotIndex =
                originalName.lastIndexOf(
                        '.'
                );

        String baseName =
                dotIndex > 0
                        ? originalName.substring(
                                0,
                                dotIndex
                        )
                        : originalName;

        String preferredName =
                normalizeFolderName(
                        baseName
                );

        if (
                preferredName.isEmpty()
        ) {

            preferredName =
                    "PDF";
        }

        String preferredKey =
                preferredName.toLowerCase(
                        Locale.ROOT
                );

        if (
                usedFolderNames.add(
                        preferredKey
                )
        ) {

            return preferredName;
        }

        /*
         * Dois PDFs com mesmo nome:
         * adiciona hash do caminho.
         */
        String hash =
                Integer.toUnsignedString(
                        sourcePath
                                .toAbsolutePath()
                                .normalize()
                                .toString()
                                .hashCode(),
                        16
                );

        String hashedName =
                preferredName
                        + "_"
                        + hash;

        String hashedKey =
                hashedName.toLowerCase(
                        Locale.ROOT
                );

        if (
                usedFolderNames.add(
                        hashedKey
                )
        ) {

            return hashedName;
        }

        int sequence = 2;

        while (
                true
        ) {

            String candidate =
                    hashedName
                            + "_"
                            + sequence;

            String candidateKey =
                    candidate.toLowerCase(
                            Locale.ROOT
                    );

            if (
                    usedFolderNames.add(
                            candidateKey
                    )
            ) {

                return candidate;
            }

            sequence++;
        }
    }

    /*
     * Identificador único:
     *
     * cenário + caminho do PDF.
     */
    private String buildPdfPackageKey(
            PendingCopy pendingCopy
    ) {

        return pendingCopy
                .targetDir
                .toAbsolutePath()
                .normalize()
                + "\u0000"
                + pendingCopy
                .sourcePath
                .toAbsolutePath()
                .normalize();
    }

    /*
     * Gera Contas_Encontradas.xlsx.
     */
    private void writeFoundAccountsReport(
            int pdfsAvaliados,
            int contasEncontradas,
            int danfesEncontradas
    ) throws IOException {

        Files.createDirectories(
                outputRootDir
        );

        Path reportPath =
                outputRootDir.resolve(
                        REPORT_FILE_NAME
                );

        List<FoundAccountRecord> sortedRecords =
                new ArrayList<>(
                        foundAccounts
                );

        sortedRecords.sort(
                Comparator
                        .comparing(
                                (
                                        FoundAccountRecord record
                                ) ->
                                        record.testName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                        .thenComparing(
                                record ->
                                        normalizeAccountForMatching(
                                                record.account
                                        )
                        )
                        .thenComparing(
                                record ->
                                        record.pdfFileName,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        try (
                XSSFWorkbook workbook =
                        new XSSFWorkbook()
        ) {

            /*
             * PRIMEIRA ABA:
             *
             * Resumo da execução.
             */
            createSummarySheet(
                    workbook,
                    pdfsAvaliados,
                    contasEncontradas,
                    danfesEncontradas
            );

            /*
             * SEGUNDA ABA:
             *
             * Contas encontradas.
             */
            Sheet sheet =
                    workbook.createSheet(
                            "Contas Encontradas"
                    );

            sheet.setDisplayGridlines(
                    false
            );

            sheet.setDefaultRowHeightInPoints(
                    20
            );

            sheet.setZoom(
                    90
            );

            /*
             * Cabeçalho.
             */
            CellStyle headerStyle =
                    workbook.createCellStyle();

            headerStyle.setFillForegroundColor(
                    IndexedColors.DARK_RED
                            .getIndex()
            );

            headerStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            headerStyle.setAlignment(
                    HorizontalAlignment.CENTER
            );

            headerStyle.setVerticalAlignment(
                    VerticalAlignment.CENTER
            );

            headerStyle.setWrapText(
                    true
            );

            setBorders(
                    headerStyle
            );

            Font headerFont =
                    workbook.createFont();

            headerFont.setBold(
                    true
            );

            headerFont.setColor(
                    IndexedColors.WHITE
                            .getIndex()
            );

            headerFont.setFontHeightInPoints(
                    (short) 11
            );

            headerStyle.setFont(
                    headerFont
            );

            /*
             * Estilo normal.
             */
            CellStyle normalStyle =
                    workbook.createCellStyle();

            normalStyle.setVerticalAlignment(
                    VerticalAlignment.TOP
            );

            normalStyle.setAlignment(
                    HorizontalAlignment.LEFT
            );

            normalStyle.setWrapText(
                    true
            );

            setBorders(
                    normalStyle
            );

            Font normalFont =
                    workbook.createFont();

            normalFont.setFontHeightInPoints(
                    (short) 10
            );

            normalStyle.setFont(
                    normalFont
            );

            /*
             * Conta.
             */
            CellStyle accountStyle =
                    workbook.createCellStyle();

            accountStyle.cloneStyleFrom(
                    normalStyle
            );

            accountStyle.setAlignment(
                    HorizontalAlignment.CENTER
            );

            /*
             * Linha alternada.
             */
            CellStyle alternateStyle =
                    workbook.createCellStyle();

            alternateStyle.cloneStyleFrom(
                    normalStyle
            );

            alternateStyle.setFillForegroundColor(
                    IndexedColors.GREY_25_PERCENT
                            .getIndex()
            );

            alternateStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            /*
             * Conta na linha alternada.
             */
            CellStyle alternateAccountStyle =
                    workbook.createCellStyle();

            alternateAccountStyle.cloneStyleFrom(
                    alternateStyle
            );

            alternateAccountStyle.setAlignment(
                    HorizontalAlignment.CENTER
            );

            /*
             * Cabeçalho da tabela.
             */
            Row headerRow =
                    sheet.createRow(
                            0
                    );

            headerRow.setHeightInPoints(
                    30
            );

            String[] headers = {
                    "Caso de Teste",
                    "Conta",
                    "Arquivo PDF"
            };

            for (
                    int column = 0;
                    column < headers.length;
                    column++
            ) {

                Cell cell =
                        headerRow.createCell(
                                column
                        );

                cell.setCellValue(
                        headers[column]
                );

                cell.setCellStyle(
                        headerStyle
                );
            }

            /*
             * Dados.
             */
            int rowIndex = 1;

            for (
                    FoundAccountRecord record :
                    sortedRecords
            ) {

                Row row =
                        sheet.createRow(
                                rowIndex
                        );

                boolean alternate =
                        rowIndex % 2 == 0;

                CellStyle textStyle =
                        alternate
                                ? alternateStyle
                                : normalStyle;

                CellStyle accountCellStyle =
                        alternate
                                ? alternateAccountStyle
                                : accountStyle;

                /*
                 * Caso de Teste.
                 */
                Cell testCell =
                        row.createCell(
                                0
                        );

                testCell.setCellValue(
                        record.testName
                );

                testCell.setCellStyle(
                        textStyle
                );

                /*
                 * Conta.
                 */
                Cell accountCell =
                        row.createCell(
                                1
                        );

                accountCell.setCellValue(
                        record.account
                );

                accountCell.setCellStyle(
                        accountCellStyle
                );

                /*
                 * Arquivo PDF.
                 */
                Cell pdfCell =
                        row.createCell(
                                2
                        );

                pdfCell.setCellValue(
                        record.pdfFileName
                );

                pdfCell.setCellStyle(
                        textStyle
                );

                int estimatedLines =
                        Math.max(
                                estimateLines(
                                        record.testName,
                                        55
                                ),
                                estimateLines(
                                        record.pdfFileName,
                                        55
                                )
                        );

                float height =
                        Math.max(
                                27,
                                estimatedLines * 15f
                        );

                row.setHeightInPoints(
                        height
                );

                rowIndex++;
            }

            /*
             * Larguras.
             */
            sheet.setColumnWidth(
                    0,
                    55 * 256
            );

            sheet.setColumnWidth(
                    1,
                    20 * 256
            );

            sheet.setColumnWidth(
                    2,
                    60 * 256
            );

            /*
             * Congela cabeçalho.
             */
            sheet.createFreezePane(
                    0,
                    1
            );

            /*
             * Filtro automático.
             */
            if (
                    rowIndex > 1
            ) {

                sheet.setAutoFilter(
                        new CellRangeAddress(
                                0,
                                rowIndex - 1,
                                0,
                                2
                        )
                );
            }

            /*
             * Abre inicialmente na aba Resumo.
             */
            workbook.setActiveSheet(
                    0
            );

            workbook.setSelectedTab(
                    0
            );

            /*
             * Grava o relatório.
             */
            try (
                    OutputStream output =
                            Files.newOutputStream(
                                    reportPath
                            )
            ) {

                workbook.write(
                        output
                );
            }
        }
    }

    /*
     * Cria a aba Resumo.
     */
    private void createSummarySheet(
            XSSFWorkbook workbook,
            int pdfsAvaliados,
            int contasEncontradas,
            int danfesEncontradas
    ) {

        Sheet summarySheet =
                workbook.createSheet(
                        "Resumo"
                );

        summarySheet.setDisplayGridlines(
                false
        );

        summarySheet.setZoom(
                100
        );

        /*
         * Título.
         */
        CellStyle titleStyle =
                workbook.createCellStyle();

        titleStyle.setFillForegroundColor(
                IndexedColors.DARK_RED
                        .getIndex()
        );

        titleStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        titleStyle.setAlignment(
                HorizontalAlignment.CENTER
        );

        titleStyle.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        setBorders(
                titleStyle
        );

        Font titleFont =
                workbook.createFont();

        titleFont.setBold(
                true
        );

        titleFont.setColor(
                IndexedColors.WHITE
                        .getIndex()
        );

        titleFont.setFontHeightInPoints(
                (short) 14
        );

        titleStyle.setFont(
                titleFont
        );

        /*
         * Indicadores.
         */
        CellStyle labelStyle =
                workbook.createCellStyle();

        labelStyle.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT
                        .getIndex()
        );

        labelStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        labelStyle.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        labelStyle.setAlignment(
                HorizontalAlignment.LEFT
        );

        setBorders(
                labelStyle
        );

        Font labelFont =
                workbook.createFont();

        labelFont.setBold(
                true
        );

        labelFont.setFontHeightInPoints(
                (short) 11
        );

        labelStyle.setFont(
                labelFont
        );

        /*
         * Valores.
         */
        CellStyle valueStyle =
                workbook.createCellStyle();

        valueStyle.setAlignment(
                HorizontalAlignment.CENTER
        );

        valueStyle.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        setBorders(
                valueStyle
        );

        Font valueFont =
                workbook.createFont();

        valueFont.setBold(
                true
        );

        valueFont.setFontHeightInPoints(
                (short) 14
        );

        valueStyle.setFont(
                valueFont
        );

        /*
         * Título da aba.
         */
        Row titleRow =
                summarySheet.createRow(
                        0
                );

        titleRow.setHeightInPoints(
                32
        );

        Cell titleCell =
                titleRow.createCell(
                        0
                );

        titleCell.setCellValue(
                "Resumo da Execução"
        );

        titleCell.setCellStyle(
                titleStyle
        );

        Cell secondTitleCell =
                titleRow.createCell(
                        1
                );

        secondTitleCell.setCellStyle(
                titleStyle
        );

        summarySheet.addMergedRegion(
                new CellRangeAddress(
                        0,
                        0,
                        0,
                        1
                )
        );

        /*
         * PDFs.
         */
        createSummaryRow(
                summarySheet,
                2,
                "PDFs avaliados",
                pdfsAvaliados,
                labelStyle,
                valueStyle
        );

        /*
         * DANFEs.
         */
        createSummaryRow(
                summarySheet,
                3,
                "DANFEs encontradas",
                danfesEncontradas,
                labelStyle,
                valueStyle
        );

        /*
         * Contas.
         */
        createSummaryRow(
                summarySheet,
                4,
                "Contas encontradas",
                contasEncontradas,
                labelStyle,
                valueStyle
        );

        /*
         * Larguras.
         */
        summarySheet.setColumnWidth(
                0,
                32 * 256
        );

        summarySheet.setColumnWidth(
                1,
                20 * 256
        );
    }

    /*
     * Cria uma linha na aba Resumo.
     */
    private void createSummaryRow(
            Sheet sheet,
            int rowIndex,
            String label,
            int value,
            CellStyle labelStyle,
            CellStyle valueStyle
    ) {

        Row row =
                sheet.createRow(
                        rowIndex
                );

        row.setHeightInPoints(
                27
        );

        Cell labelCell =
                row.createCell(
                        0
                );

        labelCell.setCellValue(
                label
        );

        labelCell.setCellStyle(
                labelStyle
        );

        Cell valueCell =
                row.createCell(
                        1
                );

        valueCell.setCellValue(
                value
        );

        valueCell.setCellStyle(
                valueStyle
        );
    }

    private int estimateLines(
            String value,
            int charactersPerLine
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return 1;
        }

        int totalLines = 0;

        String[] explicitLines =
                value.split(
                        "\\R",
                        -1
                );

        for (
                String line :
                explicitLines
        ) {

            int length =
                    Math.max(
                            line.length(),
                            1
                    );

            totalLines +=
                    (int) Math.ceil(
                            length
                                    / (double)
                                    charactersPerLine
                    );
        }

        return Math.max(
                totalLines,
                1
        );
    }

    private void setBorders(
            CellStyle style
    ) {

        style.setBorderTop(
                BorderStyle.THIN
        );

        style.setBorderBottom(
                BorderStyle.THIN
        );

        style.setBorderLeft(
                BorderStyle.THIN
        );

        style.setBorderRight(
                BorderStyle.THIN
        );

        short borderColor =
                IndexedColors.GREY_40_PERCENT
                        .getIndex();

        style.setTopBorderColor(
                borderColor
        );

        style.setBottomBorderColor(
                borderColor
        );

        style.setLeftBorderColor(
                borderColor
        );

        style.setRightBorderColor(
                borderColor
        );
    }

    private String buildPendingCopyKey(
            AccountDestination destination,
            Path pdfPath
    ) {

        return destination.testName
                + "\u0000"
                + destination.originalAccount
                + "\u0000"
                + destination.targetDir
                + "\u0000"
                + pdfPath;
    }

    private int findRequiredColumn(
            Row header,
            String expectedTitle
    ) {

        String expected =
                normalizeLabel(
                        expectedTitle
                );

        int lastCell =
                Math.max(
                        header.getLastCellNum(),
                        0
                );

        for (
                int columnIndex = 0;
                columnIndex < lastCell;
                columnIndex++
        ) {

            Cell cell =
                    header.getCell(
                            columnIndex
                    );

            String current =
                    formatter.formatCellValue(
                            cell
                    );

            if (
                    normalizeLabel(
                            current
                    ).equals(
                            expected
                    )
            ) {

                return columnIndex;
            }
        }

        throw new IllegalStateException(
                "Coluna obrigatória não encontrada: \""
                        + expectedTitle
                        + "\". "
                        + "A planilha deve possuir as colunas "
                        + "\"Conta\" e "
                        + "\"Test name (initial)\" "
                        + "na primeira aba."
        );
    }

    private String normalizeLabel(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }

        String withoutAccents =
                Normalizer
                        .normalize(
                                value,
                                Normalizer.Form.NFD
                        )
                        .replaceAll(
                                "\\p{M}",
                                ""
                        );

        return withoutAccents
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                )
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private String normalizeAccount(
            String value
    ) {

        return value == null
                ? ""
                : value.replaceAll(
                        "\\D",
                        ""
                );
    }

    private String normalizeAccountForMatching(
            String value
    ) {

        String digitsOnly =
                normalizeAccount(
                        value
                );

        if (
                digitsOnly.isEmpty()
        ) {

            return "";
        }

        return digitsOnly.replaceFirst(
                "^0+(?!$)",
                ""
        );
    }

    private String normalizeFolderName(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }

        String safe =
                value
                        .trim()
                        .replaceAll(
                                "[\\\\/:*?\"<>|]",
                                "_"
                        )
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .replaceAll(
                                "[. ]+$",
                                ""
                        );

        if (
                safe.isEmpty()
                        || ".".equals(
                                safe
                        )
                        || "..".equals(
                                safe
                        )
        ) {

            return "";
        }

        if (
                safe.length() > 150
        ) {

            safe =
                    safe.substring(
                            0,
                            150
                    ).trim();
        }

        return safe;
    }

    /*
     * Destino encontrado na planilha.
     */
    private static class AccountDestination {

        private final Path targetDir;
        private final String originalAccount;
        private final String testName;

        private AccountDestination(
                Path targetDir,
                String originalAccount,
                String testName
        ) {

            this.targetDir =
                    targetDir;

            this.originalAccount =
                    originalAccount;

            this.testName =
                    testName;
        }
    }

    /*
     * Correspondência encontrada.
     */
    private static class PendingCopy {

        private final String testName;
        private final String account;
        private final Path sourcePath;
        private final Path targetDir;
        private final Set<String> accessKeys;

        private PendingCopy(
                String testName,
                String account,
                Path sourcePath,
                Path targetDir,
                Set<String> accessKeys
        ) {

            this.testName =
                    testName;

            this.account =
                    account;

            this.sourcePath =
                    sourcePath;

            this.targetDir =
                    targetDir;

            this.accessKeys =
                    new LinkedHashSet<>();

            if (
                    accessKeys != null
            ) {

                this.accessKeys.addAll(
                        accessKeys
                );
            }
        }
    }

    /*
     * Um PDF dentro de um cenário.
     *
     * Guarda também o testName
     * para renomear os XMLs.
     */
    private static class PdfPackage {

        private final String testName;
        private final Path targetDir;
        private final Path sourcePath;

        private final Set<String> accessKeys =
                new LinkedHashSet<>();

        private final List<PendingCopy> matches =
                new ArrayList<>();

        private PdfPackage(
                String testName,
                Path targetDir,
                Path sourcePath,
                Set<String> accessKeys
        ) {

            this.testName =
                    testName;

            this.targetDir =
                    targetDir;

            this.sourcePath =
                    sourcePath;

            if (
                    accessKeys != null
            ) {

                this.accessKeys.addAll(
                        accessKeys
                );
            }
        }
    }

    /*
     * Linha da aba Contas Encontradas.
     */
    private static class FoundAccountRecord {

        private final String testName;
        private final String account;
        private final String pdfFileName;

        private FoundAccountRecord(
                String testName,
                String account,
                String pdfFileName
        ) {

            this.testName =
                    testName;

            this.account =
                    account;

            this.pdfFileName =
                    pdfFileName;
        }
    }
}