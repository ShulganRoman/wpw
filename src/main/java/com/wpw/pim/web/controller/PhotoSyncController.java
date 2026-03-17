package com.wpw.pim.web.controller;

import com.wpw.pim.service.media.PhotoImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/photos")
@RequiredArgsConstructor
public class PhotoSyncController {

    private final PhotoImportService photoImportService;

    @PostMapping("/validate")
    public Map<String, Object> validatePhotos(@RequestParam("files") MultipartFile[] files) {
        return photoImportService.validatePhotos(files);
    }

    @PostMapping("/import")
    public Map<String, Object> importPhotos(@RequestParam("files") MultipartFile[] files) throws IOException {
        return photoImportService.importPhotos(files);
    }

    @PostMapping("/sync")
    public Map<String, Object> syncExistingPhotos() throws IOException {
        return photoImportService.syncExistingPhotos();
    }
}
