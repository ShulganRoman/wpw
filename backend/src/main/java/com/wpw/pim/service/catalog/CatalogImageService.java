package com.wpw.pim.service.catalog;

import com.wpw.pim.repository.catalog.CategoryRepository;
import com.wpw.pim.repository.catalog.ProductGroupRepository;
import com.wpw.pim.repository.catalog.SectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogImageService {

    private final SectionRepository sectionRepo;
    private final CategoryRepository categoryRepo;
    private final ProductGroupRepository groupRepo;

    @Value("${pim.media.base-path:/media/products}")
    private String mediaBasePath;

    @Value("${pim.media.base-url:/media/products}")
    private String mediaBaseUrl;

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public String upload(String nodeType, UUID nodeId, MultipartFile file) {
        validateNodeType(nodeType);

        Path dir = Paths.get(mediaBasePath, "catalog", nodeType);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot create media directory");
        }

        Path webpPath = dir.resolve(nodeId + ".webp");
        String url = mediaBaseUrl + "/catalog/" + nodeType + "/" + nodeId + ".webp";

        try {
            convertToWebp(file, webpPath);
        } catch (IOException e) {
            log.error("WebP conversion failed for catalog node {}/{}: {}", nodeType, nodeId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Image conversion failed");
        }

        saveUrl(nodeType, nodeId, url);
        return url;
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void delete(String nodeType, UUID nodeId) {
        Path webpPath = Paths.get(mediaBasePath, "catalog", nodeType, nodeId + ".webp");
        try {
            Files.deleteIfExists(webpPath);
        } catch (IOException e) {
            log.warn("Could not delete catalog image {}: {}", webpPath, e.getMessage());
        }
        saveUrl(nodeType, nodeId, null);
    }

    private void validateNodeType(String nodeType) {
        if (!nodeType.equals("sections") && !nodeType.equals("categories") && !nodeType.equals("product-groups")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unknown node type: " + nodeType + ". Use sections, categories or product-groups");
        }
    }

    private void saveUrl(String nodeType, UUID nodeId, String url) {
        switch (nodeType) {
            case "sections" -> sectionRepo.findById(nodeId)
                .ifPresentOrElse(s -> { s.setImageUrl(url); sectionRepo.save(s); },
                    () -> { throw new ResponseStatusException(HttpStatus.NOT_FOUND); });
            case "categories" -> categoryRepo.findById(nodeId)
                .ifPresentOrElse(c -> { c.setImageUrl(url); categoryRepo.save(c); },
                    () -> { throw new ResponseStatusException(HttpStatus.NOT_FOUND); });
            case "product-groups" -> groupRepo.findById(nodeId)
                .ifPresentOrElse(g -> { g.setImageUrl(url); groupRepo.save(g); },
                    () -> { throw new ResponseStatusException(HttpStatus.NOT_FOUND); });
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown node type: " + nodeType);
        }
    }

    private void convertToWebp(MultipartFile file, Path output) throws IOException {
        Path temp = Files.createTempFile("catalog-img-", "." + getExtension(file.getOriginalFilename()));
        try {
            file.transferTo(temp);
            ProcessBuilder pb = new ProcessBuilder(
                "cwebp", "-q", "85", "-quiet", temp.toString(), "-o", output.toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String procOutput = new String(proc.getInputStream().readAllBytes());
            int exitCode;
            try {
                exitCode = proc.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted during cwebp");
            }
            if (exitCode != 0) {
                throw new IOException("cwebp failed (exit " + exitCode + "): " + procOutput);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "jpg";
    }
}
