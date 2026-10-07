package com.ftcverificador;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PdfService {

    public List<Path> listarArquivosPDF(Path pdfDir) {
        try (Stream<Path> stream = Files.walk(pdfDir)) {
            List<Path> pdfs = stream
                    .filter(Files::isRegularFile)
                    .filter(PdfService::isPdfFile)
                    .sorted(Comparator.comparing(p -> pdfDir.relativize(p).toString()))
                    .collect(Collectors.toList());
            if (pdfs.isEmpty()) {
                throw new IllegalStateException("Nenhum arquivo PDF encontrado em " + pdfDir.toAbsolutePath());
            }
            return pdfs;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao listar arquivos em " + pdfDir.toAbsolutePath(), e);
        }
    }

    public Set<String> extrairConta(Path pdfPath) {
        return extrairDadosConta(pdfPath).getContas();
    }

    public PdfContaDados extrairDadosConta(Path pdfPath) {
        try (PdfExtractor extractor = new PdfExtractor(pdfPath)) {
            return extractor.extrairDadosConta();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void imprimirConteudo(Path pdfPath) {
        try (PdfExtractor extractor = new PdfExtractor(pdfPath)) {
            extractor.printText();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isPdfFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".pdf");
    }

    public static boolean isXlsxFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".xlsx");
    }

    public static boolean isZipFile(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }

        String name = path
                .getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        return name.endsWith(".zip");
    }
}

