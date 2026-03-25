package com.wpw.pim.repository.audit;

import com.wpw.pim.domain.audit.ContentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentAuditLogRepository extends JpaRepository<ContentAuditLog, UUID> {
    List<ContentAuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(String entityType, UUID entityId);
}
