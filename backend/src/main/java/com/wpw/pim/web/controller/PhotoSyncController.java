package com.wpw.pim.web.controller;

import com.wpw.pim.service.media.PhotoImportService;
import lombok.RequiredArgsConstructor;
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
    public Map<String, Object> syncExistingPhotos() throws IOException {
        return photoImportService.syncExistingPhotos();
    }

    // ========================= Импорт из архивов =========================

    /**
     * Валидация архива с фотографиями — извлечение изображений и сопоставление с продуктами.
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
    public Map<String, Object> validateArchive(@RequestParam("archive") MultipartFile archive) throws IOException {
        return photoImportService.validateArchive(archive);
    }

    /**
     * Импорт фотографий из архива — извлечение, конвертация в WebP и сохранение.
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
    public Map<String, Object> importArchive(@RequestParam("archive") MultipartFile archive) throws IOException {
        return photoImportService.importArchive(archive);
    }
}
