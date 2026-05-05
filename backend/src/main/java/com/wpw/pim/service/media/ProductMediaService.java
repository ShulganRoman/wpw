package com.wpw.pim.service.media;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.enums.FileType;
import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.media.MediaImageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.*;

/**
 * Сервис для управления изображениями конкретного товара.
 * <p>
 * Предоставляет CRUD-операции для медиафайлов продукта:
 * получение списка, добавление новых изображений с конвертацией в WebP,
 * удаление изображений с очисткой файла на диске.
 * </p>
 *
 * @see MediaFile
 * @see PhotoImportService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductMediaService {

    private final ProductRepository productRepo;
    private final MediaFileRepository mediaFileRepo;
    private final DealerRepository dealerRepo;

    @Value("${pim.media.base-path:/media/products}")
    private String mediaBasePath;

    @Value("${pim.media.base-url:/media/products}")
    private String mediaBaseUrl;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "webp", "bmp", "tiff"
    );

    /**
     * Возвращает список изображений товара, отсортированных по sortOrder.
     *
     * @param productId идентификатор товара
     * @return список {@link MediaImageDto}
     * @throws ResponseStatusException 404 если товар не найден
     */
    @Transactional(readOnly = true)
    public List<MediaImageDto> getImages(UUID productId) {
        return getImages(productId, null, false);
    }

    @Transactional(readOnly = true)
    public List<MediaImageDto> getImages(UUID productId, UUID dealerId, boolean isAdmin) {
        ensureProductExists(productId);
        return toImageDtoList(productId, dealerId, isAdmin);
    }

    /**
     * Добавляет изображения к товару: конвертирует в WebP, сохраняет на диск и создаёт записи в БД.
     * <p>
     * Для каждого файла:
     * <ol>
     *   <li>Определяет toolNo из продукта</li>
     *   <li>Вычисляет следующий номер варианта по файлам в папке</li>
     *   <li>Конвертирует в WebP через cwebp</li>
     *   <li>Сохраняет файл и создаёт запись {@link MediaFile}</li>
     * </ol>
     * Файл сохраняется на диск ДО создания записи в БД: если конвертация упала, запись не создаётся.
     * </p>
     *
     * @param productId идентификатор товара
     * @param files     массив загружаемых файлов изображений
     * @return обновлённый список {@link MediaImageDto}
     * @throws ResponseStatusException 404 если товар не найден
     */
    @Transactional
    public List<MediaImageDto> addImages(UUID productId, MultipartFile[] files) {
        return addImages(productId, files, null, false);
    }

    @Transactional
    public List<MediaImageDto> addImages(UUID productId, MultipartFile[] files, UUID dealerId, boolean isAdmin) {
        Dealer uploader = dealerId != null ? dealerRepo.findById(dealerId).orElse(null) : null;

        Product product = productRepo.findById(productId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Product not found: " + productId));

        String toolNo = product.getToolNo().toUpperCase();
        Path toolDir = Paths.get(mediaBasePath, toolNo);

        try {
            Files.createDirectories(toolDir);
        } catch (IOException e) {
            log.error("Failed to create directory {}: {}", toolDir, e.getMessage());
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Cannot create media directory");
        }

        int nextVariant;
        try {
            nextVariant = getNextVariant(toolDir);
        } catch (IOException e) {
            log.error("Failed to scan directory {}: {}", toolDir, e.getMessage());
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Cannot scan media directory");
        }

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) continue;

            String ext = getExtension(originalName).toLowerCase();
            if (!SUPPORTED_EXTENSIONS.contains(ext)) {
                log.warn("Skipping unsupported file type: {}", originalName);
                continue;
            }

            try {
                String webpName = nextVariant + ".webp";
                Path webpPath = toolDir.resolve(webpName);
                String url = mediaBaseUrl + "/" + toolNo + "/" + webpName;

                // Конвертация в WebP через temporary file
                convertToWebp(file, webpPath);

                // Создание записи MediaFile в БД
                MediaFile mf = new MediaFile();
                mf.setProduct(product);
                mf.setFileType(FileType.image);
                mf.setUrl(url);
                mf.setThumbnailUrl(url);
                mf.setAltText(product.getToolNo() + " image " + nextVariant);
                mf.setSortOrder(nextVariant - 1);
                mf.setUploadedByDealer(uploader);
                mediaFileRepo.save(mf);

                log.info("Added image for product {}: {}", product.getToolNo(), url);
                nextVariant++;
            } catch (Exception e) {
                log.error("Failed to process image {} for product {}: {}",
                    originalName, product.getToolNo(), e.getMessage());
                throw new ResponseStatusException(INTERNAL_SERVER_ERROR,
                    "Failed to process image: " + originalName);
            }
        }

        return toImageDtoList(productId, dealerId, isAdmin);
    }

    /**
     * Удаляет изображение товара: удаляет запись из БД и файл с диска.
     * <p>
     * Если файл на диске не удаётся удалить -- логируется предупреждение,
     * но исключение не выбрасывается (запись в БД всё равно удаляется).
     * </p>
     *
     * @param productId идентификатор товара
     * @param imageId   идентификатор медиафайла
     * @return обновлённый список {@link MediaImageDto}
     * @throws ResponseStatusException 404 если изображение не найдено, 403 если не принадлежит товару
     */
    @Transactional
    public List<MediaImageDto> deleteImage(UUID productId, UUID imageId) {
        return deleteImage(productId, imageId, null, false);
    }

    @Transactional
    public List<MediaImageDto> deleteImage(UUID productId, UUID imageId, UUID dealerId, boolean isAdmin) {
        ensureProductExists(productId);

        MediaFile mediaFile = mediaFileRepo.findById(imageId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Image not found: " + imageId));

        if (mediaFile.getProduct() == null || !mediaFile.getProduct().getId().equals(productId)) {
            throw new ResponseStatusException(FORBIDDEN, "Image does not belong to this product");
        }

        // dealerId != null means the request comes from a dealer — enforce ownership
        if (dealerId != null) {
            UUID uploaderDealerId = mediaFile.getUploadedByDealer() != null
                ? mediaFile.getUploadedByDealer().getId() : null;
            if (!dealerId.equals(uploaderDealerId)) {
                throw new ResponseStatusException(FORBIDDEN, "You can only delete images you uploaded");
            }
        }
        // dealerId == null → request comes from a non-dealer user (admin) → allowed

        mediaFileRepo.delete(mediaFile);
        mediaFileRepo.flush();
        tryDeleteFileFromDisk(mediaFile.getUrl());

        log.info("Deleted image {} for product {}", imageId, productId);

        return toImageDtoList(productId, dealerId, isAdmin);
    }

    /**
     * Проверяет существование товара, выбрасывает 404 при отсутствии.
     */
    private void ensureProductExists(UUID productId) {
        if (!productRepo.existsById(productId)) {
            throw new ResponseStatusException(NOT_FOUND, "Product not found: " + productId);
        }
    }

    /**
     * Возвращает список MediaImageDto для товара. canDelete:
     *   - admin: всегда true
     *   - dealer: true только для своих изображений (uploadedByDealer.id == dealerId)
     *   - anonymous: false
     */
    private List<MediaImageDto> toImageDtoList(UUID productId, UUID dealerId, boolean isAdmin) {
        return mediaFileRepo.findByProductIdOrderBySortOrder(productId).stream()
            .map(mf -> {
                boolean canDelete;
                if (dealerId != null) {
                    // dealer: can only delete their own images
                    canDelete = mf.getUploadedByDealer() != null
                        && dealerId.equals(mf.getUploadedByDealer().getId());
                } else {
                    // non-dealer (admin): can delete any image
                    canDelete = true;
                }
                return new MediaImageDto(mf.getId(), mf.getUrl(), mf.getSortOrder(), canDelete);
            })
            .toList();
    }

    /**
     * Конвертирует загруженный файл в WebP формат через утилиту cwebp.
     * Если файл уже в формате WebP, просто копирует его.
     */
    private void convertToWebp(MultipartFile file, Path output) throws IOException {
        Path temp = Files.createTempFile("product-img-", "." + getExtension(file.getOriginalFilename()));
        try {
            file.transferTo(temp.toFile());

            // Если уже WebP -- просто копируем
            if (file.getOriginalFilename() != null
                && file.getOriginalFilename().toLowerCase().endsWith(".webp")) {
                Files.copy(temp, output, StandardCopyOption.REPLACE_EXISTING);
                return;
            }

            // Конвертация через cwebp
            ProcessBuilder pb = new ProcessBuilder(
                "cwebp", "-q", "80", "-quiet", temp.toString(), "-o", output.toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String procOutput = new String(proc.getInputStream().readAllBytes());
            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                throw new IOException("cwebp failed (exit " + exitCode + "): " + procOutput);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Image conversion interrupted", e);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /**
     * Определяет следующий номер варианта изображения в директории продукта.
     * Сканирует файлы вида {число}.webp и возвращает max + 1.
     */
    private int getNextVariant(Path toolDir) throws IOException {
        int max = 0;
        try (Stream<Path> files = Files.list(toolDir)) {
            for (Path f : files.toList()) {
                String name = f.getFileName().toString().replace(".webp", "");
                try {
                    int n = Integer.parseInt(name);
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) {
                    // Пропускаем файлы с нечисловыми именами
                }
            }
        }
        return max + 1;
    }

    /**
     * Пытается удалить файл с диска по его URL.
     * При ошибке логирует warning, но не выбрасывает исключение.
     */
    private void tryDeleteFileFromDisk(String url) {
        try {
            String relativePath = url.startsWith(mediaBaseUrl)
                ? url.substring(mediaBaseUrl.length())
                : url;
            Path filePath = Paths.get(mediaBasePath).resolve(relativePath.replaceFirst("^/", ""));
            if (Files.deleteIfExists(filePath)) {
                log.debug("Deleted file from disk: {}", filePath);
            } else {
                log.warn("File not found on disk for deletion: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file from disk for URL {}: {}", url, e.getMessage());
        }
    }

    /**
     * Извлекает расширение файла из имени.
     */
    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1) : "";
    }
}
