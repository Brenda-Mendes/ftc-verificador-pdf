package com.ftcverificador;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PdfInputService implements AutoCloseable {

    private final Path sourcePath;
    private final Path pdfRoot;
    private final boolean temporary;

    private PdfInputService(
            Path sourcePath,
            Path pdfRoot,
            boolean temporary
    ) {
        this.sourcePath = sourcePath;
        this.pdfRoot = pdfRoot;
        this.temporary = temporary;
    }

    public static PdfInputService open(Path inputPath) throws IOException {

        if (inputPath == null) {
            throw new IllegalArgumentException(
                    "A entrada de PDFs não foi informada."
            );
        }

        Path normalizedInput = inputPath
                .toAbsolutePath()
                .normalize();

        if (Files.isDirectory(normalizedInput)) {
            return new PdfInputService(
                    normalizedInput,
                    normalizedInput,
                    false
            );
        }

        if (!Files.isRegularFile(normalizedInput)
                || !PdfService.isZipFile(normalizedInput)) {

            throw new IllegalArgumentException(
                    "Selecione uma pasta de PDFs ou um arquivo ZIP válido."
            );
        }

        Path temporaryDirectory =
                Files.createTempDirectory(
                        "ftc-verificador-pdfs-"
                );

        try {
            extractZip(
                    normalizedInput,
                    temporaryDirectory
            );

            return new PdfInputService(
                    normalizedInput,
                    temporaryDirectory,
                    true
            );

        } catch (Exception exception) {
            deleteRecursively(temporaryDirectory);

            if (exception instanceof IOException ioException) {
                throw ioException;
            }

            throw new IOException(
                    "Não foi possível extrair o ZIP de PDFs.",
                    exception
            );
        }
    }

    private static void extractZip(
            Path zipPath,
            Path destination
    ) throws IOException {

        Path normalizedDestination =
                destination.toAbsolutePath().normalize();

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {

            var entries = zipFile.entries();

            while (entries.hasMoreElements()) {

                ZipEntry entry = entries.nextElement();

                String entryName = entry.getName();

                if (entryName == null || entryName.isBlank()) {
                    continue;
                }

                Path target = normalizedDestination
                        .resolve(entryName)
                        .normalize();

                /*
                 * Proteção contra ZIP Slip.
                 */
                if (!target.startsWith(normalizedDestination)) {
                    throw new IOException(
                            "O ZIP contém um caminho inválido: "
                                    + entryName
                    );
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }

                Path parent = target.getParent();

                if (parent != null) {
                    Files.createDirectories(parent);
                }

                try (InputStream input = zipFile.getInputStream(entry)) {
                    Files.copy(
                            input,
                            target,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }
        }
    }

    public Path getPdfRoot() {
        return pdfRoot;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public boolean isTemporary() {
        return temporary;
    }

    @Override
    public void close() throws IOException {
        if (temporary) {
            deleteRecursively(pdfRoot);
        }
    }

    private static void deleteRecursively(Path directory)
            throws IOException {

        if (directory == null || !Files.exists(directory)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(directory)) {
            List<Path> paths = stream
                    .sorted(Comparator.reverseOrder())
                    .toList();

            IOException failure = null;

            for (Path path : paths) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    if (failure == null) {
                        failure = exception;
                    } else {
                        failure.addSuppressed(exception);
                    }
                }
            }

            if (failure != null) {
                throw failure;
            }
        }
    }
}
