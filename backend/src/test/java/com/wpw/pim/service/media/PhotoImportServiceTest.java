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
