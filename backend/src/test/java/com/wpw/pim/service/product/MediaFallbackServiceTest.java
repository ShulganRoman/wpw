package com.wpw.pim.service.product;

import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.repository.catalog.ProductGroupRepository;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.product.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaFallbackServiceTest {

    @Mock private MediaFileRepository mediaRepo;
    @Mock private ProductRepository productRepo;
    @Mock private ProductGroupRepository groupRepo;

    @InjectMocks private MediaFallbackService mediaFallbackService;

    private MediaFile createMedia(String url, String thumbnailUrl) {
        MediaFile mf = new MediaFile();
        mf.setUrl(url);
        mf.setThumbnailUrl(thumbnailUrl);
        return mf;
    }

    @Nested
    @DisplayName("getMediaForProduct")
    class GetMediaForProduct {

        @Test
        @DisplayName("returns product media when available")
        void returnsProductMedia() {
            UUID productId = UUID.randomUUID();
            MediaFile mf = createMedia("/img/1.webp", "/img/1-thumb.webp");
            when(mediaRepo.findByProductIdOrderBySortOrder(productId)).thenReturn(List.of(mf));

            List<MediaFile> result = mediaFallbackService.getMediaForProduct(productId);

            assertThat(result).hasSize(1);
            verify(productRepo, never()).findGroupIdByProductId(any());
        }

        @Test
        @DisplayName("falls back to group media when product has none")
        void fallsBackToGroupMedia() {
            UUID productId = UUID.randomUUID();
            UUID groupId = UUID.randomUUID();
            MediaFile mf = createMedia("/img/group.webp", null);

            when(mediaRepo.findByProductIdOrderBySortOrder(productId)).thenReturn(List.of());
            when(productRepo.findGroupIdByProductId(productId)).thenReturn(groupId);
            when(mediaRepo.findByGroupIdOrderBySortOrder(groupId)).thenReturn(List.of(mf));

            List<MediaFile> result = mediaFallbackService.getMediaForProduct(productId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUrl()).isEqualTo("/img/group.webp");
        }

        @Test
        @DisplayName("falls back to category media when group has none")
        void fallsBackToCategoryMedia() {
            UUID productId = UUID.randomUUID();
            UUID groupId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            MediaFile mf = createMedia("/img/category.webp", null);

            when(mediaRepo.findByProductIdOrderBySortOrder(productId)).thenReturn(List.of());
            when(productRepo.findGroupIdByProductId(productId)).thenReturn(groupId);
            when(mediaRepo.findByGroupIdOrderBySortOrder(groupId)).thenReturn(List.of());
            when(groupRepo.findCategoryIdByGroupId(groupId)).thenReturn(categoryId);
            when(mediaRepo.findByCategoryIdOrderBySortOrder(categoryId)).thenReturn(List.of(mf));

            List<MediaFile> result = mediaFallbackService.getMediaForProduct(productId);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("returns empty list when no media at any level")
        void returnsEmptyWhenNoMedia() {
            UUID productId = UUID.randomUUID();
            UUID groupId = UUID.randomUUID();

            when(mediaRepo.findByProductIdOrderBySortOrder(productId)).thenReturn(List.of());
            when(productRepo.findGroupIdByProductId(productId)).thenReturn(groupId);
            when(mediaRepo.findByGroupIdOrderBySortOrder(groupId)).thenReturn(List.of());
            when(groupRepo.findCategoryIdByGroupId(groupId)).thenReturn(null);

            List<MediaFile> result = mediaFallbackService.getMediaForProduct(productId);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty list when groupId is null")
        void returnsEmptyWhenNoGroup() {
            UUID productId = UUID.randomUUID();

            when(mediaRepo.findByProductIdOrderBySortOrder(productId)).thenReturn(List.of());
            when(productRepo.findGroupIdByProductId(productId)).thenReturn(null);

            List<MediaFile> result = mediaFallbackService.getMediaForProduct(productId);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getThumbnail")
    class GetThumbnail {

        @Test
        @DisplayName("returns first thumbnail URL when available")
        void returnsThumbnailUrl() {
            MediaFile mf = createMedia("/img/1.webp", "/img/1-thumb.webp");
            String result = mediaFallbackService.getThumbnail(List.of(mf));
            assertThat(result).isEqualTo("/img/1-thumb.webp");
        }

        @Test
        @DisplayName("returns main URL when thumbnail is null")
        void returnsMainUrlWhenNoThumbnail() {
            MediaFile mf = createMedia("/img/1.webp", null);
            String result = mediaFallbackService.getThumbnail(List.of(mf));
            assertThat(result).isEqualTo("/img/1.webp");
        }

        @Test
        @DisplayName("returns null for empty list")
        void returnsNullForEmptyList() {
            String result = mediaFallbackService.getThumbnail(List.of());
            assertThat(result).isNull();
        }
    }
}
