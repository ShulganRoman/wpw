package com.wpw.pim.service.media;

import com.wpw.pim.service.media.ArchiveExtractorService.ExtractionResult;
import com.wpw.pim.service.media.ArchiveExtractorService.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-тесты для {@link ArchiveExtractorService}.
 * Создаёт ZIP-архивы в памяти для проверки извлечения изображений.
 */
class ArchiveExtractorServiceTest {

    private ArchiveExtractorService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new ArchiveExtractorService();
    }

    // ========================= isSupportedArchive =========================

    @Nested
    @DisplayName("isSupportedArchive")
    class IsSupportedArchive {

        @Test
        void isSupportedArchive_zip_returnsTrue() {
            assertThat(service.isSupportedArchive("photos.zip")).isTrue();
        }

        @Test
        void isSupportedArchive_7z_returnsTrue() {
            assertThat(service.isSupportedArchive("photos.7z")).isTrue();
        }

        @Test
        void isSupportedArchive_tar_returnsTrue() {
            assertThat(service.isSupportedArchive("photos.tar")).isTrue();
        }

        @Test
        void isSupportedArchive_tarGz_returnsTrue() {
            assertThat(service.isSupportedArchive("photos.tar.gz")).isTrue();
        }

        @Test
        void isSupportedArchive_tgz_returnsTrue() {
            assertThat(service.isSupportedArchive("photos.tgz")).isTrue();
        }

        @Test
        void isSupportedArchive_gz_returnsTrue() {
            assertThat(service.isSupportedArchive("photos.gz")).isTrue();
        }

        @Test
        void isSupportedArchive_rar_returnsFalse() {
            assertThat(service.isSupportedArchive("photos.rar")).isFalse();
        }

        @Test
        void isSupportedArchive_pdf_returnsFalse() {
            assertThat(service.isSupportedArchive("doc.pdf")).isFalse();
        }

        @Test
        void isSupportedArchive_null_returnsFalse() {
            assertThat(service.isSupportedArchive(null)).isFalse();
        }

        @Test
        void isSupportedArchive_blank_returnsFalse() {
            assertThat(service.isSupportedArchive("   ")).isFalse();
        }

        @Test
        void isSupportedArchive_upperCase_returnsTrue() {
            assertThat(service.isSupportedArchive("PHOTOS.ZIP")).isTrue();
        }
    }

    // ========================= extractImages =========================

    @Nested
    @DisplayName("extractImages — ZIP")
    class ExtractImagesZip {

        @Test
        void extractImages_zipWithImages_extractsAll() throws IOException {
            byte[] zipBytes = createZipArchive(
                "photo1.jpg", "image-data-1",
                "photo2.png", "image-data-2",
                "readme.txt", "not-an-image"
            );
            MockMultipartFile file = new MockMultipartFile(
                "archive", "test.zip", "application/zip", zipBytes);

            ExtractionResult result = service.extractImages(file);
            try {
                assertThat(result.imageEntries()).isEqualTo(2);
                assertThat(result.totalEntries()).isEqualTo(3);
                assertThat(result.skippedEntries()).containsExactly("readme.txt");
                assertThat(result.extractedFiles()).hasSize(2);
                assertThat(result.extractedFiles().get(0).originalName()).isEqualTo("photo1.jpg");
                assertThat(result.extractedFiles().get(1).originalName()).isEqualTo("photo2.png");
            } finally {
                service.cleanup(result);
            }
        }

        @Test
        void extractImages_zipWithNestedDirs_extractsFlat() throws IOException {
            byte[] zipBytes = createZipArchive(
                "dir1/subdir/photo.jpg", "data"
            );
            MockMultipartFile file = new MockMultipartFile(
                "archive", "nested.zip", "application/zip", zipBytes);

            ExtractionResult result = service.extractImages(file);
            try {
                assertThat(result.imageEntries()).isEqualTo(1);
                assertThat(result.extractedFiles().get(0).originalName()).isEqualTo("photo.jpg");
            } finally {
                service.cleanup(result);
            }
        }

        @Test
        void extractImages_zipSkipsMacOsJunk() throws IOException {
            byte[] zipBytes = createZipArchive(
                "__MACOSX/._photo.jpg", "junk",
                ".DS_Store", "junk",
                "Thumbs.db", "junk",
                ".hidden_file.jpg", "data",
                "photo.jpg", "data"
            );
            MockMultipartFile file = new MockMultipartFile(
                "archive", "macos.zip", "application/zip", zipBytes);

            ExtractionResult result = service.extractImages(file);
            try {
                assertThat(result.imageEntries()).isEqualTo(1);
                assertThat(result.extractedFiles().get(0).originalName()).isEqualTo("photo.jpg");
                assertThat(result.skippedEntries()).hasSize(4);
            } finally {
                service.cleanup(result);
            }
        }

        @Test
        void extractImages_emptyZip_returnsEmptyResult() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                // пустой архив
            }
            MockMultipartFile file = new MockMultipartFile(
                "archive", "empty.zip", "application/zip", baos.toByteArray());

            ExtractionResult result = service.extractImages(file);
            try {
                assertThat(result.imageEntries()).isZero();
                assertThat(result.totalEntries()).isZero();
                assertThat(result.extractedFiles()).isEmpty();
            } finally {
                service.cleanup(result);
            }
        }

        @Test
        void extractImages_allSupportedImageFormats() throws IOException {
            byte[] zipBytes = createZipArchive(
                "a.jpg", "d", "b.jpeg", "d", "c.png", "d",
                "d.webp", "d", "e.bmp", "d", "f.tiff", "d"
            );
            MockMultipartFile file = new MockMultipartFile(
                "archive", "formats.zip", "application/zip", zipBytes);

            ExtractionResult result = service.extractImages(file);
            try {
                assertThat(result.imageEntries()).isEqualTo(6);
            } finally {
                service.cleanup(result);
            }
        }

        @Test
        void extractImages_nullFilename_throwsIOException() {
            MockMultipartFile file = new MockMultipartFile(
                "archive", null, "application/zip", new byte[0]);
            assertThatThrownBy(() -> service.extractImages(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Имя файла");
        }

        @Test
        void extractImages_blankFilename_throwsIOException() {
            MockMultipartFile file = new MockMultipartFile(
                "archive", "   ", "application/zip", new byte[0]);
            assertThatThrownBy(() -> service.extractImages(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Имя файла");
        }

        @Test
        void extractImages_unsupportedFormat_throwsIOException() {
            MockMultipartFile file = new MockMultipartFile(
                "archive", "test.rar", "application/x-rar", new byte[0]);
            assertThatThrownBy(() -> service.extractImages(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("RAR");
        }

        @Test
        void extractImages_unknownExtension_throwsIOException() {
            MockMultipartFile file = new MockMultipartFile(
                "archive", "test.xyz", "application/octet-stream", new byte[0]);
            assertThatThrownBy(() -> service.extractImages(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Неподдерживаемый формат");
        }
    }

    // ========================= scanImageNames =========================

    @Nested
    @DisplayName("scanImageNames — ZIP")
    class ScanImageNamesZip {

        @Test
        void scanImageNames_zipWithImages_returnsNames() throws IOException {
            byte[] zipBytes = createZipArchive(
                "photo1.jpg", "data1",
                "photo2.png", "data2",
                "doc.txt", "text"
            );
            MockMultipartFile file = new MockMultipartFile(
                "archive", "test.zip", "application/zip", zipBytes);

            ScanResult result = service.scanImageNames(file);

            assertThat(result.imageEntries()).isEqualTo(2);
            assertThat(result.imageFileNames()).containsExactly("photo1.jpg", "photo2.png");
            assertThat(result.totalEntries()).isEqualTo(3);
            assertThat(result.skippedEntries()).containsExactly("doc.txt");
        }

        @Test
        void scanImageNames_nullFilename_throwsIOException() {
            MockMultipartFile file = new MockMultipartFile(
                "archive", null, "application/zip", new byte[0]);
            assertThatThrownBy(() -> service.scanImageNames(file))
                .isInstanceOf(IOException.class);
        }

        @Test
        void scanImageNames_skipsMacOsJunk() throws IOException {
            byte[] zipBytes = createZipArchive(
                "__MACOSX/._photo.jpg", "junk",
                ".DS_Store", "junk",
                "real.jpg", "data"
            );
            MockMultipartFile file = new MockMultipartFile(
                "archive", "test.zip", "application/zip", zipBytes);

            ScanResult result = service.scanImageNames(file);
            assertThat(result.imageFileNames()).containsExactly("real.jpg");
        }
    }

    // ========================= cleanup =========================

    @Nested
    @DisplayName("cleanup")
    class Cleanup {

        @Test
        void cleanup_nullResult_doesNotThrow() {
            service.cleanup(null);
        }

        @Test
        void cleanup_nullExtractedFiles_doesNotThrow() {
            ExtractionResult result = new ExtractionResult(null, 0, 0, null);
            service.cleanup(result);
        }

        @Test
        void cleanup_withFiles_deletesAll() throws IOException {
            byte[] zipBytes = createZipArchive("photo.jpg", "data");
            MockMultipartFile file = new MockMultipartFile(
                "archive", "test.zip", "application/zip", zipBytes);

            ExtractionResult result = service.extractImages(file);
            assertThat(result.extractedFiles()).isNotEmpty();
            Path tempFile = result.extractedFiles().get(0).tempFile();
            assertThat(tempFile).exists();

            service.cleanup(result);
            assertThat(tempFile).doesNotExist();
        }
    }

    // ========================= extractImages — TAR =========================

    @Nested
    @DisplayName("extractImages — TAR")
    class ExtractImagesTar {

        @Test
        void extractImages_tarWithImages_extractsAll() throws IOException {
            byte[] tarBytes = createTarArchive("photo1.jpg", "img-data-1", "readme.txt", "text");
            MockMultipartFile file = new MockMultipartFile(
                "archive", "test.tar", "application/x-tar", tarBytes);

            ExtractionResult result = service.extractImages(file);
            try {
                assertThat(result.imageEntries()).isEqualTo(1);
                assertThat(result.totalEntries()).isEqualTo(2);
                assertThat(result.skippedEntries()).containsExactly("readme.txt");
            } finally {
                service.cleanup(result);
            }
        }

        @Test
        void extractImages_tarGzWithImages_extractsAll() throws IOException {
            byte[] tarGzBytes = createTarGzArchive("photo.png", "img-data");
            MockMultipartFile file = new MockMultipartFile(
                "archive", "test.tar.gz", "application/gzip", tarGzBytes);

            ExtractionResult result = service.extractImages(file);
            try {
                assertThat(result.imageEntries()).isEqualTo(1);
                assertThat(result.extractedFiles().get(0).originalName()).isEqualTo("photo.png");
            } finally {
                service.cleanup(result);
            }
        }

        @Test
        void extractImages_tgzWithImages_extractsAll() throws IOException {
            byte[] tarGzBytes = createTarGzArchive("a.jpg", "data", "b.bmp", "data2");
            MockMultipartFile file = new MockMultipartFile(
                "archive", "test.tgz", "application/gzip", tarGzBytes);

            ExtractionResult result = service.extractImages(file);
            try {
                assertThat(result.imageEntries()).isEqualTo(2);
            } finally {
                service.cleanup(result);
            }
        }

        @Test
        void extractImages_tarWithNestedDirs_extractsFlat() throws IOException {
            byte[] tarBytes = createTarArchive("dir1/subdir/nested.jpg", "data");
            MockMultipartFile file = new MockMultipartFile(
                "archive", "nested.tar", "application/x-tar", tarBytes);

            ExtractionResult result = service.extractImages(file);
            try {
                assertThat(result.imageEntries()).isEqualTo(1);
                assertThat(result.extractedFiles().get(0).originalName()).isEqualTo("nested.jpg");
            } finally {
                service.cleanup(result);
            }
        }

        @Test
        void extractImages_tarSkipsMacOsJunk() throws IOException {
            byte[] tarBytes = createTarArchive(
                "__MACOSX/._photo.jpg", "junk",
                ".DS_Store", "junk",
                "photo.jpg", "data"
            );
            MockMultipartFile file = new MockMultipartFile(
                "archive", "macos.tar", "application/x-tar", tarBytes);

            ExtractionResult result = service.extractImages(file);
            try {
                assertThat(result.imageEntries()).isEqualTo(1);
                assertThat(result.extractedFiles().get(0).originalName()).isEqualTo("photo.jpg");
            } finally {
                service.cleanup(result);
            }
        }
    }

    // ========================= scanImageNames — TAR =========================

    @Nested
    @DisplayName("scanImageNames — TAR")
    class ScanImageNamesTar {

        @Test
        void scanImageNames_tarWithImages_returnsNames() throws IOException {
            byte[] tarBytes = createTarArchive("photo.jpg", "data", "doc.pdf", "text");
            MockMultipartFile file = new MockMultipartFile(
                "archive", "test.tar", "application/x-tar", tarBytes);

            ScanResult result = service.scanImageNames(file);

            assertThat(result.imageFileNames()).containsExactly("photo.jpg");
            assertThat(result.skippedEntries()).containsExactly("doc.pdf");
        }

        @Test
        void scanImageNames_tarGz_returnsNames() throws IOException {
            byte[] tarGzBytes = createTarGzArchive("img.png", "data");
            MockMultipartFile file = new MockMultipartFile(
                "archive", "test.tar.gz", "application/gzip", tarGzBytes);

            ScanResult result = service.scanImageNames(file);
            assertThat(result.imageFileNames()).containsExactly("img.png");
        }
    }

    // ========================= extractImages — Windows backslash paths =========================

    @Nested
    @DisplayName("extractImages — Windows paths")
    class WindowsPaths {

        @Test
        void extractImages_zipWithBackslashPaths_extractsFileName() throws IOException {
            byte[] zipBytes = createZipArchive("dir1\\subdir\\photo.jpg", "data");
            MockMultipartFile file = new MockMultipartFile(
                "archive", "win.zip", "application/zip", zipBytes);

            ExtractionResult result = service.extractImages(file);
            try {
                assertThat(result.imageEntries()).isEqualTo(1);
                assertThat(result.extractedFiles().get(0).originalName()).isEqualTo("photo.jpg");
            } finally {
                service.cleanup(result);
            }
        }
    }

    // ========================= helpers =========================

    /**
     * Создаёт ZIP-архив в памяти из пар (имя_файла, содержимое).
     */
    private byte[] createZipArchive(String... nameContentPairs) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (int i = 0; i < nameContentPairs.length; i += 2) {
                String name = nameContentPairs[i];
                String content = nameContentPairs[i + 1];
                zos.putNextEntry(new ZipEntry(name));
                zos.write(content.getBytes());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    /**
     * Создаёт TAR-архив в памяти из пар (имя_файла, содержимое).
     */
    private byte[] createTarArchive(String... nameContentPairs) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (org.apache.commons.compress.archivers.tar.TarArchiveOutputStream tos =
                new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(baos)) {
            tos.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX);
            for (int i = 0; i < nameContentPairs.length; i += 2) {
                String name = nameContentPairs[i];
                byte[] content = nameContentPairs[i + 1].getBytes();
                org.apache.commons.compress.archivers.tar.TarArchiveEntry entry =
                    new org.apache.commons.compress.archivers.tar.TarArchiveEntry(name);
                entry.setSize(content.length);
                tos.putArchiveEntry(entry);
                tos.write(content);
                tos.closeArchiveEntry();
            }
        }
        return baos.toByteArray();
    }

    /**
     * Создаёт TAR.GZ архив в памяти из пар (имя_файла, содержимое).
     */
    private byte[] createTarGzArchive(String... nameContentPairs) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(baos);
             org.apache.commons.compress.archivers.tar.TarArchiveOutputStream tos =
                new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(gzos)) {
            tos.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX);
            for (int i = 0; i < nameContentPairs.length; i += 2) {
                String name = nameContentPairs[i];
                byte[] content = nameContentPairs[i + 1].getBytes();
                org.apache.commons.compress.archivers.tar.TarArchiveEntry entry =
                    new org.apache.commons.compress.archivers.tar.TarArchiveEntry(name);
                entry.setSize(content.length);
                tos.putArchiveEntry(entry);
                tos.write(content);
                tos.closeArchiveEntry();
            }
        }
        return baos.toByteArray();
    }
}
