package com.wpw.pim.service.media;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Сервис для извлечения изображений из архивных файлов.
 * <p>
 * Поддерживаемые форматы: ZIP, 7Z, TAR, TAR.GZ, TGZ.
 * Использует Apache Commons Compress для потоковой обработки архивов.
 * Автоматически пропускает системные файлы macOS (__MACOSX, .DS_Store)
 * и скрытые файлы/директории.
 * </p>
 *
 * @see PhotoImportService
 */
@Service
@Slf4j
public class ArchiveExtractorService {

    /** Расширения изображений, которые будут извлечены из архива */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "bmp", "tiff"
    );

    /** Расширения архивных файлов, которые поддерживаются сервисом */
    private static final Set<String> SUPPORTED_ARCHIVE_EXTENSIONS = Set.of(
            "zip", "7z", "tar", "gz", "tgz"
    );

    /**
     * Результат извлечения изображений из архива.
     * Содержит список извлечённых файлов и статистику обработки.
     *
     * @param extractedFiles список извлечённых файлов с оригинальными именами
     * @param totalEntries   общее количество записей в архиве
     * @param imageEntries   количество найденных изображений
     * @param skippedEntries список имён пропущенных файлов (не изображения, скрытые файлы и т.д.)
     */
    public record ExtractionResult(
            List<ExtractedFile> extractedFiles,
            int totalEntries,
            int imageEntries,
            List<String> skippedEntries
    ) {}

    /**
     * Извлечённый из архива файл.
     * Хранит ссылку на временный файл и оригинальное имя из архива.
     *
     * @param tempFile     путь к временному файлу на диске
     * @param originalName оригинальное имя файла из архива (без директорий)
     */
    public record ExtractedFile(
            Path tempFile,
            String originalName
    ) {}

    /**
     * Извлекает все изображения из переданного архивного файла.
     * <p>
     * Метод определяет тип архива по расширению файла, извлекает все записи,
     * фильтрует изображения по расширению и пропускает системные/скрытые файлы.
     * Извлечённые файлы сохраняются во временную директорию.
     * </p>
     * <p>
     * <b>Важно:</b> вызывающий код обязан вызвать {@link #cleanup(ExtractionResult)}
     * после завершения работы с извлечёнными файлами для освобождения дискового пространства.
     * </p>
     *
     * @param archive загруженный архивный файл
     * @return результат извлечения со списком файлов и статистикой
     * @throws IOException если архив повреждён, формат не поддерживается или произошла ошибка I/O
     */
    public ExtractionResult extractImages(MultipartFile archive) throws IOException {
        String filename = archive.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IOException("Имя файла архива не указано");
        }

        String lowerName = filename.toLowerCase();
        ArchiveType type = detectArchiveType(lowerName);

        log.info("Начало извлечения изображений из архива '{}', тип: {}", filename, type);

        Path tempDir = Files.createTempDirectory("archive-extract-");
        try {
            ExtractionResult result = switch (type) {
                case ZIP -> extractZip(archive, tempDir);
                case TAR_GZ -> extractTarGz(archive, tempDir);
                case TAR -> extractTar(archive, tempDir);
                case SEVEN_Z -> extractSevenZ(archive, tempDir);
            };

            log.info("Извлечение завершено: всего записей={}, изображений={}, пропущено={}",
                    result.totalEntries(), result.imageEntries(), result.skippedEntries().size());

            return result;
        } catch (Exception e) {
            // При ошибке чистим временную директорию
            cleanupTempDir(tempDir);
            throw e;
        }
    }

    /**
     * Проверяет, является ли файл поддерживаемым архивом по расширению.
     *
     * @param filename имя файла для проверки
     * @return {@code true} если расширение соответствует поддерживаемому формату архива
     */
    public boolean isSupportedArchive(String filename) {
        if (filename == null || filename.isBlank()) return false;
        String lower = filename.toLowerCase();
        // Специальная обработка .tar.gz
        if (lower.endsWith(".tar.gz")) return true;
        String ext = getExtension(lower);
        return SUPPORTED_ARCHIVE_EXTENSIONS.contains(ext);
    }

    /**
     * Удаляет все временные файлы, созданные в процессе извлечения.
     * Безопасен для повторного вызова - игнорирует уже удалённые файлы.
     *
     * @param result результат извлечения, файлы которого нужно удалить
     */
    public void cleanup(ExtractionResult result) {
        if (result == null || result.extractedFiles() == null) return;

        Set<Path> parentDirs = new HashSet<>();
        for (ExtractedFile ef : result.extractedFiles()) {
            try {
                if (ef.tempFile() != null) {
                    parentDirs.add(ef.tempFile().getParent());
                    Files.deleteIfExists(ef.tempFile());
                }
            } catch (IOException e) {
                log.warn("Не удалось удалить временный файл: {}", ef.tempFile(), e);
            }
        }

        // Удаляем временные директории
        for (Path dir : parentDirs) {
            cleanupTempDir(dir);
        }
    }

    /**
     * Результат сканирования архива без извлечения файлов.
     */
    public record ScanResult(
            List<String> imageFileNames,
            int totalEntries,
            int imageEntries,
            List<String> skippedEntries
    ) {}

    /**
     * Сканирует архив и возвращает список имён файлов-изображений БЕЗ извлечения на диск.
     * Быстрая операция — читает только заголовки записей.
     */
    public ScanResult scanImageNames(MultipartFile archive) throws IOException {
        String filename = archive.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IOException("Имя файла архива не указано");
        }

        String lowerName = filename.toLowerCase();
        ArchiveType type = detectArchiveType(lowerName);

        log.info("Сканирование архива '{}', тип: {}", filename, type);

        if (type == ArchiveType.SEVEN_Z) {
            return scanSevenZ(archive);
        }

        try (InputStream is = archive.getInputStream()) {
            ArchiveInputStream<?> ais = switch (type) {
                case ZIP -> new ZipArchiveInputStream(is);
                case TAR -> new TarArchiveInputStream(is);
                case TAR_GZ -> new TarArchiveInputStream(new GzipCompressorInputStream(is));
                default -> throw new IOException("Unexpected type: " + type);
            };
            try (ais) {
                return scanFromStream(ais);
            }
        }
    }

    private ScanResult scanFromStream(ArchiveInputStream<?> ais) throws IOException {
        List<String> imageNames = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        int totalEntries = 0;

        ArchiveEntry entry;
        while ((entry = ais.getNextEntry()) != null) {
            if (entry.isDirectory()) continue;
            totalEntries++;

            String entryName = entry.getName();
            String fileName = extractFileName(entryName);

            if (shouldSkipEntry(entryName, fileName)) {
                skipped.add(entryName);
                continue;
            }

            String ext = getExtension(fileName.toLowerCase());
            if (!IMAGE_EXTENSIONS.contains(ext)) {
                skipped.add(entryName);
                continue;
            }

            imageNames.add(fileName);
        }

        return new ScanResult(imageNames, totalEntries, imageNames.size(), skipped);
    }

    private ScanResult scanSevenZ(MultipartFile archive) throws IOException {
        Path tempArchive = Files.createTempFile("archive-7z-scan-", ".7z");
        try {
            archive.transferTo(tempArchive.toFile());
            List<String> imageNames = new ArrayList<>();
            List<String> skipped = new ArrayList<>();
            int totalEntries = 0;

            try (SevenZFile sevenZFile = SevenZFile.builder()
                    .setFile(tempArchive.toFile())
                    .get()) {
                SevenZArchiveEntry entry;
                while ((entry = sevenZFile.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    totalEntries++;

                    String entryName = entry.getName();
                    String fileName = extractFileName(entryName);

                    if (shouldSkipEntry(entryName, fileName)) {
                        skipped.add(entryName);
                        continue;
                    }

                    String ext = getExtension(fileName.toLowerCase());
                    if (!IMAGE_EXTENSIONS.contains(ext)) {
                        skipped.add(entryName);
                        continue;
                    }

                    imageNames.add(fileName);
                }
            }

            return new ScanResult(imageNames, totalEntries, imageNames.size(), skipped);
        } finally {
            Files.deleteIfExists(tempArchive);
        }
    }

    // ========================= Внутренние методы =========================

    /**
     * Перечисление поддерживаемых типов архивов.
     */
    private enum ArchiveType {
        ZIP, TAR_GZ, TAR, SEVEN_Z
    }

    /**
     * Определяет тип архива по расширению имени файла.
     *
     * @param lowerName имя файла в нижнем регистре
     * @return определённый тип архива
     * @throws IOException если формат не поддерживается (например, RAR)
     */
    private ArchiveType detectArchiveType(String lowerName) throws IOException {
        if (lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz")) {
            return ArchiveType.TAR_GZ;
        }
        String ext = getExtension(lowerName);
        return switch (ext) {
            case "zip" -> ArchiveType.ZIP;
            case "tar" -> ArchiveType.TAR;
            case "7z" -> ArchiveType.SEVEN_Z;
            case "rar" -> throw new IOException(
                    "Формат RAR не поддерживается. Пожалуйста, используйте ZIP, 7Z или TAR.GZ.");
            default -> throw new IOException(
                    "Неподдерживаемый формат архива: ." + ext
                            + ". Поддерживаются: ZIP, 7Z, TAR, TAR.GZ, TGZ.");
        };
    }

    /**
     * Извлекает изображения из ZIP-архива с помощью потоковой обработки.
     */
    private ExtractionResult extractZip(MultipartFile archive, Path tempDir) throws IOException {
        try (InputStream is = archive.getInputStream();
             ZipArchiveInputStream zis = new ZipArchiveInputStream(is)) {
            return extractFromStream(zis, tempDir);
        }
    }

    /**
     * Извлекает изображения из TAR-архива.
     */
    private ExtractionResult extractTar(MultipartFile archive, Path tempDir) throws IOException {
        try (InputStream is = archive.getInputStream();
             TarArchiveInputStream tis = new TarArchiveInputStream(is)) {
            return extractFromStream(tis, tempDir);
        }
    }

    /**
     * Извлекает изображения из TAR.GZ (GZIP-сжатый TAR) архива.
     */
    private ExtractionResult extractTarGz(MultipartFile archive, Path tempDir) throws IOException {
        try (InputStream is = archive.getInputStream();
             GzipCompressorInputStream gis = new GzipCompressorInputStream(is);
             TarArchiveInputStream tis = new TarArchiveInputStream(gis)) {
            return extractFromStream(tis, tempDir);
        }
    }

    /**
     * Извлекает изображения из 7Z-архива.
     * <p>
     * 7Z не поддерживает потоковое чтение, поэтому архив сначала
     * сохраняется во временный файл, а затем обрабатывается через {@link SevenZFile}.
     * </p>
     */
    private ExtractionResult extractSevenZ(MultipartFile archive, Path tempDir) throws IOException {
        // 7Z требует RandomAccessFile, поэтому сохраняем во временный файл
        Path tempArchive = Files.createTempFile("archive-7z-", ".7z");
        try {
            archive.transferTo(tempArchive.toFile());

            List<ExtractedFile> extracted = new ArrayList<>();
            List<String> skipped = new ArrayList<>();
            int totalEntries = 0;

            try (SevenZFile sevenZFile = SevenZFile.builder()
                    .setFile(tempArchive.toFile())
                    .get()) {

                SevenZArchiveEntry entry;
                while ((entry = sevenZFile.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    totalEntries++;

                    String entryName = entry.getName();
                    String fileName = extractFileName(entryName);

                    if (shouldSkipEntry(entryName, fileName)) {
                        skipped.add(entryName);
                        continue;
                    }

                    String ext = getExtension(fileName.toLowerCase());
                    if (!IMAGE_EXTENSIONS.contains(ext)) {
                        skipped.add(entryName);
                        continue;
                    }

                    // Извлекаем файл во временную директорию
                    Path tempFile = tempDir.resolve(UUID.randomUUID() + "." + ext);
                    try (OutputStream os = Files.newOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        InputStream entryStream = sevenZFile.getInputStream(entry);
                        while ((len = entryStream.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                    extracted.add(new ExtractedFile(tempFile, fileName));
                }
            }

            return new ExtractionResult(extracted, totalEntries, extracted.size(), skipped);
        } finally {
            Files.deleteIfExists(tempArchive);
        }
    }

    /**
     * Универсальный метод извлечения изображений из потокового архива (ZIP, TAR, TAR.GZ).
     * Читает записи последовательно, фильтрует по типу и сохраняет во временные файлы.
     *
     * @param ais     входной поток архива
     * @param tempDir директория для временных файлов
     * @return результат извлечения
     * @throws IOException при ошибке чтения архива или записи файлов
     */
    private ExtractionResult extractFromStream(ArchiveInputStream<?> ais, Path tempDir) throws IOException {
        List<ExtractedFile> extracted = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        int totalEntries = 0;

        ArchiveEntry entry;
        while ((entry = ais.getNextEntry()) != null) {
            if (entry.isDirectory()) continue;
            totalEntries++;

            String entryName = entry.getName();
            String fileName = extractFileName(entryName);

            if (shouldSkipEntry(entryName, fileName)) {
                skipped.add(entryName);
                continue;
            }

            String ext = getExtension(fileName.toLowerCase());
            if (!IMAGE_EXTENSIONS.contains(ext)) {
                skipped.add(entryName);
                continue;
            }

            // Извлекаем файл во временную директорию
            Path tempFile = tempDir.resolve(UUID.randomUUID() + "." + ext);
            try (OutputStream os = Files.newOutputStream(tempFile)) {
                ais.transferTo(os);
            }
            extracted.add(new ExtractedFile(tempFile, fileName));
        }

        return new ExtractionResult(extracted, totalEntries, extracted.size(), skipped);
    }

    /**
     * Извлекает имя файла из полного пути записи архива.
     * Обрабатывает как Unix-разделители (/), так и Windows-разделители (\).
     *
     * @param entryName полный путь записи в архиве
     * @return имя файла без директорий
     */
    private String extractFileName(String entryName) {
        // Обработка путей с разными разделителями
        String name = entryName.replace('\\', '/');
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
    }

    /**
     * Проверяет, нужно ли пропустить запись архива.
     * Пропускаются: macOS-мусор (__MACOSX, .DS_Store), скрытые файлы (начинающиеся с точки),
     * файлы Thumbs.db и пустые имена.
     *
     * @param entryName полный путь записи
     * @param fileName  имя файла без директорий
     * @return {@code true} если запись следует пропустить
     */
    private boolean shouldSkipEntry(String entryName, String fileName) {
        if (fileName.isBlank()) return true;

        // Пропускаем macOS-мусор
        if (entryName.contains("__MACOSX")) return true;
        if (fileName.equals(".DS_Store")) return true;

        // Пропускаем скрытые файлы
        if (fileName.startsWith(".")) return true;

        // Пропускаем Windows-мусор
        if (fileName.equals("Thumbs.db")) return true;

        return false;
    }

    /**
     * Извлекает расширение файла (без точки).
     */
    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1) : "";
    }

    /**
     * Рекурсивно удаляет временную директорию и всё её содержимое.
     */
    private void cleanupTempDir(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Не удалось удалить: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Не удалось очистить временную директорию: {}", dir, e);
        }
    }
}
