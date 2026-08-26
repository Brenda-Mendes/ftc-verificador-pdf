package com.ftcverificador;

import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfExtractor implements AutoCloseable {

    private static final Pattern NFCOM_PATTERN =
            Pattern.compile(
                    "^\\s*nfcom\\s*n[^0-9]*(\\d+)\\b.*$"
            );

    private static final Pattern TIPO_FINALIDADE_PATTERN =
            Pattern.compile(
                    "^\\s*tipo\\s*(\\d+)\\s*\\|\\s*finalidade\\s*(\\d+)\\b.*$"
            );

    /*
     * Chave NFCom possui 44 dígitos.
     *
     * Também aceita chave exibida no PDF
     * com espaços, pontos ou hífens.
     */
    private static final Pattern ACCESS_KEY_PATTERN =
            Pattern.compile(
                    "(?<!\\d)((?:\\d[\\s.\\-]*){44})(?!\\d)"
            );

    private final PDDocument document;

    public PdfExtractor(Path pdfPath)
            throws IOException {

        this.document =
                Loader.loadPDF(
                        pdfPath.toFile()
                );
    }

    public String extratirTextoPdf()
            throws IOException {

        PDFTextStripper stripper =
                new PDFTextStripper();

        stripper.setLineSeparator(
                System.lineSeparator()
        );

        return stripper.getText(
                document
        );
    }

    public void printText()
            throws IOException {

        System.out.println(
                "== Texto =="
        );

        System.out.println(
                extratirTextoPdf().trim()
        );
    }

    public Set<String> lerNumeroConta()
            throws IOException {

        return extrairDadosConta()
                .getContas();
    }

    public PdfContaDados extrairDadosConta()
            throws IOException {

        String text =
                extratirTextoPdf();

        String[] lines =
                text.split("\\R");

        Set<String> accounts =
                new LinkedHashSet<>();

        Set<String> accessKeys =
                new LinkedHashSet<>();

        String nfCom = "";
        String tipo = "";
        String finalidade = "";

        /*
         * Extração das informações atuais.
         */
        for (
                int i = 0;
                i < lines.length;
                i++
        ) {

            String normalized =
                    normalizeText(
                            lines[i]
                    );

            /*
             * Conta.
             */
            if (
                    normalized.contains(
                            "codigo cliente"
                    )
                            && i < lines.length - 1
            ) {

                String accountLine =
                        lines[i + 1];

                String account =
                        normalizeAccount(
                                accountLine
                        );

                if (!account.isEmpty()) {
                    accounts.add(account);
                }
            }

            /*
             * Número NFCom.
             */
            if (nfCom.isEmpty()) {

                Matcher nfComMatcher =
                        NFCOM_PATTERN.matcher(
                                normalized
                        );

                if (nfComMatcher.matches()) {
                    nfCom =
                            nfComMatcher.group(1);
                }
            }

            /*
             * Tipo e finalidade.
             */
            if (
                    tipo.isEmpty()
                            || finalidade.isEmpty()
            ) {

                Matcher tipoFinalidadeMatcher =
                        TIPO_FINALIDADE_PATTERN
                                .matcher(
                                        normalized
                                );

                if (
                        tipoFinalidadeMatcher
                                .matches()
                ) {

                    tipo =
                            tipoFinalidadeMatcher
                                    .group(1);

                    finalidade =
                            tipoFinalidadeMatcher
                                    .group(2);
                }
            }
        }

        /*
         * Extração das chaves de acesso.
         *
         * Primeiro procura próximo ao texto
         * "Chave de acesso".
         */
        extractAccessKeysNearLabels(
                lines,
                accessKeys
        );

        /*
         * Segunda varredura como segurança.
         *
         * Caso o PDFBox reorganize o texto
         * e a chave fique longe do rótulo,
         * procura qualquer sequência válida
         * de 44 dígitos no documento.
         */
        extractAccessKeys(
                text,
                accessKeys
        );

        return new PdfContaDados(
                accounts,
                accessKeys,
                nfCom,
                tipo,
                finalidade
        );
    }

    private void extractAccessKeysNearLabels(
            String[] lines,
            Set<String> accessKeys
    ) {

        for (
                int i = 0;
                i < lines.length;
                i++
        ) {

            String normalized =
                    normalizeText(
                            lines[i]
                    );

            if (
                    !normalized.contains(
                            "chave de acesso"
                    )
            ) {
                continue;
            }

            /*
             * Analisa:
             *
             * linha atual
             * + próximas 3 linhas
             *
             * porque alguns DANFEs colocam a
             * chave abaixo do título.
             */
            StringBuilder area =
                    new StringBuilder();

            int limit =
                    Math.min(
                            lines.length,
                            i + 4
                    );

            for (
                    int j = i;
                    j < limit;
                    j++
            ) {

                area
                        .append(lines[j])
                        .append(' ');
            }

            extractAccessKeys(
                    area.toString(),
                    accessKeys
            );
        }
    }

    private void extractAccessKeys(
            String text,
            Set<String> accessKeys
    ) {

        if (
                text == null
                        || text.isBlank()
        ) {
            return;
        }

        Matcher matcher =
                ACCESS_KEY_PATTERN.matcher(
                        text
                );

        while (matcher.find()) {

            String key =
                    matcher
                            .group(1)
                            .replaceAll(
                                    "\\D",
                                    ""
                            );

            if (key.length() == 44) {
                accessKeys.add(key);
            }
        }
    }

    private String normalizeText(
            String value
    ) {

        String noAccents =
                Normalizer
                        .normalize(
                                emptyIfNull(value),
                                Normalizer.Form.NFD
                        )
                        .replaceAll(
                                "\\p{M}",
                                ""
                        );

        return noAccents
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private String normalizeAccount(
            String line
    ) {

        String beforeSlash =
                line.split("/", 2)[0];

        return beforeSlash.replaceAll(
                "\\D",
                ""
        );
    }

    private String emptyIfNull(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }

    @Override
    public void close()
            throws IOException {

        document.close();
    }
}