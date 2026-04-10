package com.wpw.pim.service.audit;

import com.wpw.pim.domain.audit.ContentAuditLog;
import com.wpw.pim.repository.audit.ContentAuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private ContentAuditLogRepository auditLogRepository;

    @InjectMocks private AuditService auditService;

    @Test
    @DisplayName("log() creates ContentAuditLog entry and saves it")
    void log_savesAuditEntry() {
        UUID entityId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("field", "name", "oldValue", "A", "newValue", "B");

        auditService.log("Product", entityId, "UPDATE", "admin", payload);

        ArgumentCaptor<ContentAuditLog> captor = ArgumentCaptor.forClass(ContentAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        ContentAuditLog saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo("Product");
        assertThat(saved.getEntityId()).isEqualTo(entityId);
        assertThat(saved.getAction()).isEqualTo("UPDATE");
        assertThat(saved.getChangedBy()).isEqualTo("admin");
        assertThat(saved.getPayload()).containsEntry("field", "name");
    }

    @Test
    @DisplayName("log() handles null payload")
    void log_handlesNullPayload() {
        UUID entityId = UUID.randomUUID();

        auditService.log("Product", entityId, "DELETE", "system", null);

        ArgumentCaptor<ContentAuditLog> captor = ArgumentCaptor.forClass(ContentAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        ContentAuditLog saved = captor.getValue();
        assertThat(saved.getPayload()).isNull();
        assertThat(saved.getAction()).isEqualTo("DELETE");
    }

    @Test
    @DisplayName("log() sets changedAt to non-null")
    void log_setsChangedAt() {
        auditService.log("Product", UUID.randomUUID(), "CREATE", "admin", Map.of());

        ArgumentCaptor<ContentAuditLog> captor = ArgumentCaptor.forClass(ContentAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getChangedAt()).isNotNull();
    }
}
