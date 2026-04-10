package com.wpw.pim.service.media;

import com.wpw.pim.domain.enums.FileType;
import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.media.MediaImageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link ProductMediaService}.
 * Покрывают получение, удаление изображений и проверки принадлежности.
 */
@ExtendWith(MockitoExtension.class)
class ProductMediaServiceTest {

    @Mock
    private ProductRepository productRepo;

    @Mock
    private MediaFileRepository mediaFileRepo;

    @InjectMocks
    private ProductMediaService productMediaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productMediaService, "mediaBasePath", "/tmp/test-media");
        ReflectionTestUtils.setField(productMediaService, "mediaBaseUrl", "/media/products");
    }

    // ========================= getImages =========================

    @Nested
    @DisplayName("getImages")
    class GetImages {

        @Test
        void getImages_productExists_returnsSortedImages() {
            UUID productId = UUID.randomUUID();
            when(productRepo.existsById(productId)).thenReturn(true);

            MediaFile mf1 = buildMediaFile(productId, "/media/img1.webp", 0);
            MediaFile mf2 = buildMediaFile(productId, "/media/img2.webp", 1);
            when(mediaFileRepo.findByProductIdOrderBySortOrder(productId))
                .thenReturn(List.of(mf1, mf2));

            List<MediaImageDto> result = productMediaService.getImages(productId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).url()).isEqualTo("/media/img1.webp");
            assertThat(result.get(1).sortOrder()).isEqualTo(1);
        }

        @Test
        void getImages_productNotFound_throws404() {
            UUID productId = UUID.randomUUID();
            when(productRepo.existsById(productId)).thenReturn(false);

            assertThatThrownBy(() -> productMediaService.getImages(productId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product not found");
        }

        @Test
        void getImages_noMedia_returnsEmptyList() {
            UUID productId = UUID.randomUUID();
            when(productRepo.existsById(productId)).thenReturn(true);
            when(mediaFileRepo.findByProductIdOrderBySortOrder(productId))
                .thenReturn(List.of());

            List<MediaImageDto> result = productMediaService.getImages(productId);
            assertThat(result).isEmpty();
        }
    }

    // ========================= addImages =========================

    @Nested
    @DisplayName("addImages")
    class AddImages {

        @Test
        void addImages_productNotFound_throws404() {
            UUID productId = UUID.randomUUID();
            when(productRepo.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productMediaService.addImages(productId, new org.springframework.mock.web.MockMultipartFile[0]))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product not found");
        }

        @Test
        void addImages_webpFile_copiedWithoutConversion() throws Exception {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setToolNo("TEST-001");

            when(productRepo.findById(productId)).thenReturn(Optional.of(product));
            when(mediaFileRepo.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mediaFileRepo.findByProductIdOrderBySortOrder(productId)).thenReturn(List.of());

            // Используем временную директорию для реальной записи файлов
            java.nio.file.Path tmpMedia = java.nio.file.Files.createTempDirectory("test-media");
            ReflectionTestUtils.setField(productMediaService, "mediaBasePath", tmpMedia.toString());

            org.springframework.mock.web.MockMultipartFile webpFile =
                new org.springframework.mock.web.MockMultipartFile(
                    "files", "TEST-001.webp", "image/webp", new byte[]{1, 2, 3, 4});

            java.util.List<com.wpw.pim.web.dto.media.MediaImageDto> result =
                productMediaService.addImages(productId, new org.springframework.web.multipart.MultipartFile[]{webpFile});

            verify(mediaFileRepo).save(any(MediaFile.class));

            // Cleanup
            java.nio.file.Files.walk(tmpMedia)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { java.nio.file.Files.delete(p); } catch (Exception ignored) {} });
        }

        @Test
        void addImages_blankFilename_skipped() throws Exception {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setToolNo("TEST-002");

            when(productRepo.findById(productId)).thenReturn(Optional.of(product));
            when(mediaFileRepo.findByProductIdOrderBySortOrder(productId)).thenReturn(List.of());

            java.nio.file.Path tmpMedia = java.nio.file.Files.createTempDirectory("test-media");
            ReflectionTestUtils.setField(productMediaService, "mediaBasePath", tmpMedia.toString());

            org.springframework.mock.web.MockMultipartFile blankFile =
                new org.springframework.mock.web.MockMultipartFile(
                    "files", "", "image/jpeg", new byte[]{1});

            productMediaService.addImages(productId, new org.springframework.web.multipart.MultipartFile[]{blankFile});

            verify(mediaFileRepo, never()).save(any(MediaFile.class));

            java.nio.file.Files.walk(tmpMedia)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { java.nio.file.Files.delete(p); } catch (Exception ignored) {} });
        }

        @Test
        void addImages_unsupportedExtension_skipped() throws Exception {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setToolNo("TEST-003");

            when(productRepo.findById(productId)).thenReturn(Optional.of(product));
            when(mediaFileRepo.findByProductIdOrderBySortOrder(productId)).thenReturn(List.of());

            java.nio.file.Path tmpMedia = java.nio.file.Files.createTempDirectory("test-media");
            ReflectionTestUtils.setField(productMediaService, "mediaBasePath", tmpMedia.toString());

            org.springframework.mock.web.MockMultipartFile txtFile =
                new org.springframework.mock.web.MockMultipartFile(
                    "files", "readme.txt", "text/plain", new byte[]{1});

            productMediaService.addImages(productId, new org.springframework.web.multipart.MultipartFile[]{txtFile});

            verify(mediaFileRepo, never()).save(any(MediaFile.class));

            java.nio.file.Files.walk(tmpMedia)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { java.nio.file.Files.delete(p); } catch (Exception ignored) {} });
        }

        @Test
        void addImages_multipleWebpFiles_allSaved() throws Exception {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setToolNo("TEST-MULTI");

            when(productRepo.findById(productId)).thenReturn(Optional.of(product));
            when(mediaFileRepo.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mediaFileRepo.findByProductIdOrderBySortOrder(productId)).thenReturn(List.of());

            java.nio.file.Path tmpMedia = java.nio.file.Files.createTempDirectory("test-media");
            ReflectionTestUtils.setField(productMediaService, "mediaBasePath", tmpMedia.toString());

            org.springframework.mock.web.MockMultipartFile f1 =
                new org.springframework.mock.web.MockMultipartFile("files", "a.webp", "image/webp", new byte[]{1});
            org.springframework.mock.web.MockMultipartFile f2 =
                new org.springframework.mock.web.MockMultipartFile("files", "b.webp", "image/webp", new byte[]{2});

            productMediaService.addImages(productId,
                new org.springframework.web.multipart.MultipartFile[]{f1, f2});

            verify(mediaFileRepo, times(2)).save(any(MediaFile.class));

            java.nio.file.Files.walk(tmpMedia)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { java.nio.file.Files.delete(p); } catch (Exception ignored) {} });
        }
    }

    // ========================= deleteImage =========================

    @Nested
    @DisplayName("deleteImage")
    class DeleteImage {

        @Test
        void deleteImage_validOwnership_deletesAndReturnsUpdatedList() {
            UUID productId = UUID.randomUUID();
            UUID imageId = UUID.randomUUID();

            when(productRepo.existsById(productId)).thenReturn(true);

            Product product = new Product();
            product.setId(productId);

            MediaFile mf = new MediaFile();
            mf.setId(imageId);
            mf.setUrl("/media/products/DR001/1.webp");
            mf.setProduct(product);

            when(mediaFileRepo.findById(imageId)).thenReturn(Optional.of(mf));
            when(mediaFileRepo.findByProductIdOrderBySortOrder(productId)).thenReturn(List.of());

            List<MediaImageDto> result = productMediaService.deleteImage(productId, imageId);

            verify(mediaFileRepo).delete(mf);
            verify(mediaFileRepo).flush();
            assertThat(result).isEmpty();
        }

        @Test
        void deleteImage_imageNotFound_throws404() {
            UUID productId = UUID.randomUUID();
            UUID imageId = UUID.randomUUID();
            when(productRepo.existsById(productId)).thenReturn(true);
            when(mediaFileRepo.findById(imageId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productMediaService.deleteImage(productId, imageId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Image not found");
        }

        @Test
        void deleteImage_imageBelongsToDifferentProduct_throws403() {
            UUID productId = UUID.randomUUID();
            UUID imageId = UUID.randomUUID();
            UUID otherProductId = UUID.randomUUID();

            when(productRepo.existsById(productId)).thenReturn(true);

            Product otherProduct = new Product();
            otherProduct.setId(otherProductId);

            MediaFile mf = new MediaFile();
            mf.setId(imageId);
            mf.setProduct(otherProduct);

            when(mediaFileRepo.findById(imageId)).thenReturn(Optional.of(mf));

            assertThatThrownBy(() -> productMediaService.deleteImage(productId, imageId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");
        }

        @Test
        void deleteImage_imageWithNullProduct_throws403() {
            UUID productId = UUID.randomUUID();
            UUID imageId = UUID.randomUUID();

            when(productRepo.existsById(productId)).thenReturn(true);

            MediaFile mf = new MediaFile();
            mf.setId(imageId);
            mf.setProduct(null);

            when(mediaFileRepo.findById(imageId)).thenReturn(Optional.of(mf));

            assertThatThrownBy(() -> productMediaService.deleteImage(productId, imageId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");
        }

        @Test
        void deleteImage_productNotFound_throws404() {
            UUID productId = UUID.randomUUID();
            UUID imageId = UUID.randomUUID();
            when(productRepo.existsById(productId)).thenReturn(false);

            assertThatThrownBy(() -> productMediaService.deleteImage(productId, imageId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product not found");
        }
    }

    // ========================= helpers =========================

    private MediaFile buildMediaFile(UUID productId, String url, int sortOrder) {
        MediaFile mf = new MediaFile();
        mf.setId(UUID.randomUUID());
        mf.setUrl(url);
        mf.setSortOrder(sortOrder);
        mf.setFileType(FileType.image);
        return mf;
    }
}
