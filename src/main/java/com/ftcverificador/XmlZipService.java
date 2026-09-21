package com.ftcverificador;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlZipService {

    private static final Pattern KEY_PATTERN =
            Pattern.compile(
                    "(?<!\\d)(\\d{44})(?!\\d)"
            );

    /*
     * Aceita:
     *
     * CT001.01
     * CT 001.01
     * CT 1.1
     */
    private static final Pattern CT_PATTERN =
            Pattern.compile(
                    "(?i)\\bCT\\s*(\\d{1,3})\\s*\\.\\s*(\\d{1,2})\\b"
            );

    /*
     * Segurança para localizar nNF
     * diretamente no texto.
     */
    private static final Pattern NNF_TEXT_PATTERN =
            Pattern.compile(
                    "<(?:[\\w.-]+:)?nNF\\b[^>]*>\\s*(\\d+)\\s*</(?:[\\w.-]+:)?nNF\\s*>",
                    Pattern.CASE_INSENSITIVE
            );

    private final Path zipPath;

    /*
     * Chave de acesso -> XMLs correspondentes.
     */
    private final Map<String, List<XmlReference>>
            xmlByAccessKey =
            new HashMap<>();

    private int xmlCount;

    public XmlZipService(
            Path zipPath
    ) throws IOException {

        if (
                zipPath == null
                        || !Files.isRegularFile(
                                zipPath
                        )
        ) {

            throw new IllegalArgumentException(
                    "Selecione um arquivo ZIP de XMLs válido."
            );
        }

        if (!isZipFile(zipPath)) {

            throw new IllegalArgumentException(
                    "O arquivo de XMLs deve possuir extensão .zip."
            );
        }

        this.zipPath =
                zipPath
                        .toAbsolutePath()
                        .normalize();

        indexXmlFiles();
    }

    /*
     * Varre todos os XMLs do ZIP,
     * inclusive os localizados em subpastas.
     */
    private void indexXmlFiles()
            throws IOException {

        xmlByAccessKey.clear();
        xmlCount = 0;

        try (
                ZipFile zipFile =
                        new ZipFile(
                                zipPath.toFile()
                        )
        ) {

            var entries =
                    zipFile.entries();

            while (entries.hasMoreElements()) {

                ZipEntry entry =
                        entries.nextElement();

                if (
                        entry.isDirectory()
                                || !isXmlEntry(entry)
                ) {
                    continue;
                }

                xmlCount++;

                byte[] xmlBytes;

                try (
                        InputStream input =
                                zipFile.getInputStream(
                                        entry
                                )
                ) {

                    xmlBytes =
                            input.readAllBytes();
                }

                Set<String> keys =
                        extractKeysFromXml(
                                xmlBytes
                        );

                String nfComNumber =
                        extractNfComNumber(
                                xmlBytes
                        );

                for (String key : keys) {

                    xmlByAccessKey
                            .computeIfAbsent(
                                    key,
                                    ignored ->
                                            new ArrayList<>()
                            )
                            .add(
                                    new XmlReference(
                                            entry.getName(),
                                            nfComNumber
                                    )
                            );
                }
            }
        }
    }

    /*
     * Extrai chaves do XML.
     */
    private Set<String> extractKeysFromXml(
            byte[] xmlBytes
    ) {

        Set<String> keys =
                new LinkedHashSet<>();

        /*
         * Primeiro tenta pelo XML estruturado.
         */
        try {

            Document document =
                    parseXml(
                            xmlBytes
                    );

            extractFromChNfCom(
                    document,
                    keys
            );

            extractFromInfNfCom(
                    document,
                    keys
            );

        } catch (Exception ignored) {
            /*
             * A busca textual abaixo
             * ainda será executada.
             */
        }

        /*
         * Fallback:
         * qualquer sequência de 44 dígitos.
         */
        String xmlText =
                new String(
                        xmlBytes,
                        StandardCharsets.UTF_8
                );

        Matcher matcher =
                KEY_PATTERN.matcher(
                        xmlText
                );

        while (matcher.find()) {

            String key =
                    matcher.group(1);

            if (key.length() == 44) {
                keys.add(key);
            }
        }

        return keys;
    }

    /*
     * Obtém o número da NF:
     *
     * <nNF>110000010</nNF>
     *
     * retorna:
     *
     * 110000010
     */
    private String extractNfComNumber(
            byte[] xmlBytes
    ) {

        try {

            Document document =
                    parseXml(
                            xmlBytes
                    );

            NodeList nodes =
                    document.getElementsByTagNameNS(
                            "*",
                            "nNF"
                    );

            if (nodes.getLength() == 0) {
                nodes =
                        document.getElementsByTagName(
                                "nNF"
                        );
            }

            for (
                    int index = 0;
                    index < nodes.getLength();
                    index++
            ) {

                Node node =
                        nodes.item(index);

                if (
                        node == null
                                || node.getTextContent() == null
                ) {
                    continue;
                }

                String value =
                        node
                                .getTextContent()
                                .trim()
                                .replaceAll(
                                        "\\D",
                                        ""
                                );

                if (!value.isEmpty()) {
                    return value;
                }
            }

        } catch (Exception ignored) {
        }

        /*
         * Fallback textual.
         */
        String xmlText =
                new String(
                        xmlBytes,
                        StandardCharsets.UTF_8
                );

        Matcher matcher =
                NNF_TEXT_PATTERN.matcher(
                        xmlText
                );

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    private Document parseXml(
            byte[] xmlBytes
    ) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory
                        .newInstance();

        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);

        /*
         * Proteção contra XXE.
         */
        try {
            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );
        } catch (Exception ignored) {
        }

        try {
            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false
            );
        } catch (Exception ignored) {
        }

        try {
            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
            );
        } catch (Exception ignored) {
        }

        try {
            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    ""
            );
        } catch (Exception ignored) {
        }

        try {
            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    ""
            );
        } catch (Exception ignored) {
        }

        DocumentBuilder builder =
                factory.newDocumentBuilder();

        return builder.parse(
                new ByteArrayInputStream(
                        xmlBytes
                )
        );
    }

    private void extractFromChNfCom(
            Document document,
            Set<String> keys
    ) {

        NodeList nodes =
                document.getElementsByTagNameNS(
                        "*",
                        "chNFCom"
                );

        if (nodes.getLength() == 0) {

            nodes =
                    document.getElementsByTagName(
                            "chNFCom"
                    );
        }

        for (
                int index = 0;
                index < nodes.getLength();
                index++
        ) {

            Node node =
                    nodes.item(index);

            if (node != null) {
                addPossibleKey(
                        node.getTextContent(),
                        keys
                );
            }
        }
    }

    private void extractFromInfNfCom(
            Document document,
            Set<String> keys
    ) {

        NodeList nodes =
                document.getElementsByTagNameNS(
                        "*",
                        "infNFCom"
                );

        if (nodes.getLength() == 0) {

            nodes =
                    document.getElementsByTagName(
                            "infNFCom"
                    );
        }

        for (
                int index = 0;
                index < nodes.getLength();
                index++
        ) {

            Node node =
                    nodes.item(index);

            if (!(node instanceof Element element)) {
                continue;
            }

            String id =
                    element.getAttribute(
                            "Id"
                    );

            addPossibleKey(
                    id,
                    keys
            );
        }
    }

    private void addPossibleKey(
            String value,
            Set<String> keys
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return;
        }

        Matcher matcher =
                KEY_PATTERN.matcher(
                        value
                );

        while (matcher.find()) {

            String key =
                    matcher.group(1);

            if (key.length() == 44) {
                keys.add(key);
            }
        }

        /*
         * Trata:
         *
         * Id="NFCom35123..."
         */
        String digits =
                value.replaceAll(
                        "\\D",
                        ""
                );

        if (digits.length() == 44) {
            keys.add(digits);
        }
    }

    /*
     * Retorna nomes dos XMLs encontrados.
     */
    public List<String> findXmlEntries(
            Set<String> accessKeys
    ) {

        List<XmlReference> references =
                findXmlReferences(
                        accessKeys
                );

        if (references.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> entries =
                new ArrayList<>();

        for (XmlReference reference : references) {

            entries.add(
                    reference.entryName
            );
        }

        return entries;
    }

    /*
     * Localiza XMLs únicos para
     * as chaves do PDF.
     */
    private List<XmlReference> findXmlReferences(
            Set<String> accessKeys
    ) {

        if (
                accessKeys == null
                        || accessKeys.isEmpty()
        ) {
            return Collections.emptyList();
        }

        Map<String, XmlReference> references =
                new LinkedHashMap<>();

        for (String rawKey : accessKeys) {

            if (rawKey == null) {
                continue;
            }

            String key =
                    rawKey.replaceAll(
                            "\\D",
                            ""
                    );

            List<XmlReference> found =
                    xmlByAccessKey.get(
                            key
                    );

            if (found == null) {
                continue;
            }

            for (XmlReference reference : found) {

                references.putIfAbsent(
                        reference.entryName,
                        reference
                );
            }
        }

        return new ArrayList<>(
                references.values()
        );
    }

    /*
     * Copia os XMLs e aplica o NOVO nome:
     *
     * NFCOM+RT27 - FTC -
     * CT001.01 - NF110000010.xml
     */
    public int copyXmlsForKeys(
            Set<String> accessKeys,
            Path targetDir,
            String testName
    ) throws IOException {

        List<XmlReference> references =
                findXmlReferences(
                        accessKeys
                );

        if (references.isEmpty()) {
            return 0;
        }

        String ctLabel =
                extractCtLabel(
                        testName
                );

        Files.createDirectories(
                targetDir
        );

        Set<String> usedNames =
                new LinkedHashSet<>();

        int copied = 0;

        try (
                ZipFile zipFile =
                        new ZipFile(
                                zipPath.toFile()
                        )
        ) {

            for (XmlReference reference : references) {

                ZipEntry entry =
                        zipFile.getEntry(
                                reference.entryName
                        );

                if (entry == null) {
                    continue;
                }

                String nfComNumber =
                        reference.nfComNumber;

                if (
                        nfComNumber == null
                                || nfComNumber.isBlank()
                ) {

                    throw new IllegalStateException(
                            "Não foi possível localizar o nNF no XML: "
                                    + reference.entryName
                    );
                }

                /*
                 * NOVO PADRÃƒO:
                 *
                 * NFCOM+RT27 - FTC -
                 * CT001.01 - NF110000010.xml
                 */
                String targetName =
                        "NFCOM+RT27 - FTC - "
                                + ctLabel
                                + " - NF"
                                + nfComNumber
                                + ".xml";

                /*
                 * Segurança:
                 *
                 * se dois XMLs produzirem
                 * exatamente o mesmo nome,
                 * gera:
                 *
                 * ...NF110000010_2.xml
                 */
                targetName =
                        uniqueFileName(
                                targetName,
                                usedNames
                        );

                Path targetPath =
                        targetDir.resolve(
                                targetName
                        );

                try (
                        InputStream input =
                                zipFile.getInputStream(
                                        entry
                                )
                ) {

                    Files.copy(
                            input,
                            targetPath,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                copied++;
            }
        }

        return copied;
    }

    private String extractCtLabel(
            String testName
    ) {

        if (
                testName == null
                        || testName.isBlank()
        ) {

            throw new IllegalStateException(
                    "Não foi possível identificar o CT: o Test name (initial) está vazio."
            );
        }

        Matcher matcher =
                CT_PATTERN.matcher(
                        testName
                );

        if (!matcher.find()) {

            throw new IllegalStateException(
                    "Não foi possível identificar o CT no cenário: "
                            + testName
            );
        }

        int mainNumber =
                Integer.parseInt(
                        matcher.group(1)
                );

        int subNumber =
                Integer.parseInt(
                        matcher.group(2)
                );

        return String.format(
                Locale.ROOT,
                "CT%03d.%02d",
                mainNumber,
                subNumber
        );
    }

    public boolean containsKey(
            String accessKey
    ) {

        if (
                accessKey == null
                        || accessKey.isBlank()
        ) {
            return false;
        }

        return xmlByAccessKey.containsKey(
                accessKey.replaceAll(
                        "\\D",
                        ""
                )
        );
    }

    public int getXmlCount() {
        return xmlCount;
    }

    public int getIndexedKeyCount() {
        return xmlByAccessKey.size();
    }

    /*
     * Evita sobrescrever dois XMLs
     * com o mesmo nome.
     */
    private String uniqueFileName(
            String originalName,
            Set<String> usedNames
    ) {

        String originalKey =
                originalName.toLowerCase(
                        Locale.ROOT
                );

        if (usedNames.add(originalKey)) {
            return originalName;
        }

        int dotIndex =
                originalName.lastIndexOf('.');

        String base =
                dotIndex > 0
                        ? originalName.substring(
                                0,
                                dotIndex
                        )
                        : originalName;

        String extension =
                dotIndex > 0
                        ? originalName.substring(
                                dotIndex
                        )
                        : "";

        int sequence = 2;

        while (true) {

            String candidate =
                    base
                            + "_"
                            + sequence
                            + extension;

            String candidateKey =
                    candidate.toLowerCase(
                            Locale.ROOT
                    );

            if (usedNames.add(candidateKey)) {
                return candidate;
            }

            sequence++;
        }
    }

    private boolean isXmlEntry(
            ZipEntry entry
    ) {

        return entry
                .getName()
                .toLowerCase(
                        Locale.ROOT
                )
                .endsWith(
                        ".xml"
                );
    }

    public static boolean isZipFile(
            Path path
    ) {

        if (path == null) {
            return false;
        }

        return path
                .getFileName()
                .toString()
                .toLowerCase(
                        Locale.ROOT
                )
                .endsWith(
                        ".zip"
                );
    }

    private static class XmlReference {

        private final String entryName;
        private final String nfComNumber;

        private XmlReference(
                String entryName,
                String nfComNumber
        ) {

            this.entryName =
                    entryName;

            this.nfComNumber =
                    nfComNumber;
        }
    }
}
