package com.wpw.pim.repository.notification;

import com.wpw.pim.domain.notification.EmailOutbox;
import com.wpw.pim.domain.notification.EmailOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, UUID> {

    @Query("""
        SELECT e FROM EmailOutbox e
        WHERE e.status = com.wpw.pim.domain.notification.EmailOutboxStatus.PENDING
          AND e.nextAttemptAt <= :now
        ORDER BY e.nextAttemptAt ASC
        """)
    List<EmailOutbox> findDueForRetry(@Param("now") OffsetDateTime now, Pageable pageable);

    List<EmailOutbox> findByStatusOrderByCreatedAtDesc(EmailOutboxStatus status, Pageable pageable);
}
