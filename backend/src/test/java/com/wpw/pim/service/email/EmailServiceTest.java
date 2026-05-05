package com.wpw.pim.service.email;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.notification.NotificationEmail;
import com.wpw.pim.domain.order.Order;
import com.wpw.pim.domain.order.OrderItem;
import com.wpw.pim.domain.order.OrderStatus;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.notification.NotificationEmailRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private NotificationEmailRepository notificationEmailRepository;
    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private OrderExcelExporter orderExcelExporter;

    @InjectMocks
    private EmailService service;

    @BeforeEach
    void injectValues() throws Exception {
        var fromField = EmailService.class.getDeclaredField("fromAddress");
        fromField.setAccessible(true);
        fromField.set(service, "from@test.com");

        var baseUrlField = EmailService.class.getDeclaredField("exportBaseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(service, "https://example.com");
    }

    private NotificationEmail recipient(String email, boolean active) {
        NotificationEmail n = new NotificationEmail();
        n.setEmail(email);
        n.setActive(active);
        return n;
    }

    private Order buildOrder() {
        Dealer d = new Dealer();
        d.setId(UUID.randomUUID());
        d.setName("Acme");

        Order o = new Order();
        o.setId(UUID.randomUUID());
        o.setDealer(d);
        o.setCurrency("USD");
        o.setTotal(new BigDecimal("100.00"));
        o.setStatus(OrderStatus.SUBMITTED);

        OrderItem item = new OrderItem();
        item.setOrder(o);
        item.setProductId(UUID.randomUUID());
        item.setToolNo("T001");
        item.setName("Tool 1");
        item.setQty(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setLineTotal(new BigDecimal("100.00"));
        o.getItems().add(item);
        return o;
    }

    @Nested
    @DisplayName("sendOrderSubmittedToAdmins")
    class OrderSubmitted {

        @BeforeEach
        void stubMailSender() {
            lenient().when(orderExcelExporter.export(any(), any())).thenReturn(new byte[0]);
            lenient().when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        }

        @Test
        @DisplayName("sends mime email with attachment if active recipients exist")
        void sendsWhenRecipientsExist() {
            when(notificationEmailRepository.findByActiveTrue())
                .thenReturn(List.of(recipient("admin@example.com", true)));

            service.sendOrderSubmittedToAdmins(buildOrder());

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("sends nothing if no active recipients")
        void noRecipients() {
            when(notificationEmailRepository.findByActiveTrue()).thenReturn(List.of());

            service.sendOrderSubmittedToAdmins(buildOrder());

            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("mailSender error is not propagated")
        void swallowsException() {
            when(notificationEmailRepository.findByActiveTrue())
                .thenReturn(List.of(recipient("admin@example.com", true)));
            doThrow(new RuntimeException("smtp down"))
                .when(mailSender).send(any(MimeMessage.class));

            assertThatCode(() -> service.sendOrderSubmittedToAdmins(buildOrder()))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("sendStatusChangedToDealer")
    class StatusChanged {

        @Test
        @DisplayName("sends email with valid email")
        void sendsValid() {
            service.sendStatusChangedToDealer(buildOrder(), "dealer@example.com");

            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("does not send email if email == null")
        void nullEmail() {
            service.sendStatusChangedToDealer(buildOrder(), null);

            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("does not send email if email is empty")
        void blankEmail() {
            service.sendStatusChangedToDealer(buildOrder(), "  ");

            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("mailSender error is not propagated")
        void swallowsException() {
            doThrow(new RuntimeException("smtp down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

            assertThatCode(() -> service.sendStatusChangedToDealer(buildOrder(), "dealer@example.com"))
                .doesNotThrowAnyException();
        }
    }
}
