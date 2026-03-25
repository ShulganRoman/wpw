package com.wpw.pim.service.audit;

import com.wpw.pim.domain.audit.ContentAuditLog;
import com.wpw.pim.repository.audit.ContentAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final ContentAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, UUID entityId, String action, String changedBy, Map<String, Object> payload) {
        ContentAuditLog entry = new ContentAuditLog();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setChangedBy(changedBy);
        entry.setPayload(payload);
        auditLogRepository.save(entry);
    }
}
