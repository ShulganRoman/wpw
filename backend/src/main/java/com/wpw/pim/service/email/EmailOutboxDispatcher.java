package com.wpw.pim.service.email;

import com.wpw.pim.domain.notification.EmailOutbox;
import com.wpw.pim.domain.notification.EmailOutboxStatus;
import com.wpw.pim.repository.notification.EmailOutboxRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Reads pending rows from email_outbox and delivers them via SMTP.
 *
 * Two entry points:
 *   - {@link #tryDeliverAsync(UUID)} — fired right after enqueue, fast path.
 *   - {@link #processRetries()} — @Scheduled fallback that drains the backlog
 *     after SMTP outages and handles every retry attempt.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailOutboxDispatcher {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 20;

    /** Exponential-ish backoff per attempt index (0-based). */
    private static final Duration[] BACKOFFS = new Duration[] {
        Duration.ofMinutes(1),   // after 1st failure
        Duration.ofMinutes(5),   // after 2nd
        Duration.ofMinutes(15),  // after 3rd
        Duration.ofHours(1),     // after 4th
        Duration.ofHours(6),     // after 5th — but we cap at MAX_ATTEMPTS so unused
    };

    private final EmailOutboxRepository repository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    /** Fast-path delivery right after enqueue. Errors are caught — retry job will pick it up. */
    @Async
    public void tryDeliverAsync(UUID outboxId) {
        try {
            deliverOne(outboxId);
        } catch (Exception e) {
            // deliverOne already logs + persists the failure
            log.debug("Async deliver of {} did not complete: {}", outboxId, e.getMessage());
        }
    }

    /** Polls for pending messages whose next_attempt_at has come due. */
    @Scheduled(fixedDelayString = "${pim.email.outbox.poll-interval-ms:30000}",
               initialDelayString = "${pim.email.outbox.initial-delay-ms:30000}")
    public void processRetries() {
        List<EmailOutbox> due = repository.findDueForRetry(OffsetDateTime.now(),
            PageRequest.of(0, BATCH_SIZE));
        if (due.isEmpty()) return;

        log.info("EmailOutbox: dispatching {} pending message(s)", due.size());
        for (EmailOutbox row : due) {
            try {
                deliverOne(row.getId());
            } catch (Exception ignored) {
                // already logged + persisted
            }
        }
    }

    /**
     * Sends a single outbox row. Each call runs in its own transaction so a
     * failure on one message does not roll back the status update on another.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverOne(UUID outboxId) {
        EmailOutbox row = repository.findById(outboxId).orElse(null);
        if (row == null || row.getStatus() != EmailOutboxStatus.PENDING) return;

        try {
            send(row);
            row.setStatus(EmailOutboxStatus.SENT);
            row.setSentAt(OffsetDateTime.now());
            row.setLastError(null);
            repository.save(row);
        } catch (Exception e) {
            handleFailure(row, e);
            // rethrow only so callers can react; the row is already persisted in this tx
            throw new RuntimeException(e);
        }
    }

    private void send(EmailOutbox row) throws Exception {
        String[] to = Arrays.stream(row.getRecipients().split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        if (to.length == 0) {
            throw new IllegalStateException("No recipients");
        }

        if (row.getAttachment() != null && row.getAttachment().length > 0) {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            if (fromAddress != null && !fromAddress.isBlank()) helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(row.getSubject());
            helper.setText(row.getBody());
            helper.addAttachment(
                row.getAttachmentFilename() != null ? row.getAttachmentFilename() : "attachment.bin",
                new ByteArrayDataSource(row.getAttachment(),
                    row.getAttachmentMime() != null ? row.getAttachmentMime() : "application/octet-stream")
            );
            mailSender.send(msg);
        } else {
            SimpleMailMessage msg = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(row.getSubject());
            msg.setText(row.getBody());
            mailSender.send(msg);
        }
    }

    private void handleFailure(EmailOutbox row, Exception e) {
        int attempts = row.getAttempts() + 1;
        row.setAttempts(attempts);
        row.setLastError(truncate(e.getMessage(), 4000));

        if (attempts >= MAX_ATTEMPTS) {
            row.setStatus(EmailOutboxStatus.FAILED);
            log.error("EmailOutbox {} permanently FAILED after {} attempts: {}",
                row.getId(), attempts, e.getMessage());
        } else {
            Duration delay = BACKOFFS[Math.min(attempts - 1, BACKOFFS.length - 1)];
            row.setNextAttemptAt(OffsetDateTime.now().plus(delay));
            log.warn("EmailOutbox {} attempt {} failed ({}); next retry in {}",
                row.getId(), attempts, e.getMessage(), delay);
        }
        repository.save(row);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
