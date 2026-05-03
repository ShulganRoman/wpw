package com.wpw.pim.web.controller;

import com.wpw.pim.service.media.PhotoImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Контроллер для управления фотографиями продуктов в админ-панели.
 * <p>
 * Предоставляет эндпоинты для:
 * <ul>
 *   <li>Валидации и импорта отдельных фотографий</li>
 *   <li>Валидации и импорта фотографий из архивов (ZIP, 7Z, TAR, TAR.GZ)</li>
 *   <li>Синхронизации существующих фотографий на диске</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/admin/photos")
@RequiredArgsConstructor
@Tag(name = "Admin: Photos", description = "Product photo import and management (WebP conversion, ZIP/7Z/TAR archives)")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Admin access required")
})
public class PhotoSyncController {

    private final PhotoImportService photoImportService;

    // ========================= Импорт отдельных файлов =========================

    /**
     * Валидация загруженных фотографий — сопоставление имён файлов с продуктами.
     *
     * @param files массив загруженных файлов изображений
     * @return отчёт о валидации с информацией о сопоставлении
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate photos", description = "Validates file names against SKUs without importing.")
    @ApiResponse(responseCode = "200", description = "Validation report")
    public Map<String, Object> validatePhotos(@RequestParam("files") MultipartFile[] files) {
        return photoImportService.validatePhotos(files);
    }

    /**
     * Импорт загруженных фотографий — конвертация в WebP и сохранение.
     *
     * @param files массив загруженных файлов изображений
     * @return отчёт об импорте со статистикой обработки
     * @throws IOException при ошибке конвертации или записи файлов
     */
    @PostMapping("/import")
    @Operation(summary = "Import photos", description = "Converts files to WebP and saves. File without suffix (_N) becomes the main image (sort_order=0).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Import report"),
        @ApiResponse(responseCode = "400", description = "Invalid files")
    })
    public Map<String, Object> importPhotos(@RequestParam("files") MultipartFile[] files) throws IOException {
        return photoImportService.importPhotos(files);
    }

    /**
     * Синхронизация существующих фотографий на диске с базой данных.
     *
     * @return отчёт о синхронизации
     * @throws IOException при ошибке чтения файловой системы
     */
    @PostMapping("/sync")
    @Operation(summary = "Sync photos from disk", description = "Creates MediaFile records for WebP files already on disk (no import).")
    @ApiResponse(responseCode = "200", description = "Sync report")
    public Map<String, Object> syncExistingPhotos() throws IOException {
        return photoImportService.syncExistingPhotos();
    }

    // ========================= Импорт из архивов =========================

    /**
     * Validate archive с фотографиями — извлечение изображений и сопоставление с продуктами.
     * <p>
     * Поддерживаемые форматы: ZIP, 7Z, TAR, TAR.GZ, TGZ.
     * Архив может содержать вложенные директории — все изображения будут найдены рекурсивно.
     * Системные файлы macOS (__MACOSX, .DS_Store) автоматически пропускаются.
     * </p>
     *
     * @param archive загруженный архивный файл
     * @return отчёт о валидации с содержимым архива и сопоставлением с продуктами
     * @throws IOException при ошибке чтения или распаковки архива
     */
    @PostMapping("/archive/validate")
    @Operation(summary = "Validate archive", description = "Scans archive (ZIP/7Z/TAR) and validates file names against SKUs without extracting to disk.")
    @ApiResponse(responseCode = "200", description = "Archive validation report")
    public Map<String, Object> validateArchive(@RequestParam("archive") MultipartFile archive) throws IOException {
        return photoImportService.validateArchive(archive);
    }

    /**
     * Import photosграфий из архива — извлечение, конвертация в WebP и сохранение.
     * <p>
     * Поддерживаемые форматы: ZIP, 7Z, TAR, TAR.GZ, TGZ.
     * Каждое изображение конвертируется в WebP и привязывается к продукту по номеру инструмента (toolNo).
     * </p>
     *
     * @param archive загруженный архивный файл
     * @return отчёт об импорте со статистикой обработки
     * @throws IOException при ошибке распаковки, конвертации или записи файлов
     */
    @PostMapping("/archive/import")
    @Operation(summary = "Import from archive", description = "Extracts, converts to WebP and imports all photos from archive.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Import report"),
        @ApiResponse(responseCode = "400", description = "Unsupported archive format")
    })
    public Map<String, Object> importArchive(@RequestParam("archive") MultipartFile archive) throws IOException {
        return photoImportService.importArchive(archive);
    }

    @PreAuthorize("hasAuthority('MODIFY_PRODUCTS')")
    @DeleteMapping("/all")
    @Operation(summary = "Delete all product media files", description = "Deletes all MediaFile records from DB and all product directories from disk. The catalog/ directory (catalog node images) is preserved. Requires MODIFY_PRODUCTS.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deletion report"),
        @ApiResponse(responseCode = "403", description = "MODIFY_PRODUCTS required")
    })
    public Map<String, Object> deleteAllProductMedia() throws IOException {
        return photoImportService.deleteAllProductMedia();
    }
}
