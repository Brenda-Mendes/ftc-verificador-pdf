package com.ftcverificador;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

    private static final String ACCOUNT_COLUMN = "Conta";
    private static final String TEST_NAME_COLUMN = "Test name (initial)";
    private static final String REPORT_SHEET_NAME = "Contas Encontradas";
    private static final String SCENARIO_SUMMARY_SHEET_NAME = "Resumo por Cenário";
    private static final String NOT_FOUND_SHEET_NAME = "CTs Não Encontrados";

    public static final String REPORT_FILE_NAME = "Contas_Encontradas.xlsx";

    private static final Pattern CT_PATTERN = Pattern.compile(
            "(?i)\\bCT\\s*(\\d{1,3})\\s*\\.\\s*(\\d{1,2})\\b"
    );

    private final Path xlsxPath;
    private final Path outputRootDir;
    private final Path reportPath;

    private final DataFormatter formatter = new DataFormatter();

    private final Map<String, List<AccountDestination>> accountIndex;
    private final Map<String, String> expectedScenariosByCt = new LinkedHashMap<>();
    private final Map<String, LinkedHashSet<String>> expectedAccountsByCt = new LinkedHashMap<>();
    private final Set<String> filledCtLabels = new LinkedHashSet<>();

    private final List<PendingCopy> pendingCopies = new ArrayList<>();
    private final Set<String> scheduledAccountKeys = new LinkedHashSet<>();

    private final List<FoundAccountRecord> foundAccounts = new ArrayList<>();
    private final Set<String> foundAccountKeys = new LinkedHashSet<>();

    public SpreadsheetService(
            Path xlsxPath,
            Path outputRootDir
    ) throws IOException {

        if (xlsxPath == null) {
            throw new IllegalArgumentException(
                    "A planilha de cenários não foi informada."
            );
        }

        if (outputRootDir == null) {
            throw new IllegalArgumentException(
                    "A pasta final não foi informada."
            );
        }

        this.xlsxPath = xlsxPath.toAbsolutePath().normalize();
        this.outputRootDir = outputRootDir.toAbsolutePath().normalize();
        this.reportPath = this.outputRootDir.resolve(REPORT_FILE_NAME);

        Files.createDirectories(this.outputRootDir);

        this.accountIndex = loadAccountIndex();
    }

    public int markMatches(
            PdfContaDados dadosConta,
            Path pdfPath
    ) {

        if (dadosConta == null || pdfPath == null) {
            return 0;
        }

        Path normalizedPdfPath = pdfPath.toAbsolutePath().normalize();
        int matched = 0;

        for (String account : dadosConta.getContas()) {
            String accountKey = normalizeAccountForMatching(account);

            if (accountKey.isEmpty()) {
                continue;
            }

            if (scheduledAccountKeys.contains(accountKey)) {
                continue;
            }

            List<AccountDestination> destinations = accountIndex.get(accountKey);

            if (destinations == null || destinations.isEmpty()) {
                continue;
            }

            AccountDestination destination = destinations.get(0);

            pendingCopies.add(
                    new PendingCopy(
                            destination.testName,
                            destination.originalAccount,
                            normalizedPdfPath,
                            destination.targetDir,
                            dadosConta.getChavesAcesso()
                    )
            );

            scheduledAccountKeys.add(accountKey);
            matched++;
        }

        return matched;
    }

    public void writeResults(
            XmlZipService xmlZipService,
            int pdfsAvaliados,
            int contasEncontradas,
            int danfesEncontradas
    ) throws IOException {

        if (xmlZipService == null) {
            throw new IllegalArgumentException(
                    "O serviço de XMLs não foi informado."
            );
        }

        foundAccounts.clear();
        foundAccountKeys.clear();

        copyMatchedFilesByTestName(xmlZipService);

        writeFoundAccountsReport(
                pdfsAvaliados,
                danfesEncontradas
        );
    }

    public void writeResults(
            XmlZipService xmlZipService
    ) throws IOException {
        writeResults(xmlZipService, 0, 0, 0);
    }

    private Map<String, List<AccountDestination>> loadAccountIndex()
            throws IOException {

        Map<String, List<AccountDestination>> index = new HashMap<>();

        try (
                FileInputStream input = new FileInputStream(xlsxPath.toFile());
                XSSFWorkbook workbook = new XSSFWorkbook(input)
        ) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                throw new IllegalStateException(
                        "A primeira aba da planilha está vazia: " + xlsxPath
                );
            }

            Row header = sheet.getRow(0);

            if (header == null) {
                throw new IllegalStateException(
                        "A planilha não possui linha de cabeçalho."
                );
            }

            int accountColumn = findRequiredColumn(header, ACCOUNT_COLUMN);
            int testNameColumn = findRequiredColumn(header, TEST_NAME_COLUMN);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null) {
                    continue;
                }

                String testName = formatter.formatCellValue(
                        row.getCell(testNameColumn)
                );

                testName = testName == null ? "" : testName.trim();

                if (testName.isBlank()) {
                    continue;
                }

                String scenarioFolderName = buildScenarioFolderName(testName);

                if (scenarioFolderName.isEmpty()) {
                    throw new IllegalStateException(
                            "A coluna \""
                                    + TEST_NAME_COLUMN
                                    + "\" está vazia na linha "
                                    + (rowIndex + 1)
                                    + "."
                    );
                }

                String ctLabel = extractCtLabel(testName);

                expectedScenariosByCt.putIfAbsent(
                        ctLabel,
                        scenarioFolderName
                );

                expectedAccountsByCt.computeIfAbsent(
                        ctLabel,
                        ignored -> new LinkedHashSet<>()
                );

                String accountCellValue = formatter.formatCellValue(
                        row.getCell(accountColumn)
                );

                List<String> accounts = extractAccounts(accountCellValue);

                if (accounts.isEmpty()) {
                    continue;
                }

                filledCtLabels.add(ctLabel);
                expectedAccountsByCt.get(ctLabel).addAll(accounts);

                Path targetDir = outputRootDir
                        .resolve(scenarioFolderName)
                        .normalize();

                for (String account : accounts) {
                    String accountKey = normalizeAccountForMatching(account);

                    if (accountKey.isEmpty()) {
                        continue;
                    }

                    List<AccountDestination> destinations = index.computeIfAbsent(
                            accountKey,
                            ignored -> new ArrayList<>()
                    );

                    boolean alreadyExists = false;

                    for (AccountDestination destination : destinations) {
                        if (destination.testName.equalsIgnoreCase(testName)
                                && destination.targetDir.equals(targetDir)) {
                            alreadyExists = true;
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        destinations.add(
                                new AccountDestination(
                                        targetDir,
                                        account,
                                        testName
                                )
                        );
                    }
                }
            }
        }

        if (index.isEmpty()) {
            throw new IllegalStateException(
                    "Nenhuma conta válida foi encontrada na coluna \""
                            + ACCOUNT_COLUMN
                            + "\"."
            );
        }

        return index;
    }

    private List<String> extractAccounts(String rawValue) {
        List<String> accounts = new ArrayList<>();

        if (rawValue == null || rawValue.isBlank()) {
            return accounts;
        }

        for (String part : rawValue.split("\\R")) {
            String normalized = normalizeAccount(part);

            if (!normalized.isEmpty()) {
                accounts.add(normalized);
            }
        }

        return accounts;
    }

    private void copyMatchedFilesByTestName(
            XmlZipService xmlZipService
    ) throws IOException {

        for (PendingCopy pendingCopy : pendingCopies) {
            Path scenarioDir = pendingCopy.targetDir;
            Files.createDirectories(scenarioDir);

            String accountFolderName = normalizeFolderName(
                    pendingCopy.account
            );

            if (accountFolderName.isEmpty()) {
                accountFolderName = "CONTA_NAO_IDENTIFICADA";
            }

            Path accountDir = scenarioDir.resolve(accountFolderName);

            recreateDirectory(accountDir);

            String ctLabel = extractCtLabel(pendingCopy.testName);
            String pdfFileName = ctLabel + ".pdf";
            Path pdfTarget = accountDir.resolve(pdfFileName);

            Files.copy(
                    pendingCopy.sourcePath,
                    pdfTarget,
                    StandardCopyOption.REPLACE_EXISTING
            );

            xmlZipService.copyXmlsForKeys(
                    pendingCopy.accessKeys,
                    accountDir,
                    pendingCopy.testName
            );

            String reportPdfPath = accountFolderName
                    + "\\"
                    + pdfFileName;

            addFoundAccountRecord(
                    pendingCopy.testName,
                    pendingCopy.account,
                    reportPdfPath
            );
        }
    }

    private void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> stream = Files.walk(directory)) {
                List<Path> paths = stream
                        .sorted((left, right) -> right.compareTo(left))
                        .toList();

                for (Path path : paths) {
                    Files.deleteIfExists(path);
                }
            }
        }

        Files.createDirectories(directory);
    }

    private void addFoundAccountRecord(
            String testName,
            String account,
            String pdfFileName
    ) {
        String accountKey = normalizeAccountForMatching(account);

        if (accountKey.isEmpty() || !foundAccountKeys.add(accountKey)) {
            return;
        }

        foundAccounts.add(
                new FoundAccountRecord(
                        testName,
                        account,
                        pdfFileName
                )
        );
    }

    private void writeFoundAccountsReport(
            int pdfsAvaliados,
            int danfesEncontradas
    ) throws IOException {

        Files.createDirectories(outputRootDir);

        int totalContasPlanilha = getTotalExpectedAccountsCount();
        int contasEncontradas = getFoundAccountsCount();
        int contasNaoEncontradas = Math.max(
                totalContasPlanilha - contasEncontradas,
                0
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            createSummarySheet(
                    workbook,
                    pdfsAvaliados,
                    danfesEncontradas,
                    contasEncontradas,
                    contasNaoEncontradas,
                    totalContasPlanilha
            );

            createAccountsSheet(workbook, foundAccounts);
            createScenarioSummarySheet(workbook, foundAccounts);
            createNotFoundCtSheet(workbook, foundAccounts);
            workbook.setActiveSheet(0);

            try (OutputStream output = Files.newOutputStream(reportPath)) {
                workbook.write(output);
            }
        }
    }

    private void createAccountsSheet(
            XSSFWorkbook workbook,
            List<FoundAccountRecord> records
    ) {
        Sheet sheet = workbook.createSheet(REPORT_SHEET_NAME);
        sheet.setDisplayGridlines(false);
        sheet.setDefaultRowHeightInPoints(20);
        sheet.setZoom(90);

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle normalStyle = createNormalStyle(workbook);

        CellStyle accountStyle = workbook.createCellStyle();
        accountStyle.cloneStyleFrom(normalStyle);
        accountStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle alternateStyle = workbook.createCellStyle();
        alternateStyle.cloneStyleFrom(normalStyle);
        alternateStyle.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT.getIndex()
        );
        alternateStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        CellStyle alternateAccountStyle = workbook.createCellStyle();
        alternateAccountStyle.cloneStyleFrom(alternateStyle);
        alternateAccountStyle.setAlignment(
                HorizontalAlignment.CENTER
        );

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(30);

        String[] headers = {
                "Caso de Teste",
                "Conta",
                "Arquivo PDF"
        };

        for (int column = 0; column < headers.length; column++) {
            Cell cell = headerRow.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = 1;

        for (FoundAccountRecord record : records) {
            Row row = sheet.createRow(rowIndex);
            boolean alternate = rowIndex % 2 == 0;

            CellStyle textStyle = alternate
                    ? alternateStyle
                    : normalStyle;

            CellStyle accountCellStyle = alternate
                    ? alternateAccountStyle
                    : accountStyle;

            createTextCell(row, 0, record.testName, textStyle);
            createTextCell(row, 1, record.account, accountCellStyle);
            createTextCell(row, 2, record.pdfFileName, textStyle);

            int estimatedLines = Math.max(
                    estimateLines(record.testName, 55),
                    estimateLines(record.pdfFileName, 50)
            );

            row.setHeightInPoints(
                    Math.max(27, estimatedLines * 15f)
            );

            rowIndex++;
        }

        sheet.setColumnWidth(0, 55 * 256);
        sheet.setColumnWidth(1, 22 * 256);
        sheet.setColumnWidth(2, 46 * 256);
        sheet.createFreezePane(0, 1);

        if (rowIndex > 1) {
            sheet.setAutoFilter(
                    new CellRangeAddress(
                            0,
                            rowIndex - 1,
                            0,
                            2
                    )
            );
        }
    }

    private void createNotFoundCtSheet(
            XSSFWorkbook workbook,
            List<FoundAccountRecord> records
    ) {

        Sheet sheet = workbook.createSheet(NOT_FOUND_SHEET_NAME);
        sheet.setDisplayGridlines(false);
        sheet.setDefaultRowHeightInPoints(20);
        sheet.setZoom(90);

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle normalStyle = createNormalStyle(workbook);

        CellStyle centeredStyle = workbook.createCellStyle();
        centeredStyle.cloneStyleFrom(normalStyle);
        centeredStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle alternateStyle = workbook.createCellStyle();
        alternateStyle.cloneStyleFrom(normalStyle);
        alternateStyle.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT.getIndex()
        );
        alternateStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        CellStyle alternateCenteredStyle = workbook.createCellStyle();
        alternateCenteredStyle.cloneStyleFrom(alternateStyle);
        alternateCenteredStyle.setAlignment(
                HorizontalAlignment.CENTER
        );

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(30);

        String[] headers = {
                "CT",
                "Cenário",
                "Contas Selecionadas"
        };

        for (int column = 0; column < headers.length; column++) {
            Cell cell = headerRow.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(headerStyle);
        }

        Set<String> foundCts = new LinkedHashSet<>();

        for (FoundAccountRecord record : records) {
            if (record.testName == null || record.testName.isBlank()) {
                continue;
            }

            Matcher matcher = CT_PATTERN.matcher(record.testName);

            if (!matcher.find()) {
                continue;
            }

            int mainNumber = Integer.parseInt(matcher.group(1));
            int subNumber = Integer.parseInt(matcher.group(2));

            String ctLabel = String.format(
                    Locale.ROOT,
                    "CT%03d.%02d",
                    mainNumber,
                    subNumber
            );

            foundCts.add(ctLabel);
        }

        int rowIndex = 1;

        for (
                Map.Entry<String, String> entry :
                        expectedScenariosByCt.entrySet()
        ) {
            String ctLabel = entry.getKey();

            if (foundCts.contains(ctLabel)) {
                continue;
            }

            Row row = sheet.createRow(rowIndex);
            boolean alternate = rowIndex % 2 == 0;

            CellStyle textStyle = alternate
                    ? alternateStyle
                    : normalStyle;

            CellStyle ctStyle = alternate
                    ? alternateCenteredStyle
                    : centeredStyle;

            createTextCell(
                    row,
                    0,
                    ctLabel,
                    ctStyle
            );

            createTextCell(
                    row,
                    1,
                    entry.getValue(),
                    textStyle
            );

            LinkedHashSet<String> accounts = expectedAccountsByCt.getOrDefault(
                    ctLabel,
                    new LinkedHashSet<>()
            );

            String accountsText = String.join(
                    System.lineSeparator(),
                    accounts
            );

            createTextCell(
                    row,
                    2,
                    accountsText,
                    textStyle
            );

            int accountLines = Math.max(accounts.size(), 1);
            int scenarioLines = estimateLines(entry.getValue(), 65);

            row.setHeightInPoints(
                    Math.max(
                            27,
                            Math.max(accountLines, scenarioLines) * 15f
                    )
            );

            rowIndex++;
        }

        sheet.setColumnWidth(0, 16 * 256);
        sheet.setColumnWidth(1, 75 * 256);
        sheet.setColumnWidth(2, 28 * 256);
        sheet.createFreezePane(0, 1);

        if (rowIndex > 1) {
            sheet.setAutoFilter(
                    new CellRangeAddress(
                            0,
                            rowIndex - 1,
                            0,
                            2
                    )
            );
        }
    }

    private void createScenarioSummarySheet(
            XSSFWorkbook workbook,
            List<FoundAccountRecord> records
    ) throws IOException {

        Sheet sheet = workbook.createSheet(
                SCENARIO_SUMMARY_SHEET_NAME
        );

        sheet.setDisplayGridlines(false);
        sheet.setDefaultRowHeightInPoints(20);
        sheet.setZoom(90);

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle normalStyle = createNormalStyle(workbook);

        CellStyle centeredStyle = workbook.createCellStyle();
        centeredStyle.cloneStyleFrom(normalStyle);
        centeredStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle alternateStyle = workbook.createCellStyle();
        alternateStyle.cloneStyleFrom(normalStyle);
        alternateStyle.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT.getIndex()
        );
        alternateStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        CellStyle alternateCenteredStyle = workbook.createCellStyle();
        alternateCenteredStyle.cloneStyleFrom(alternateStyle);
        alternateCenteredStyle.setAlignment(
                HorizontalAlignment.CENTER
        );

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(30);

        String[] headers = {
                "Cenário",
                "Qtd. Contas",
                "Conta",
                "Qtd. XMLs"
        };

        for (int column = 0; column < headers.length; column++) {
            Cell cell = headerRow.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(headerStyle);
        }

        Map<String, LinkedHashMap<String, String>> accountsByScenario =
                new LinkedHashMap<>();

        for (FoundAccountRecord record : records) {
            String scenarioFolderName = buildScenarioFolderName(
                    record.testName
            );

            if (scenarioFolderName.isBlank()) {
                scenarioFolderName = record.testName == null
                        ? ""
                        : record.testName.trim();
            }

            String accountKey = normalizeAccountForMatching(
                    record.account
            );

            if (scenarioFolderName.isBlank() || accountKey.isEmpty()) {
                continue;
            }

            accountsByScenario
                    .computeIfAbsent(
                            scenarioFolderName,
                            ignored -> new LinkedHashMap<>()
                    )
                    .putIfAbsent(
                            accountKey,
                            record.account
                    );
        }

        int rowIndex = 1;

        for (
                Map.Entry<String, LinkedHashMap<String, String>> scenarioEntry :
                        accountsByScenario.entrySet()
        ) {
            String scenarioName = scenarioEntry.getKey();
            LinkedHashMap<String, String> accounts =
                    scenarioEntry.getValue();

            int accountCount = accounts.size();

            for (String account : accounts.values()) {
                Row row = sheet.createRow(rowIndex);
                boolean alternate = rowIndex % 2 == 0;

                CellStyle textStyle = alternate
                        ? alternateStyle
                        : normalStyle;

                CellStyle numberStyle = alternate
                        ? alternateCenteredStyle
                        : centeredStyle;

                int xmlCount = countXmlFilesForAccount(
                        scenarioName,
                        account
                );

                createTextCell(
                        row,
                        0,
                        scenarioName,
                        textStyle
                );

                Cell accountCountCell = row.createCell(1);
                accountCountCell.setCellValue(accountCount);
                accountCountCell.setCellStyle(numberStyle);

                createTextCell(
                        row,
                        2,
                        account,
                        numberStyle
                );

                Cell xmlCountCell = row.createCell(3);
                xmlCountCell.setCellValue(xmlCount);
                xmlCountCell.setCellStyle(numberStyle);

                int estimatedLines = estimateLines(
                        scenarioName,
                        55
                );

                row.setHeightInPoints(
                        Math.max(27, estimatedLines * 15f)
                );

                rowIndex++;
            }
        }

        sheet.setColumnWidth(0, 58 * 256);
        sheet.setColumnWidth(1, 16 * 256);
        sheet.setColumnWidth(2, 24 * 256);
        sheet.setColumnWidth(3, 16 * 256);
        sheet.createFreezePane(0, 1);

        if (rowIndex > 1) {
            sheet.setAutoFilter(
                    new CellRangeAddress(
                            0,
                            rowIndex - 1,
                            0,
                            3
                    )
            );
        }
    }

    private int countXmlFilesForAccount(
            String scenarioFolderName,
            String account
    ) throws IOException {

        String accountFolderName = normalizeFolderName(account);

        if (scenarioFolderName == null
                || scenarioFolderName.isBlank()
                || accountFolderName.isBlank()) {
            return 0;
        }

        Path accountDir = outputRootDir
                .resolve(scenarioFolderName)
                .resolve(accountFolderName)
                .normalize();

        if (!Files.isDirectory(accountDir)) {
            return 0;
        }

        try (Stream<Path> stream = Files.walk(accountDir)) {
            long count = stream
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .toLowerCase(Locale.ROOT)
                                    .endsWith(".xml")
                    )
                    .count();

            return count > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) count;
        }
    }

    private void createSummarySheet(
            XSSFWorkbook workbook,
            int pdfsAvaliados,
            int danfesEncontradas,
            int contasEncontradas,
            int contasNaoEncontradas,
            int totalContasPlanilha
    ) {
        Sheet sheet = workbook.createSheet("Resumo");
        sheet.setDisplayGridlines(false);
        sheet.setZoom(100);

        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFillForegroundColor(
                IndexedColors.DARK_RED.getIndex()
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
        setBorders(titleStyle);

        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setColor(
                IndexedColors.WHITE.getIndex()
        );
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        CellStyle labelStyle = workbook.createCellStyle();
        labelStyle.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT.getIndex()
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
        setBorders(labelStyle);

        Font labelFont = workbook.createFont();
        labelFont.setBold(true);
        labelFont.setFontHeightInPoints((short) 11);
        labelStyle.setFont(labelFont);

        CellStyle valueStyle = workbook.createCellStyle();
        valueStyle.setAlignment(
                HorizontalAlignment.CENTER
        );
        valueStyle.setVerticalAlignment(
                VerticalAlignment.CENTER
        );
        valueStyle.setWrapText(true);
        setBorders(valueStyle);

        Font valueFont = workbook.createFont();
        valueFont.setBold(true);
        valueFont.setFontHeightInPoints((short) 11);
        valueStyle.setFont(valueFont);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(32);

        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Resumo da Execução");
        titleCell.setCellStyle(titleStyle);

        Cell titleCell2 = titleRow.createCell(1);
        titleCell2.setCellStyle(titleStyle);

        sheet.addMergedRegion(
                new CellRangeAddress(0, 0, 0, 1)
        );

        createSummaryRow(
                sheet,
                2,
                "PDFs avaliados nesta execução",
                pdfsAvaliados,
                labelStyle,
                valueStyle
        );

        createSummaryRow(
                sheet,
                3,
                "DANFEs encontradas nesta execução",
                danfesEncontradas,
                labelStyle,
                valueStyle
        );

        createSummaryRow(
                sheet,
                4,
                "Contas encontradas",
                contasEncontradas,
                labelStyle,
                valueStyle
        );

        createSummaryRow(
                sheet,
                5,
                "Contas não encontradas",
                contasNaoEncontradas,
                labelStyle,
                valueStyle
        );

        createSummaryRow(
                sheet,
                6,
                "Total de contas únicas na planilha",
                totalContasPlanilha,
                labelStyle,
                valueStyle
        );

        createSummaryRow(
                sheet,
                8,
                "Total de CTs",
                getTotalCtCount(),
                labelStyle,
                valueStyle
        );

        createSummaryRow(
                sheet,
                9,
                "Total de CTs Preenchidos",
                getFilledCtCount(),
                labelStyle,
                valueStyle
        );

        createSummaryRow(
                sheet,
                10,
                "Total de CTs Não Preenchidos",
                getUnfilledCtCount(),
                labelStyle,
                valueStyle
        );

        sheet.setColumnWidth(0, 42 * 256);
        sheet.setColumnWidth(1, 30 * 256);
    }

    private CellStyle createHeaderStyle(
            XSSFWorkbook workbook
    ) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(
                IndexedColors.DARK_RED.getIndex()
        );
        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );
        style.setAlignment(
                HorizontalAlignment.CENTER
        );
        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );
        style.setWrapText(true);
        setBorders(style);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(
                IndexedColors.WHITE.getIndex()
        );
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        return style;
    }

    private CellStyle createNormalStyle(
            XSSFWorkbook workbook
    ) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(
                VerticalAlignment.TOP
        );
        style.setAlignment(
                HorizontalAlignment.LEFT
        );
        style.setWrapText(true);
        setBorders(style);

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        return style;
    }

    private void createTextCell(
            Row row,
            int column,
            String value,
            CellStyle style
    ) {
        Cell cell = row.createCell(column);
        cell.setCellValue(
                value == null ? "" : value
        );
        cell.setCellStyle(style);
    }

    private void createSummaryRow(
            Sheet sheet,
            int rowIndex,
            String label,
            int value,
            CellStyle labelStyle,
            CellStyle valueStyle
    ) {
        createSummaryRow(
                sheet,
                rowIndex,
                label,
                String.valueOf(value),
                labelStyle,
                valueStyle
        );
    }

    private void createSummaryRow(
            Sheet sheet,
            int rowIndex,
            String label,
            String value,
            CellStyle labelStyle,
            CellStyle valueStyle
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(27);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(
                value == null ? "" : value
        );
        valueCell.setCellStyle(valueStyle);
    }

    private String buildScenarioFolderName(
            String testName
    ) {
        if (testName == null || testName.isBlank()) {
            return "";
        }

        String[] parts = testName.split("\\s*-\\s*");
        List<String> filteredParts = new ArrayList<>();

        for (String part : parts) {
            String current = part == null
                    ? ""
                    : part.trim();

            if (current.isEmpty()) {
                continue;
            }

            if (current.matches("(?i)^\\(?FAT\\)?$")) {
                continue;
            }

            filteredParts.add(current);
        }

        return normalizeFolderName(
                String.join(" - ", filteredParts)
        );
    }

    private String extractCtLabel(
            String testName
    ) {
        if (testName == null || testName.isBlank()) {
            throw new IllegalStateException(
                    "Não foi possível identificar o CT: "
                            + "o Test name (initial) está vazio."
            );
        }

        Matcher matcher = CT_PATTERN.matcher(testName);

        if (!matcher.find()) {
            throw new IllegalStateException(
                    "Não foi possível identificar o CT no cenário: "
                            + testName
            );
        }

        int mainNumber = Integer.parseInt(
                matcher.group(1)
        );

        int subNumber = Integer.parseInt(
                matcher.group(2)
        );

        return String.format(
                Locale.ROOT,
                "CT%03d.%02d",
                mainNumber,
                subNumber
        );
    }

    private int findRequiredColumn(
            Row header,
            String expectedTitle
    ) {
        int column = findColumn(
                header,
                expectedTitle
        );

        if (column >= 0) {
            return column;
        }

        throw new IllegalStateException(
                "Coluna obrigatória não encontrada: \""
                        + expectedTitle
                        + "\". A planilha deve possuir "
                        + "\"Conta\" e \"Test name (initial)\"."
        );
    }

    private int findColumn(
            Row header,
            String expectedTitle
    ) {
        if (header == null) {
            return -1;
        }

        String expected = normalizeLabel(
                expectedTitle
        );

        int lastCell = Math.max(
                header.getLastCellNum(),
                0
        );

        for (
                int columnIndex = 0;
                columnIndex < lastCell;
                columnIndex++
        ) {
            Cell cell = header.getCell(columnIndex);
            String current = formatter.formatCellValue(cell);

            if (
                    normalizeLabel(current)
                            .equals(expected)
            ) {
                return columnIndex;
            }
        }

        return -1;
    }

    private String normalizeLabel(
            String value
    ) {
        if (value == null) {
            return "";
        }

        String withoutAccents = Normalizer
                .normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "");

        return withoutAccents
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeAccount(
            String value
    ) {
        return value == null
                ? ""
                : value.replaceAll("\\D", "");
    }

    private String normalizeAccountForMatching(
            String value
    ) {
        String digitsOnly = normalizeAccount(value);

        if (digitsOnly.isEmpty()) {
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
        if (value == null) {
            return "";
        }

        String safe = value
                .trim()
                .replaceAll(
                        "[\\\\/:*?\"<>|]",
                        "_"
                )
                .replaceAll("\\s+", " ")
                .replaceAll("[. ]+$", "");

        if (
                safe.isEmpty()
                        || ".".equals(safe)
                        || "..".equals(safe)
        ) {
            return "";
        }

        if (safe.length() > 110) {
            safe = safe
                    .substring(0, 110)
                    .trim();
        }

        return safe;
    }

    private int estimateLines(
            String value,
            int charactersPerLine
    ) {
        if (value == null || value.isBlank()) {
            return 1;
        }

        int totalLines = 0;

        for (
                String line :
                        value.split("\\R", -1)
        ) {
            int length = Math.max(
                    line.length(),
                    1
            );

            totalLines += (int) Math.ceil(
                    length
                            / (double) charactersPerLine
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
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        short borderColor =
                IndexedColors.GREY_40_PERCENT.getIndex();

        style.setTopBorderColor(borderColor);
        style.setBottomBorderColor(borderColor);
        style.setLeftBorderColor(borderColor);
        style.setRightBorderColor(borderColor);
    }

    public int getFoundAccountsCount() {
        return foundAccounts.size();
    }

    public int getTotalExpectedAccountsCount() {
        return accountIndex.size();
    }

    public int getNotFoundAccountsCount() {
        return Math.max(
                getTotalExpectedAccountsCount()
                        - getFoundAccountsCount(),
                0
        );
    }

    public int getTotalCtCount() {
        return expectedScenariosByCt.size();
    }

    public int getFilledCtCount() {
        return filledCtLabels.size();
    }

    public int getUnfilledCtCount() {
        return Math.max(
                getTotalCtCount() - getFilledCtCount(),
                0
        );
    }

    public Path getReportPath() {
        return reportPath;
    }

    private static class AccountDestination {
        private final Path targetDir;
        private final String originalAccount;
        private final String testName;

        private AccountDestination(
                Path targetDir,
                String originalAccount,
                String testName
        ) {
            this.targetDir = targetDir;
            this.originalAccount = originalAccount;
            this.testName = testName;
        }
    }

    private static class PendingCopy {
        private final String testName;
        private final String account;
        private final Path sourcePath;
        private final Path targetDir;
        private final Set<String> accessKeys =
                new LinkedHashSet<>();

        private PendingCopy(
                String testName,
                String account,
                Path sourcePath,
                Path targetDir,
                Set<String> accessKeys
        ) {
            this.testName = testName;
            this.account = account;
            this.sourcePath = sourcePath;
            this.targetDir = targetDir;

            if (accessKeys != null) {
                this.accessKeys.addAll(accessKeys);
            }
        }
    }

    private static class FoundAccountRecord {
        private final String testName;
        private final String account;
        private final String pdfFileName;

        private FoundAccountRecord(
                String testName,
                String account,
                String pdfFileName
        ) {
            this.testName = testName == null
                    ? ""
                    : testName;

            this.account = account == null
                    ? ""
                    : account;

            this.pdfFileName = pdfFileName == null
                    ? ""
                    : pdfFileName;
        }
    }
}
