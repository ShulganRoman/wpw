package com.wpw.pim.repository.media;

import com.wpw.pim.domain.media.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    List<MediaFile> findByProductIdOrderBySortOrder(UUID productId);
    List<MediaFile> findByGroupIdOrderBySortOrder(UUID groupId);
    List<MediaFile> findByCategoryIdOrderBySortOrder(UUID categoryId);
}
