package com.wpw.pim.service.media;

import com.wpw.pim.domain.enums.FileType;
import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.service.media.ArchiveExtractorService.ExtractionResult;
import com.wpw.pim.service.media.ArchiveExtractorService.ExtractedFile;
import com.wpw.pim.service.media.ArchiveExtractorService.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhotoImportServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private ArchiveExtractorService archiveExtractorService;

    @InjectMocks private PhotoImportService photoImportService;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(photoImportService, "mediaBasePath", tempDir.toString());
        ReflectionTestUtils.setField(photoImportService, "mediaBaseUrl", "/media/products");
    }

    private Product product(String toolNo) {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setToolNo(toolNo);
        return p;
    }

    @Nested
    @DisplayName("validatePhotos")
    class ValidatePhotos {

        @Test
        @DisplayName("matches files to products by toolNo")
        void matchesFiles() {
            when(productRepository.findAll()).thenReturn(List.of(product("WPW-001"), product("WPW-002")));

            MockMultipartFile f1 = new MockMultipartFile("files", "WPW-001.jpg", "image/jpeg", new byte[]{1});
            MockMultipartFile f2 = new MockMultipartFile("files", "UNKNOWN.jpg", "image/jpeg", new byte[]{2});
            MockMultipartFile f3 = new MockMultipartFile("files", "WPW-002_1.png", "image/png", new byte[]{3});

            Map<String, Object> report = photoImportService.validatePhotos(
                    new MockMultipartFile[]{f1, f2, f3});

            assertThat(report).containsEntry("totalFiles", 3);
            assertThat(report).containsEntry("matched", 2);
            assertThat(report).containsEntry("unmatched", 1);
        }

        @Test
        @DisplayName("skips files with blank original filename")
        void skipsBlankFilenames() {
            when(productRepository.findAll()).thenReturn(List.of());

            MockMultipartFile f1 = new MockMultipartFile("files", "", "image/jpeg", new byte[]{1});
            Map<String, Object> report = photoImportService.validatePhotos(new MockMultipartFile[]{f1});

            assertThat(report).containsEntry("totalFiles", 1);
            assertThat(report).containsEntry("matched", 0);
            assertThat(report).containsEntry("unmatched", 0);
        }
    }

    @Nested
    @DisplayName("validateArchive")
    class ValidateArchive {

        @Test
        @DisplayName("validates archive contents without extraction")
        void validatesArchive() throws Exception {
            ScanResult scan = new ScanResult(
                    List.of("WPW-001.jpg", "WPW-002.png", "UNKNOWN.bmp"),
                    5, 3, List.of("readme.txt", ".DS_Store"));
            when(archiveExtractorService.scanImageNames(any())).thenReturn(scan);
            when(productRepository.findAll()).thenReturn(List.of(product("WPW-001"), product("WPW-002")));

            MockMultipartFile archive = new MockMultipartFile("archive", "photos.zip", "application/zip", new byte[]{1});
            Map<String, Object> report = photoImportService.validateArchive(archive);

            assertThat(report).containsEntry("matched", 2);
            assertThat(report).containsEntry("unmatched", 1);
            assertThat(report).containsEntry("totalEntriesInArchive", 5);
            assertThat(report).containsEntry("imagesExtracted", 3);
        }
    }

    @Nested
    @DisplayName("importPhotos")
    class ImportPhotos {

        @Test
        @DisplayName("groups files by toolNo and skips unmatched")
        void importPhotos_groupsByToolAndSkipsUnmatched() throws Exception {
            Product p = product("WPW-001");
            when(productRepository.findAll()).thenReturn(List.of(p));
            when(mediaFileRepository.findAll()).thenReturn(List.of());
            when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

            // WebP файл - не требует cwebp, просто копируется
            MockMultipartFile f1 = new MockMultipartFile("files", "WPW-001.webp", "image/webp", new byte[]{1, 2, 3});
            MockMultipartFile f2 = new MockMultipartFile("files", "UNKNOWN.webp", "image/webp", new byte[]{4, 5});

            Map<String, Object> report = photoImportService.importPhotos(new MockMultipartFile[]{f1, f2});

            assertThat(report).containsEntry("matchedProducts", 1);
            assertThat(report).containsEntry("skipped", 1);
            assertThat(report).containsEntry("converted", 1);
            verify(mediaFileRepository, times(1)).save(any(MediaFile.class));
        }

        @Test
        @DisplayName("skips files with blank filename")
        void importPhotos_blankFilenames_skipped() throws Exception {
            when(productRepository.findAll()).thenReturn(List.of());
            when(mediaFileRepository.findAll()).thenReturn(List.of());

            MockMultipartFile f1 = new MockMultipartFile("files", "", "image/jpeg", new byte[]{1});

            Map<String, Object> report = photoImportService.importPhotos(new MockMultipartFile[]{f1});

            assertThat(report).containsEntry("matchedProducts", 0);
            assertThat(report).containsEntry("converted", 0);
        }

        @Test
        @DisplayName("skips non-image files")
        void importPhotos_nonImageFiles_skipped() throws Exception {
            Product p = product("WPW-001");
            when(productRepository.findAll()).thenReturn(List.of(p));
            when(mediaFileRepository.findAll()).thenReturn(List.of());

            MockMultipartFile txt = new MockMultipartFile("files", "WPW-001.txt", "text/plain", new byte[]{1});

            Map<String, Object> report = photoImportService.importPhotos(new MockMultipartFile[]{txt});

            assertThat(report).containsEntry("matchedProducts", 1);
            assertThat(report).containsEntry("skipped", 1);
            assertThat(report).containsEntry("converted", 0);
        }

        @Test
        @DisplayName("handles existing URL by incrementing variant number")
        void importPhotos_existingUrl_incrementsVariant() throws Exception {
            Product p = product("WPW-001");
            when(productRepository.findAll()).thenReturn(List.of(p));

            MediaFile existingMf = new MediaFile();
            existingMf.setUrl("/media/products/WPW-001/1.webp");
            when(mediaFileRepository.findAll()).thenReturn(List.of(existingMf));
            when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

            MockMultipartFile f1 = new MockMultipartFile("files", "WPW-001.webp", "image/webp", new byte[]{1, 2, 3});

            Map<String, Object> report = photoImportService.importPhotos(new MockMultipartFile[]{f1});

            assertThat(report).containsEntry("converted", 1);
        }

        @Test
        @DisplayName("multiple files for same tool increment variant correctly")
        void importPhotos_multipleFilesForSameTool() throws Exception {
            Product p = product("WPW-002");
            when(productRepository.findAll()).thenReturn(List.of(p));
            when(mediaFileRepository.findAll()).thenReturn(List.of());
            when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

            MockMultipartFile f1 = new MockMultipartFile("files", "WPW-002.webp", "image/webp", new byte[]{1});
            MockMultipartFile f2 = new MockMultipartFile("files", "WPW-002_1.webp", "image/webp", new byte[]{2});

            Map<String, Object> report = photoImportService.importPhotos(new MockMultipartFile[]{f1, f2});

            assertThat(report).containsEntry("matchedProducts", 1);
            assertThat(report).containsEntry("converted", 2);
            verify(mediaFileRepository, times(2)).save(any(MediaFile.class));
        }
    }

    @Nested
    @DisplayName("importArchive")
    class ImportArchive {

        @Test
        @DisplayName("imports archive and converts extracted files")
        void importArchive_withMatches_convertsAndSaves() throws Exception {
            Product p = product("WPW-001");
            when(productRepository.findAll()).thenReturn(List.of(p));
            when(mediaFileRepository.findAll()).thenReturn(List.of());
            when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

            // Создаём WebP temp file чтобы convertTempFileToWebp просто скопировал его
            Path tempFile = java.nio.file.Files.createTempFile(tempDir, "img-", ".webp");
            java.nio.file.Files.write(tempFile, new byte[]{1, 2, 3});

            ExtractionResult extraction = new ExtractionResult(
                    List.of(new ExtractedFile(tempFile, "WPW-001.webp")),
                    2, 1, List.of("readme.txt"));

            when(archiveExtractorService.extractImages(any())).thenReturn(extraction);
            doNothing().when(archiveExtractorService).cleanup(any());

            MockMultipartFile archive = new MockMultipartFile("archive", "photos.zip", "application/zip", new byte[]{1});
            Map<String, Object> report = photoImportService.importArchive(archive);

            assertThat(report).containsEntry("matchedProducts", 1);
            assertThat(report).containsEntry("converted", 1);
            assertThat(report).containsEntry("totalEntriesInArchive", 2);
            verify(archiveExtractorService).cleanup(extraction);
        }

        @Test
        @DisplayName("skips unmatched tool numbers in archive")
        void importArchive_unmatchedToolNo_skipped() throws Exception {
            when(productRepository.findAll()).thenReturn(List.of());
            when(mediaFileRepository.findAll()).thenReturn(List.of());

            Path tempFile = java.nio.file.Files.createTempFile(tempDir, "img-", ".webp");
            java.nio.file.Files.write(tempFile, new byte[]{1});

            ExtractionResult extraction = new ExtractionResult(
                    List.of(new ExtractedFile(tempFile, "UNKNOWN.webp")),
                    1, 1, List.of());

            when(archiveExtractorService.extractImages(any())).thenReturn(extraction);
            doNothing().when(archiveExtractorService).cleanup(any());

            MockMultipartFile archive = new MockMultipartFile("archive", "photos.zip", "application/zip", new byte[]{1});
            Map<String, Object> report = photoImportService.importArchive(archive);

            assertThat(report).containsEntry("matchedProducts", 0);
            assertThat(report).containsEntry("skipped", 1);
            verify(mediaFileRepository, never()).save(any());
        }

        @Test
        @DisplayName("cleanup called even if extraction fails")
        void importArchive_extractionFails_cleanupStillCalled() throws Exception {
            when(archiveExtractorService.extractImages(any()))
                    .thenThrow(new java.io.IOException("broken archive"));

            MockMultipartFile archive = new MockMultipartFile("archive", "broken.zip", "application/zip", new byte[]{1});

            try {
                photoImportService.importArchive(archive);
            } catch (java.io.IOException expected) {
                // expected
            }

            verify(archiveExtractorService).cleanup(null);
        }

        @Test
        @DisplayName("handles existing URL collision in archive import")
        void importArchive_existingUrlCollision_incrementsVariant() throws Exception {
            Product p = product("WPW-001");
            when(productRepository.findAll()).thenReturn(List.of(p));

            MediaFile existingMf = new MediaFile();
            existingMf.setUrl("/media/products/WPW-001/1.webp");
            when(mediaFileRepository.findAll()).thenReturn(List.of(existingMf));
            when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

            Path tempFile = java.nio.file.Files.createTempFile(tempDir, "img-", ".webp");
            java.nio.file.Files.write(tempFile, new byte[]{1, 2, 3});

            ExtractionResult extraction = new ExtractionResult(
                    List.of(new ExtractedFile(tempFile, "WPW-001.webp")),
                    1, 1, List.of());

            when(archiveExtractorService.extractImages(any())).thenReturn(extraction);
            doNothing().when(archiveExtractorService).cleanup(any());

            MockMultipartFile archive = new MockMultipartFile("archive", "photos.zip", "application/zip", new byte[]{1});
            Map<String, Object> report = photoImportService.importArchive(archive);

            assertThat(report).containsEntry("converted", 1);
        }
    }

    @Nested
    @DisplayName("deleteAllProductMedia")
    class DeleteAllProductMedia {

        @Test
        @DisplayName("deletes all DB records and product directories, preserves catalog/")
        void deletesRecordsAndDirs_preservesCatalog() throws Exception {
            when(mediaFileRepository.count()).thenReturn(5L);

            // Create product dirs and catalog/ dir
            Path toolDir1 = tempDir.resolve("WPW-001");
            Path toolDir2 = tempDir.resolve("WPW-002");
            Path catalogDir = tempDir.resolve("catalog");
            java.nio.file.Files.createDirectories(toolDir1);
            java.nio.file.Files.createDirectories(toolDir2);
            java.nio.file.Files.createDirectories(catalogDir);
            java.nio.file.Files.write(toolDir1.resolve("0.webp"), new byte[]{1});
            java.nio.file.Files.write(catalogDir.resolve("sections"), new byte[]{2});

            Map<String, Object> report = photoImportService.deleteAllProductMedia();

            assertThat(report).containsEntry("deletedRecords", 5L);
            assertThat(report).containsEntry("deletedDirectories", 2);
            assertThat(java.nio.file.Files.exists(toolDir1)).isFalse();
            assertThat(java.nio.file.Files.exists(toolDir2)).isFalse();
            assertThat(java.nio.file.Files.exists(catalogDir)).isTrue();
            verify(mediaFileRepository).deleteAll();
        }

        @Test
        @DisplayName("works when media directory does not exist")
        void mediaDir_absent_returnsZeroDirs() throws Exception {
            ReflectionTestUtils.setField(photoImportService, "mediaBasePath", tempDir.resolve("nonexistent").toString());
            when(mediaFileRepository.count()).thenReturn(0L);

            Map<String, Object> report = photoImportService.deleteAllProductMedia();

            assertThat(report).containsEntry("deletedRecords", 0L);
            assertThat(report).containsEntry("deletedDirectories", 0);
            verify(mediaFileRepository).deleteAll();
        }

        @Test
        @DisplayName("works when no product directories exist")
        void noProductDirs_returnsZero() throws Exception {
            when(mediaFileRepository.count()).thenReturn(3L);

            Map<String, Object> report = photoImportService.deleteAllProductMedia();

            assertThat(report).containsEntry("deletedRecords", 3L);
            assertThat(report).containsEntry("deletedDirectories", 0);
        }
    }

    @Nested
    @DisplayName("extractVariantOrder")
    class ExtractVariantOrder {

        @Test
        @DisplayName("no suffix returns -1 (main image)")
        void noSuffix_returnsMinusOne() {
            assertThat(photoImportService.extractVariantOrder("sku.jpeg")).isEqualTo(-1);
            assertThat(photoImportService.extractVariantOrder("WPW-001.jpg")).isEqualTo(-1);
            assertThat(photoImportService.extractVariantOrder("A-100.webp")).isEqualTo(-1);
        }

        @Test
        @DisplayName("_N suffix returns the numeric value")
        void withSuffix_returnsNumber() {
            assertThat(photoImportService.extractVariantOrder("sku_1.jpeg")).isEqualTo(1);
            assertThat(photoImportService.extractVariantOrder("sku_2.png")).isEqualTo(2);
            assertThat(photoImportService.extractVariantOrder("WPW-001_10.webp")).isEqualTo(10);
        }

        @Test
        @DisplayName("no-suffix file sorts before suffix files")
        void noSuffix_sortsFirst() {
            List<String> names = List.of("sku_2.jpeg", "sku.jpeg", "sku_1.jpeg");
            List<String> sorted = names.stream()
                .sorted(Comparator.comparingInt(n -> photoImportService.extractVariantOrder(n)))
                .toList();
            assertThat(sorted).containsExactly("sku.jpeg", "sku_1.jpeg", "sku_2.jpeg");
        }
    }

    @Nested
    @DisplayName("importPhotos — variant ordering")
    class ImportPhotosVariantOrdering {

        @Test
        @DisplayName("no-suffix file gets sort_order 0 even if supplied last")
        void noSuffixFile_getsSortOrderZero() throws Exception {
            Product p = product("WPW-001");
            when(productRepository.findAll()).thenReturn(List.of(p));
            when(mediaFileRepository.findAll()).thenReturn(List.of());

            List<MediaFile> saved = new ArrayList<>();
            when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> {
                saved.add(inv.getArgument(0));
                return inv.getArgument(0);
            });

            // Supplied in wrong order: _2, _1, no-suffix
            MockMultipartFile f2  = new MockMultipartFile("files", "WPW-001_2.webp", "image/webp", new byte[]{3});
            MockMultipartFile f1  = new MockMultipartFile("files", "WPW-001_1.webp", "image/webp", new byte[]{2});
            MockMultipartFile f0  = new MockMultipartFile("files", "WPW-001.webp",   "image/webp", new byte[]{1});

            photoImportService.importPhotos(new MockMultipartFile[]{f2, f1, f0});

            assertThat(saved).hasSize(3);
            // sort_order mirrors disk slot: no-suffix processed first → slot 0
            assertThat(saved.get(0).getSortOrder()).isEqualTo(0);
            assertThat(saved.get(1).getSortOrder()).isEqualTo(1);
            assertThat(saved.get(2).getSortOrder()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("importArchive — variant ordering")
    class ImportArchiveVariantOrdering {

        @Test
        @DisplayName("no-suffix file from archive gets sort_order 0")
        void noSuffixFromArchive_getsSortOrderZero() throws Exception {
            Product p = product("WPW-001");
            when(productRepository.findAll()).thenReturn(List.of(p));
            when(mediaFileRepository.findAll()).thenReturn(List.of());

            List<MediaFile> saved = new ArrayList<>();
            when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> {
                saved.add(inv.getArgument(0));
                return inv.getArgument(0);
            });

            Path tmp0 = java.nio.file.Files.createTempFile(tempDir, "img0-", ".webp");
            Path tmp1 = java.nio.file.Files.createTempFile(tempDir, "img1-", ".webp");
            java.nio.file.Files.write(tmp0, new byte[]{1});
            java.nio.file.Files.write(tmp1, new byte[]{2});

            // Supplied in wrong order: _1 first, no-suffix second
            ExtractionResult extraction = new ExtractionResult(
                List.of(
                    new ExtractedFile(tmp1, "WPW-001_1.webp"),
                    new ExtractedFile(tmp0, "WPW-001.webp")
                ),
                2, 2, List.of());

            when(archiveExtractorService.extractImages(any())).thenReturn(extraction);
            doNothing().when(archiveExtractorService).cleanup(any());

            photoImportService.importArchive(
                new MockMultipartFile("archive", "photos.zip", "application/zip", new byte[]{1}));

            assertThat(saved).hasSize(2);
            assertThat(saved.get(0).getSortOrder()).isEqualTo(0); // no-suffix
            assertThat(saved.get(1).getSortOrder()).isEqualTo(1); // _1
        }
    }

    @Nested
    @DisplayName("validatePhotos edge cases")
    class ValidatePhotosEdgeCases {

        @Test
        @DisplayName("null filename files are skipped")
        void validatePhotos_nullFilename_skipped() {
            when(productRepository.findAll()).thenReturn(List.of());

            MockMultipartFile f = new MockMultipartFile("files", null, "image/jpeg", new byte[]{1});
            Map<String, Object> report = photoImportService.validatePhotos(new MockMultipartFile[]{f});

            assertThat(report).containsEntry("totalFiles", 1);
            assertThat(report).containsEntry("matched", 0);
            assertThat(report).containsEntry("unmatched", 0);
        }
    }

    @Nested
    @DisplayName("validateArchive edge cases")
    class ValidateArchiveEdgeCases {

        @Test
        @DisplayName("reports skipped file names when present")
        void validateArchive_withSkippedFiles_includesInReport() throws Exception {
            ScanResult scan = new ScanResult(
                    List.of("WPW-001.jpg"),
                    3, 1, List.of("readme.txt", ".DS_Store"));
            when(archiveExtractorService.scanImageNames(any())).thenReturn(scan);
            when(productRepository.findAll()).thenReturn(List.of(product("WPW-001")));

            MockMultipartFile archive = new MockMultipartFile("archive", "photos.zip", "application/zip", new byte[]{1});
            Map<String, Object> report = photoImportService.validateArchive(archive);

            assertThat(report).containsEntry("skippedFiles", 2);
            assertThat(report).containsKey("skippedFileNames");
        }

        @Test
        @DisplayName("empty skipped list omits skippedFileNames key")
        void validateArchive_noSkippedFiles_omitsKey() throws Exception {
            ScanResult scan = new ScanResult(List.of("WPW-001.jpg"), 1, 1, List.of());
            when(archiveExtractorService.scanImageNames(any())).thenReturn(scan);
            when(productRepository.findAll()).thenReturn(List.of(product("WPW-001")));

            MockMultipartFile archive = new MockMultipartFile("archive", "photos.zip", "application/zip", new byte[]{1});
            Map<String, Object> report = photoImportService.validateArchive(archive);

            assertThat(report).doesNotContainKey("skippedFileNames");
        }
    }

    @Nested
    @DisplayName("importPhotos — cwebp errors")
    class ImportPhotosCwebpErrors {

        private boolean isCwebpAvailable() {
            try {
                Process p = new ProcessBuilder("cwebp", "-version").start();
                return p.waitFor() == 0;
            } catch (Exception e) {
                return false;
            }
        }

        private byte[] createMinimalJpeg() throws Exception {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics g = img.createGraphics();
            g.setColor(java.awt.Color.BLUE);
            g.fillRect(0, 0, 10, 10);
            g.dispose();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "jpg", baos);
            return baos.toByteArray();
        }

        @Test
        @DisplayName("invalid jpg byte data — error captured in errorDetails")
        void importPhotos_brokenJpg_errorCaptured() throws Exception {
            org.junit.jupiter.api.Assumptions.assumeTrue(isCwebpAvailable(), "cwebp unavailable");

            Product p = product("WPW-001");
            when(productRepository.findAll()).thenReturn(List.of(p));
            when(mediaFileRepository.findAll()).thenReturn(List.of());

            // Поломанный JPEG — cwebp вернёт ненулевой код
            MockMultipartFile broken = new MockMultipartFile(
                "files", "WPW-001.jpg", "image/jpeg", new byte[]{0, 1, 2});

            Map<String, Object> report = photoImportService.importPhotos(new MockMultipartFile[]{broken});

            assertThat(report).containsEntry("errors", 1);
            assertThat(report).containsKey("errorDetails");
        }

        @Test
        @DisplayName("valid jpg converts successfully via real cwebp")
        void importPhotos_validJpg_realCwebp() throws Exception {
            org.junit.jupiter.api.Assumptions.assumeTrue(isCwebpAvailable(), "cwebp unavailable");

            Product p = product("WPW-001");
            when(productRepository.findAll()).thenReturn(List.of(p));
            when(mediaFileRepository.findAll()).thenReturn(List.of());
            when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

            byte[] jpg = createMinimalJpeg();
            MockMultipartFile validJpg = new MockMultipartFile(
                "files", "WPW-001.jpg", "image/jpeg", jpg);

            Map<String, Object> report = photoImportService.importPhotos(new MockMultipartFile[]{validJpg});

            assertThat(report).containsEntry("converted", 1);
            assertThat(report).containsEntry("matchedProducts", 1);
        }
    }

    @Nested
    @DisplayName("importArchive — cwebp errors")
    class ImportArchiveCwebpErrors {

        private boolean isCwebpAvailable() {
            try {
                Process p = new ProcessBuilder("cwebp", "-version").start();
                return p.waitFor() == 0;
            } catch (Exception e) {
                return false;
            }
        }

        @Test
        @DisplayName("broken jpg in archive — error captured")
        void importArchive_brokenJpg_errorCaptured() throws Exception {
            org.junit.jupiter.api.Assumptions.assumeTrue(isCwebpAvailable(), "cwebp unavailable");

            Product p = product("WPW-001");
            when(productRepository.findAll()).thenReturn(List.of(p));
            when(mediaFileRepository.findAll()).thenReturn(List.of());

            // создаём тестовый битый jpg в temp
            Path brokenJpg = java.nio.file.Files.createTempFile(tempDir, "broken-", ".jpg");
            java.nio.file.Files.write(brokenJpg, new byte[]{0, 1, 2});

            ExtractionResult extraction = new ExtractionResult(
                List.of(new ExtractedFile(brokenJpg, "WPW-001.jpg")),
                1, 1, List.of());
            when(archiveExtractorService.extractImages(any())).thenReturn(extraction);
            doNothing().when(archiveExtractorService).cleanup(any());

            Map<String, Object> report = photoImportService.importArchive(
                new MockMultipartFile("a", "x.zip", "application/zip", new byte[]{1}));

            assertThat(report).containsEntry("errors", 1);
            assertThat(report).containsKey("errorDetails");
        }
    }

    @Nested
    @DisplayName("@PostConstruct ensureMediaDirectory")
    class EnsureMediaDirectory {

        @Test
        @DisplayName("creates missing media directory")
        void postConstruct_createsMissingDir() {
            Path subDir = tempDir.resolve("missing-subdir");
            ReflectionTestUtils.setField(photoImportService, "mediaBasePath", subDir.toString());

            // вызываем метод напрямую через рефлексию (он package-private)
            ReflectionTestUtils.invokeMethod(photoImportService, "ensureMediaDirectory");

            assertThat(java.nio.file.Files.isDirectory(subDir)).isTrue();
        }

        @Test
        @DisplayName("does nothing when directory exists")
        void postConstruct_existingDir_doesNothing() {
            ReflectionTestUtils.invokeMethod(photoImportService, "ensureMediaDirectory");
            assertThat(java.nio.file.Files.isDirectory(tempDir)).isTrue();
        }
    }

    @Nested
    @DisplayName("syncExistingPhotos")
    class SyncExisting {

        @Test
        @DisplayName("syncs WebP files on disk with database")
        void syncsPhotos() throws Exception {
            Product p = product("WPW-001");
            when(productRepository.findAll()).thenReturn(List.of(p));
            when(mediaFileRepository.findAll()).thenReturn(List.of());
            when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

            // Create directory structure: mediaBasePath/WPW-001/1.webp
            Path toolDir = tempDir.resolve("WPW-001");
            java.nio.file.Files.createDirectories(toolDir);
            java.nio.file.Files.write(toolDir.resolve("1.webp"), new byte[]{1, 2, 3});
            java.nio.file.Files.write(toolDir.resolve("2.webp"), new byte[]{4, 5, 6});

            Map<String, Object> report = photoImportService.syncExistingPhotos();

            assertThat(report).containsEntry("matched", 1);
            assertThat(report).containsEntry("created", 2);
            assertThat(report).containsEntry("skipped", 0);
            verify(mediaFileRepository, times(2)).save(any(MediaFile.class));
        }

        @Test
        @DisplayName("skips unmatched directories and already-existing URLs")
        void skipsUnmatchedAndExisting() throws Exception {
            Product p = product("WPW-001");
            when(productRepository.findAll()).thenReturn(List.of(p));

            MediaFile existing = new MediaFile();
            existing.setUrl("/media/products/WPW-001/1.webp");
            when(mediaFileRepository.findAll()).thenReturn(List.of(existing));
            when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

            Path toolDir = tempDir.resolve("WPW-001");
            java.nio.file.Files.createDirectories(toolDir);
            java.nio.file.Files.write(toolDir.resolve("1.webp"), new byte[]{1});

            // Also create an unmatched directory
            Path unmatchedDir = tempDir.resolve("UNKNOWN");
            java.nio.file.Files.createDirectories(unmatchedDir);
            java.nio.file.Files.write(unmatchedDir.resolve("1.webp"), new byte[]{1});

            Map<String, Object> report = photoImportService.syncExistingPhotos();

            assertThat(report).containsEntry("matched", 1);
            assertThat(report).containsEntry("unmatched", 1);
            assertThat(report).containsEntry("skipped", 1);
            assertThat(report).containsEntry("created", 0);
        }

        @Test
        @DisplayName("throws IOException when media directory does not exist")
        void syncExistingPhotos_noMediaDir_throwsIOException() {
            ReflectionTestUtils.setField(photoImportService, "mediaBasePath", "/nonexistent/path/12345");

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> photoImportService.syncExistingPhotos())
                    .isInstanceOf(java.io.IOException.class)
                    .hasMessageContaining("Media directory not found");
        }

        @Test
        @DisplayName("sorts webp files numerically")
        void syncExistingPhotos_sortsFilesNumerically() throws Exception {
            Product p = product("WPW-SORT");
            when(productRepository.findAll()).thenReturn(List.of(p));
            when(mediaFileRepository.findAll()).thenReturn(List.of());
            when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

            Path toolDir = tempDir.resolve("WPW-SORT");
            java.nio.file.Files.createDirectories(toolDir);
            java.nio.file.Files.write(toolDir.resolve("10.webp"), new byte[]{1});
            java.nio.file.Files.write(toolDir.resolve("2.webp"), new byte[]{2});
            java.nio.file.Files.write(toolDir.resolve("1.webp"), new byte[]{3});

            Map<String, Object> report = photoImportService.syncExistingPhotos();

            assertThat(report).containsEntry("created", 3);
            verify(mediaFileRepository, times(3)).save(any(MediaFile.class));
        }
    }
}
