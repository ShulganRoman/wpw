package com.wpw.pim.service.email;

import com.wpw.pim.domain.notification.EmailOutbox;
import com.wpw.pim.domain.notification.EmailOutboxStatus;
import com.wpw.pim.repository.notification.EmailOutboxRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailOutboxDispatcherTest {

    @Mock private EmailOutboxRepository repository;
    @Mock private JavaMailSender mailSender;

    @InjectMocks
    private EmailOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() throws Exception {
        var f = EmailOutboxDispatcher.class.getDeclaredField("fromAddress");
        f.setAccessible(true);
        f.set(dispatcher, "from@test.com");

        lenient().when(repository.save(any(EmailOutbox.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(mailSender.createMimeMessage())
            .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    private EmailOutbox pending(int attempts) {
        EmailOutbox e = new EmailOutbox();
        e.setId(UUID.randomUUID());
        e.setRecipients("user@example.com");
        e.setSubject("Hi");
        e.setBody("Body");
        e.setStatus(EmailOutboxStatus.PENDING);
        e.setAttempts(attempts);
        e.setNextAttemptAt(OffsetDateTime.now());
        return e;
    }

    @Test
    @DisplayName("successful delivery marks row as SENT and clears last_error")
    void marksSent() {
        EmailOutbox row = pending(0);
        row.setLastError("previous");
        when(repository.findById(row.getId())).thenReturn(Optional.of(row));

        dispatcher.deliverOne(row.getId());

        verify(mailSender).send(any(SimpleMailMessage.class));
        ArgumentCaptor<EmailOutbox> captor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(repository).save(captor.capture());
        EmailOutbox saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(EmailOutboxStatus.SENT);
        assertThat(saved.getSentAt()).isNotNull();
        assertThat(saved.getLastError()).isNull();
    }

    @Test
    @DisplayName("smtp error: stays PENDING and schedules retry while attempts < MAX")
    void schedulesRetry() {
        EmailOutbox row = pending(0);
        when(repository.findById(row.getId())).thenReturn(Optional.of(row));
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> dispatcher.deliverOne(row.getId()))
            .hasMessageContaining("smtp down");

        ArgumentCaptor<EmailOutbox> captor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(repository).save(captor.capture());
        EmailOutbox saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
        assertThat(saved.getAttempts()).isEqualTo(1);
        assertThat(saved.getLastError()).contains("smtp down");
        assertThat(saved.getNextAttemptAt()).isAfter(OffsetDateTime.now().plusSeconds(30));
    }

    @Test
    @DisplayName("smtp error after MAX_ATTEMPTS-1: marks FAILED")
    void marksFailedAfterMaxAttempts() {
        // After this attempt, attempts==5 == MAX_ATTEMPTS → FAILED
        EmailOutbox row = pending(4);
        when(repository.findById(row.getId())).thenReturn(Optional.of(row));
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> dispatcher.deliverOne(row.getId()));

        ArgumentCaptor<EmailOutbox> captor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EmailOutboxStatus.FAILED);
        assertThat(captor.getValue().getAttempts()).isEqualTo(5);
    }

    @Test
    @DisplayName("non-PENDING row is skipped (idempotent)")
    void skipsAlreadySent() {
        EmailOutbox row = pending(0);
        row.setStatus(EmailOutboxStatus.SENT);
        when(repository.findById(row.getId())).thenReturn(Optional.of(row));

        dispatcher.deliverOne(row.getId());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("message with attachment is sent as MimeMessage")
    void sendsMimeWithAttachment() {
        EmailOutbox row = pending(0);
        row.setAttachment(new byte[]{1, 2, 3});
        row.setAttachmentFilename("order.xlsx");
        row.setAttachmentMime("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        when(repository.findById(row.getId())).thenReturn(Optional.of(row));

        dispatcher.deliverOne(row.getId());

        verify(mailSender).send(any(MimeMessage.class));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
