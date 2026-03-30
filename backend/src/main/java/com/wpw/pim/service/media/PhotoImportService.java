package com.wpw.pim.service.media;

import com.wpw.pim.domain.enums.FileType;
import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.service.media.ArchiveExtractorService.ScanResult;
import com.wpw.pim.service.media.ArchiveExtractorService.StreamResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoImportService {

    private final ProductRepository productRepository;
    private final MediaFileRepository mediaFileRepository;
    private final ArchiveExtractorService archiveExtractorService;

    @Value("${pim.media.base-path:/media/products}")
    private String mediaBasePath;

    @Value("${pim.media.base-url:/media/products}")
    private String mediaBaseUrl;

    private static final Pattern TOOL_NO_PATTERN = Pattern.compile("^(.+?)(?:_\\d+)?\\.[a-zA-Z]+$");

    @PostConstruct
    void ensureMediaDirectory() {
        Path mediaDir = Paths.get(mediaBasePath);
        if (!Files.exists(mediaDir)) {
            try {
                Files.createDirectories(mediaDir);
                log.info("Created media directory: {}", mediaDir);
            } catch (IOException e) {
                log.warn("Could not create media directory '{}': {}. Create it manually and ensure the app has write access.", mediaDir, e.getMessage());
            }
        }
    }

    /**
     * Validate uploaded photos - match filenames to products, return report without importing.
     */
    public Map<String, Object> validatePhotos(MultipartFile[] files) {
        Map<String, Product> productsByToolNo = getProductMap();

        List<Map<String, Object>> matched = new ArrayList<>();
        List<Map<String, Object>> unmatched = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) continue;

            String toolNo = extractToolNo(originalName);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("filename", originalName);
            entry.put("toolNo", toolNo);
            entry.put("size", file.getSize());

            Product product = productsByToolNo.get(toolNo.toUpperCase());
            if (product != null) {
                entry.put("productName", toolNo);
                entry.put("productId", product.getId().toString());
                matched.add(entry);
            } else {
                unmatched.add(entry);
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalFiles", files.length);
        report.put("matched", matched.size());
        report.put("unmatched", unmatched.size());
        report.put("matchedFiles", matched);
        report.put("unmatchedFiles", unmatched);
        return report;
    }

    /**
     * Import uploaded photos - convert to WebP, save to disk, create MediaFile records.
     */
    @Transactional
    public Map<String, Object> importPhotos(MultipartFile[] files) throws IOException {
        Map<String, Product> productsByToolNo = getProductMap();
        Path mediaDir = Paths.get(mediaBasePath);
        Files.createDirectories(mediaDir);

        int matched = 0;
        int converted = 0;
        int skipped = 0;
        int errors = 0;
        List<String> errorDetails = new ArrayList<>();

        // Get existing URLs to avoid duplicates
        Set<String> existingUrls = mediaFileRepository.findAll().stream()
            .map(MediaFile::getUrl)
            .collect(Collectors.toSet());

        // Group files by tool number
        Map<String, List<MultipartFile>> filesByTool = new LinkedHashMap<>();
        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) continue;
            String toolNo = extractToolNo(originalName).toUpperCase();
            filesByTool.computeIfAbsent(toolNo, k -> new ArrayList<>()).add(file);
        }

        for (Map.Entry<String, List<MultipartFile>> entry : filesByTool.entrySet()) {
            String toolNo = entry.getKey();
            List<MultipartFile> toolFiles = entry.getValue();
            Product product = productsByToolNo.get(toolNo);

            if (product == null) {
                skipped += toolFiles.size();
                continue;
            }

            matched++;
            Path toolDir = mediaDir.resolve(toolNo);
            Files.createDirectories(toolDir);

            // Find next available variant number
            int nextVariant = getNextVariant(toolDir);

            for (MultipartFile file : toolFiles) {
                String ext = getExtension(file.getOriginalFilename()).toLowerCase();
                if (!isImageFile(ext)) {
                    skipped++;
                    continue;
                }

                try {
                    String webpName = nextVariant + ".webp";
                    Path webpPath = toolDir.resolve(webpName);
                    String url = mediaBaseUrl + "/" + toolNo + "/" + webpName;

                    if (existingUrls.contains(url)) {
                        nextVariant++;
                        webpName = nextVariant + ".webp";
                        webpPath = toolDir.resolve(webpName);
                        url = mediaBaseUrl + "/" + toolNo + "/" + webpName;
                    }

                    // Convert to WebP
                    convertToWebp(file, webpPath);

                    // Create MediaFile record
                    MediaFile mf = new MediaFile();
                    mf.setProduct(product);
                    mf.setFileType(FileType.image);
                    mf.setUrl(url);
                    mf.setThumbnailUrl(url);
                    mf.setAltText(product.getToolNo() + " image " + nextVariant);
                    mf.setSortOrder(nextVariant - 1);
                    mediaFileRepository.save(mf);

                    existingUrls.add(url);
                    converted++;
                    nextVariant++;
                } catch (Exception e) {
                    errors++;
                    errorDetails.add(file.getOriginalFilename() + ": " + e.getMessage());
                    log.error("Failed to convert {}: {}", file.getOriginalFilename(), e.getMessage());
                }
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("matchedProducts", matched);
        report.put("converted", converted);
        report.put("skipped", skipped);
        report.put("errors", errors);
        if (!errorDetails.isEmpty()) {
            report.put("errorDetails", errorDetails);
        }
        return report;
    }

    /**
     * Sync existing photos on disk (for photos already converted by script).
     */
    @Transactional
    public Map<String, Object> syncExistingPhotos() throws IOException {
        Path mediaDir = Paths.get(mediaBasePath);
        if (!Files.isDirectory(mediaDir)) {
            throw new IOException("Media directory not found: " + mediaBasePath);
        }

        Map<String, Product> productsByToolNo = getProductMap();
        Set<String> existingUrls = mediaFileRepository.findAll().stream()
            .map(MediaFile::getUrl).collect(Collectors.toSet());

        int matched = 0, unmatched = 0, created = 0, skipped = 0;

        try (Stream<Path> toolDirs = Files.list(mediaDir)) {
            for (Path toolDir : toolDirs.filter(Files::isDirectory).toList()) {
                String toolNo = toolDir.getFileName().toString().toUpperCase();
                Product product = productsByToolNo.get(toolNo);

                if (product == null) { unmatched++; continue; }
                matched++;

                List<Path> images;
                try (Stream<Path> fileStream = Files.list(toolDir)) {
                    images = fileStream.filter(f -> f.toString().endsWith(".webp"))
                        .sorted(Comparator.comparing(f -> {
                            try { return Integer.parseInt(f.getFileName().toString().replace(".webp", "")); }
                            catch (NumberFormatException e) { return 999; }
                        })).toList();
                }

                for (int i = 0; i < images.size(); i++) {
                    String url = mediaBaseUrl + "/" + toolDir.getFileName() + "/" + images.get(i).getFileName();
                    if (existingUrls.contains(url)) { skipped++; continue; }

                    MediaFile mf = new MediaFile();
                    mf.setProduct(product);
                    mf.setFileType(FileType.image);
                    mf.setUrl(url);
                    mf.setThumbnailUrl(url);
                    mf.setAltText(product.getToolNo() + " image " + (i + 1));
                    mf.setSortOrder(i);
                    mediaFileRepository.save(mf);
                    created++;
                }
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("matched", matched);
        report.put("unmatched", unmatched);
        report.put("created", created);
        report.put("skipped", skipped);
        return report;
    }

    /**
     * Валидация архива - извлекает изображения, сопоставляет с продуктами, возвращает отчёт без импорта.
     * <p>
     * Поддерживаемые форматы архивов: ZIP, 7Z, TAR, TAR.GZ, TGZ.
     * Метод извлекает все изображения из архива во временные файлы,
     * сопоставляет их с продуктами по номеру инструмента (toolNo),
     * формирует отчёт и очищает временные файлы.
     * </p>
     *
     * @param archive загруженный архивный файл
     * @return отчёт о валидации с информацией о содержимом архива и сопоставлении с продуктами
     * @throws IOException при ошибке чтения архива
     */
    public Map<String, Object> validateArchive(MultipartFile archive) throws IOException {
        ScanResult scan = archiveExtractorService.scanImageNames(archive);

        Map<String, Product> productsByToolNo = getProductMap();
        List<Map<String, Object>> matched = new ArrayList<>();
        List<Map<String, Object>> unmatched = new ArrayList<>();

        for (String originalName : scan.imageFileNames()) {
            String toolNo = extractToolNo(originalName);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("filename", originalName);
            entry.put("toolNo", toolNo);

            Product product = productsByToolNo.get(toolNo.toUpperCase());
            if (product != null) {
                entry.put("productName", toolNo);
                entry.put("productId", product.getId().toString());
                matched.add(entry);
            } else {
                unmatched.add(entry);
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("archiveName", archive.getOriginalFilename());
        report.put("archiveSize", archive.getSize());
        report.put("totalEntriesInArchive", scan.totalEntries());
        report.put("imagesExtracted", scan.imageEntries());
        report.put("skippedFiles", scan.skippedEntries().size());
        report.put("matched", matched.size());
        report.put("unmatched", unmatched.size());
        report.put("matchedFiles", matched);
        report.put("unmatchedFiles", unmatched);
        if (!scan.skippedEntries().isEmpty()) {
            report.put("skippedFileNames", scan.skippedEntries());
        }
        return report;
    }

    /**
     * Импорт фотографий из архива — потоковая обработка.
     * Файлы извлекаются по одному, конвертируются в WebP, temp удаляется сразу.
     * БД-записи сохраняются пачками по BATCH_SIZE.
     */
    private static final int BATCH_SIZE = 50;

    @Transactional
    public Map<String, Object> importArchive(MultipartFile archive) throws IOException {
        Map<String, Product> productsByToolNo = getProductMap();
        Path mediaDir = Paths.get(mediaBasePath);
        Files.createDirectories(mediaDir);

        // Собираем ID matched-продуктов для точечного запроса дубликатов
        Set<UUID> matchedProductIds = productsByToolNo.values().stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
        Set<String> existingUrls = matchedProductIds.isEmpty()
                ? new HashSet<>()
                : mediaFileRepository.findUrlsByProductIds(matchedProductIds);

        // Счётчики — используем массивы для доступа из лямбды
        int[] converted = {0};
        int[] skipped = {0};
        int[] errors = {0};
        Set<String> matchedToolNos = new HashSet<>();
        List<String> errorDetails = new ArrayList<>();
        List<MediaFile> batch = new ArrayList<>();

        // Кэш nextVariant по toolNo — чтобы не сканировать директорию повторно
        Map<String, Integer> variantCache = new HashMap<>();

        StreamResult streamResult = archiveExtractorService.processImages(archive, (tempFile, originalName) -> {
            String toolNo = extractToolNo(originalName).toUpperCase();
            Product product = productsByToolNo.get(toolNo);

            if (product == null) {
                skipped[0]++;
                return;
            }

            matchedToolNos.add(toolNo);

            try {
                Path toolDir = mediaDir.resolve(toolNo);
                Files.createDirectories(toolDir);

                int nextVariant = variantCache.computeIfAbsent(toolNo, k -> {
                    try { return getNextVariant(toolDir); }
                    catch (IOException e) { return 1; }
                });

                String webpName = nextVariant + ".webp";
                String url = mediaBaseUrl + "/" + toolNo + "/" + webpName;

                while (existingUrls.contains(url)) {
                    nextVariant++;
                    webpName = nextVariant + ".webp";
                    url = mediaBaseUrl + "/" + toolNo + "/" + webpName;
                }

                Path webpPath = toolDir.resolve(webpName);
                convertTempFileToWebp(tempFile, originalName, webpPath);

                MediaFile mf = new MediaFile();
                mf.setProduct(product);
                mf.setFileType(FileType.image);
                mf.setUrl(url);
                mf.setThumbnailUrl(url);
                mf.setAltText(product.getToolNo() + " image " + nextVariant);
                mf.setSortOrder(nextVariant - 1);
                batch.add(mf);

                existingUrls.add(url);
                variantCache.put(toolNo, nextVariant + 1);
                converted[0]++;

                if (batch.size() >= BATCH_SIZE) {
                    mediaFileRepository.saveAll(batch);
                    batch.clear();
                }
            } catch (Exception e) {
                errors[0]++;
                errorDetails.add(originalName + ": " + e.getMessage());
                log.error("Ошибка конвертации {}: {}", originalName, e.getMessage());
            }
        });

        // Сохраняем остаток
        if (!batch.isEmpty()) {
            mediaFileRepository.saveAll(batch);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("archiveName", archive.getOriginalFilename());
        report.put("totalEntriesInArchive", streamResult.totalEntries());
        report.put("imagesExtracted", streamResult.imageEntries());
        report.put("skippedInArchive", streamResult.skippedEntries());
        report.put("matchedProducts", matchedToolNos.size());
        report.put("converted", converted[0]);
        report.put("skipped", skipped[0]);
        report.put("errors", errors[0]);
        if (!errorDetails.isEmpty()) {
            report.put("errorDetails", errorDetails);
        }
        return report;
    }

    /**
     * Конвертирует временный файл изображения в WebP формат.
     * Если файл уже в формате WebP, просто копирует его.
     *
     * @param tempFile     путь к временному файлу изображения
     * @param originalName оригинальное имя файла (для определения формата)
     * @param output       путь для сохранения результата в WebP
     * @throws IOException при ошибке конвертации
     */
    private void convertTempFileToWebp(Path tempFile, String originalName, Path output) throws IOException {
        // Если уже WebP, просто копируем
        if (originalName != null && originalName.toLowerCase().endsWith(".webp")) {
            Files.copy(tempFile, output, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        // Конвертируем через cwebp
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cwebp", "-q", "80", "-quiet", tempFile.toString(), "-o", output.toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String procOutput = new String(proc.getInputStream().readAllBytes());
            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                throw new IOException("cwebp failed (exit " + exitCode + "): " + procOutput);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Конвертация прервана", e);
        }
    }

    private Map<String, Product> getProductMap() {
        return productRepository.findAll().stream()
            .collect(Collectors.toMap(p -> p.getToolNo().toUpperCase(), p -> p, (a, b) -> a));
    }

    private String extractToolNo(String filename) {
        Matcher m = TOOL_NO_PATTERN.matcher(filename);
        if (m.matches()) return m.group(1);
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1) : "";
    }

    private boolean isImageFile(String ext) {
        return Set.of("jpg", "jpeg", "png", "webp", "bmp", "tiff").contains(ext);
    }

    private int getNextVariant(Path toolDir) throws IOException {
        int max = 0;
        try (Stream<Path> files = Files.list(toolDir)) {
            for (Path f : files.toList()) {
                String name = f.getFileName().toString().replace(".webp", "");
                try {
                    int n = Integer.parseInt(name);
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) {}
            }
        }
        return max + 1;
    }

    private void convertToWebp(MultipartFile file, Path output) throws IOException {
        // Save temp file
        Path temp = Files.createTempFile("photo-", "." + getExtension(file.getOriginalFilename()));
        try {
            file.transferTo(temp.toFile());

            // If already WebP, just copy
            if (file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".webp")) {
                Files.copy(temp, output, StandardCopyOption.REPLACE_EXISTING);
                return;
            }

            // Convert using cwebp
            ProcessBuilder pb = new ProcessBuilder("cwebp", "-q", "80", "-quiet", temp.toString(), "-o", output.toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String procOutput = new String(proc.getInputStream().readAllBytes());
            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                throw new IOException("cwebp failed (exit " + exitCode + "): " + procOutput);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Conversion interrupted", e);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
