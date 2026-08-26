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
     * Exemplo:
     *
     * CT001.02
     *
     * Retorna:
     *
     * 001.02
     */
    private static final Pattern CT_PATTERN =
            Pattern.compile(
                    "(?i)\\bCT\\s*(\\d{3}\\.\\d{2})\\b"
            );

    /*
     * Segurança para localizar nNF diretamente
     * no texto do XML caso o parser DOM falhe.
     */
    private static final Pattern NNF_TEXT_PATTERN =
            Pattern.compile(
                    "<(?:[\\w.-]+:)?nNF\\b[^>]*>\\s*(\\d+)\\s*</(?:[\\w.-]+:)?nNF\\s*>",
                    Pattern.CASE_INSENSITIVE
            );

    private final Path zipPath;

    /*
     * chave de acesso -> XMLs encontrados.
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
     * Varre todos os XMLs existentes dentro
     * do ZIP, inclusive em subpastas.
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

            while (
                    entries.hasMoreElements()
            ) {

                ZipEntry entry =
                        entries.nextElement();

                if (
                        entry.isDirectory()
                                || !isXmlEntry(
                                        entry
                                )
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

                /*
                 * Localiza as chaves desse XML.
                 */
                Set<String> keys =
                        extractKeysFromXml(
                                xmlBytes
                        );

                /*
                 * NOVO:
                 * Pega o número da NFCom.
                 *
                 * Exemplo:
                 *
                 * <nNF>110000008</nNF>
                 *
                 * retorna:
                 *
                 * 110000008
                 */
                String nfComNumber =
                        extractNfComNumber(
                                xmlBytes
                        );

                for (
                        String key :
                        keys
                ) {

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
     * Extrai as chaves existentes no XML.
     */
    private Set<String> extractKeysFromXml(
            byte[] xmlBytes
    ) {

        Set<String> keys =
                new LinkedHashSet<>();

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
             * Se houver alguma estrutura inesperada,
             * a varredura textual abaixo ainda será
             * executada.
             */
        }

        String xmlText =
                new String(
                        xmlBytes,
                        StandardCharsets.UTF_8
                );

        Matcher matcher =
                KEY_PATTERN.matcher(
                        xmlText
                );

        while (
                matcher.find()
        ) {

            String key =
                    matcher.group(1);

            if (
                    key.length()
                            == 44
            ) {

                keys.add(
                        key
                );
            }
        }

        return keys;
    }

    /*
     * NOVO:
     * Extrai o nNF do XML.
     */
    private String extractNfComNumber(
            byte[] xmlBytes
    ) {

        /*
         * Primeiro tenta estruturalmente.
         */
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

            if (
                    nodes.getLength()
                            == 0
            ) {

                nodes =
                        document.getElementsByTagName(
                                "nNF"
                        );
            }

            for (
                    int i = 0;
                    i < nodes.getLength();
                    i++
            ) {

                Node node =
                        nodes.item(i);

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

                if (
                        !value.isEmpty()
                ) {

                    return value;
                }
            }

        } catch (
                Exception ignored
        ) {
        }

        /*
         * Segunda tentativa:
         * procura diretamente no texto.
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

        if (
                matcher.find()
        ) {

            return matcher.group(1);
        }

        return "";
    }

    /*
     * Parser XML com proteção contra
     * entidades externas.
     */
    private Document parseXml(
            byte[] xmlBytes
    ) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory
                        .newInstance();

        factory.setNamespaceAware(
                true
        );

        try {

            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );

        } catch (
                Exception ignored
        ) {
        }

        try {

            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false
            );

        } catch (
                Exception ignored
        ) {
        }

        try {

            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
            );

        } catch (
                Exception ignored
        ) {
        }

        factory.setExpandEntityReferences(
                false
        );

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

        if (
                nodes.getLength()
                        == 0
        ) {

            nodes =
                    document.getElementsByTagName(
                            "chNFCom"
                    );
        }

        for (
                int i = 0;
                i < nodes.getLength();
                i++
        ) {

            Node node =
                    nodes.item(i);

            addPossibleKey(
                    node.getTextContent(),
                    keys
            );
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

        if (
                nodes.getLength()
                        == 0
        ) {

            nodes =
                    document.getElementsByTagName(
                            "infNFCom"
                    );
        }

        for (
                int i = 0;
                i < nodes.getLength();
                i++
        ) {

            Node node =
                    nodes.item(i);

            if (
                    !(node instanceof Element element)
            ) {

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

        while (
                matcher.find()
        ) {

            String key =
                    matcher.group(1);

            if (
                    key.length()
                            == 44
            ) {

                keys.add(
                        key
                );
            }
        }

        String digits =
                value.replaceAll(
                        "\\D",
                        ""
                );

        if (
                digits.length()
                        == 44
        ) {

            keys.add(
                    digits
            );
        }
    }

    /*
     * Mantido para consultas.
     */
    public List<String> findXmlEntries(
            Set<String> accessKeys
    ) {

        List<XmlReference> references =
                findXmlReferences(
                        accessKeys
                );

        if (
                references.isEmpty()
        ) {

            return Collections.emptyList();
        }

        List<String> entries =
                new ArrayList<>();

        for (
                XmlReference reference :
                references
        ) {

            entries.add(
                    reference.entryName
            );
        }

        return entries;
    }

    /*
     * Localiza os XMLs correspondentes
     * às chaves daquele PDF.
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

        /*
         * entryName -> referência
         *
         * Impede copiar o mesmo XML duas vezes.
         */
        Map<String, XmlReference> references =
                new LinkedHashMap<>();

        for (
                String key :
                accessKeys
        ) {

            List<XmlReference> found =
                    xmlByAccessKey.get(
                            key
                    );

            if (
                    found == null
            ) {

                continue;
            }

            for (
                    XmlReference reference :
                    found
            ) {

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
     * NOVA VERSÃO.
     *
     * Recebe também o Test name (initial)
     * para extrair o CT.
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

        if (
                references.isEmpty()
        ) {

            return 0;
        }

        /*
         * Exemplo:
         *
         * "NFCOM+RT27 - FAT - FTC - CT001.02"
         *
         * retorna:
         *
         * 001.02
         */
        String ctNumber =
                extractCtNumber(
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

            for (
                    XmlReference reference :
                    references
            ) {

                ZipEntry entry =
                        zipFile.getEntry(
                                reference.entryName
                        );

                if (
                        entry == null
                ) {

                    continue;
                }

                String nfComNumber =
                        reference.nfComNumber;

                /*
                 * Um XML NFCom válido deve possuir nNF.
                 */
                if (
                        nfComNumber == null
                                || nfComNumber.isBlank()
                ) {

                    throw new IllegalStateException(
                            "Não foi possível localizar o número da NFCom (nNF) no XML: "
                                    + reference.entryName
                    );
                }

                /*
                 * Nome solicitado:
                 *
                 * NFCOM+RT27 - FAT - FTC - CT001.02
                 * - XML - NF110000008.xml
                 */
                String targetName =
                        "NFCOM+RT27 - FAT - FTC - CT"
                                + ctNumber
                                + " - XML - NF"
                                + nfComNumber
                                + ".xml";

                /*
                 * Caso haja excepcionalmente dois XMLs
                 * que gerem exatamente o mesmo nome,
                 * não sobrescreve o primeiro.
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

    /*
     * Extrai somente:
     *
     * 001.02
     *
     * de:
     *
     * CT001.02
     */
    private String extractCtNumber(
            String testName
    ) {

        if (
                testName == null
                        || testName.isBlank()
        ) {

            throw new IllegalStateException(
                    "Não foi possível identificar o CT porque o Test name (initial) está vazio."
            );
        }

        Matcher matcher =
                CT_PATTERN.matcher(
                        testName
                );

        if (
                matcher.find()
        ) {

            return matcher.group(1);
        }

        throw new IllegalStateException(
                "Não foi possível identificar o CT no cenário: "
                        + testName
                        + ". Esperado um valor como CT001.02."
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

    private String uniqueFileName(
            String originalName,
            Set<String> usedNames
    ) {

        if (
                usedNames.add(
                        originalName.toLowerCase(
                                Locale.ROOT
                        )
                )
        ) {

            return originalName;
        }

        int dotIndex =
                originalName.lastIndexOf(
                        '.'
                );

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

        while (
                true
        ) {

            String candidate =
                    base
                            + "_"
                            + sequence
                            + extension;

            String candidateKey =
                    candidate.toLowerCase(
                            Locale.ROOT
                    );

            if (
                    usedNames.add(
                            candidateKey
                    )
            ) {

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

        if (
                path == null
        ) {

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

    /*
     * Agora guardamos:
     *
     * - caminho original no ZIP;
     * - número NFCom daquele XML.
     */
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