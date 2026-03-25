package com.wpw.pim.service.product;

import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.repository.catalog.ProductGroupRepository;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaFallbackService {

    private final MediaFileRepository mediaRepo;
    private final ProductRepository productRepo;
    private final ProductGroupRepository groupRepo;

    @Transactional(readOnly = true)
    public List<MediaFile> getMediaForProduct(UUID productId) {
        List<MediaFile> media = mediaRepo.findByProductIdOrderBySortOrder(productId);
        if (!media.isEmpty()) return media;

        UUID groupId = productRepo.findGroupIdByProductId(productId);
        if (groupId != null) {
            media = mediaRepo.findByGroupIdOrderBySortOrder(groupId);
            if (!media.isEmpty()) return media;

            UUID categoryId = groupRepo.findCategoryIdByGroupId(groupId);
            if (categoryId != null) {
                return mediaRepo.findByCategoryIdOrderBySortOrder(categoryId);
            }
        }
        return List.of();
    }

    public String getThumbnail(List<MediaFile> media) {
        return media.stream()
            .filter(m -> m.getThumbnailUrl() != null)
            .findFirst()
            .map(MediaFile::getThumbnailUrl)
            .orElse(media.isEmpty() ? null : media.get(0).getUrl());
    }
}
