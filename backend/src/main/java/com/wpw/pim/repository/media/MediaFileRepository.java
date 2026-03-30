package com.wpw.pim.repository.media;

import com.wpw.pim.domain.media.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    List<MediaFile> findByProductIdOrderBySortOrder(UUID productId);
    List<MediaFile> findByGroupIdOrderBySortOrder(UUID groupId);
    List<MediaFile> findByCategoryIdOrderBySortOrder(UUID categoryId);

    @Query("SELECT m.url FROM MediaFile m WHERE m.product.id IN :productIds")
    Set<String> findUrlsByProductIds(Set<UUID> productIds);
}
