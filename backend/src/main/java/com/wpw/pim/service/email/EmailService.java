package com.wpw.pim.service.email;

import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.domain.order.Order;
import com.wpw.pim.domain.order.OrderItem;
import com.wpw.pim.domain.order.OrderStatus;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.notification.NotificationEmailRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationEmailRepository notificationEmailRepository;
    private final MediaFileRepository mediaFileRepository;
    private final OrderExcelExporter orderExcelExporter;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${pim.export.base-url}")
    private String exportBaseUrl;

    @Async
    public void sendOrderSubmittedToAdmins(Order order) {
        List<String> recipients = notificationEmailRepository.findByActiveTrue()
            .stream().map(e -> e.getEmail()).toList();
        if (recipients.isEmpty()) return;

        String dealerName = order.getDealer().getName();
        String subject = "New order from dealer: " + dealerName;
        StringBuilder body = new StringBuilder(String.format(
            "Dealer \"%s\" submitted order #%s for %s %s.%n%nItems: %d%nRequires processing in the admin panel.",
            dealerName,
            order.getId(),
            order.getTotal().toPlainString(),
            order.getCurrency(),
            order.getItems().size()
        ));
        if (order.getComment() != null && !order.getComment().isBlank()) {
            body.append(String.format("%n%nComment from dealer:%n%s", order.getComment()));
        }

        Map<UUID, String> imageUrls = resolveImageUrls(order);
        byte[] excel = orderExcelExporter.export(order, imageUrls);
        String filename = buildFilename(dealerName, order);
        sendWithAttachment(recipients, subject, body.toString(), excel, filename);
    }

    @Async
    public void sendOrderSubmittedToDealer(Order order, String dealerEmail) {
        if (dealerEmail == null || dealerEmail.isBlank()) return;

        String subject = "Your order has been submitted — #" + order.getId().toString().substring(0, 8).toUpperCase();
        StringBuilder body = new StringBuilder(String.format(
            "Your order has been successfully submitted.%n%n" +
            "Order: #%s%n" +
            "Total: %s %s%n" +
            "Items: %d%n%n" +
            "We will notify you when the status changes.",
            order.getId(),
            order.getTotal().toPlainString(),
            order.getCurrency(),
            order.getItems().size()
        ));
        if (order.getComment() != null && !order.getComment().isBlank()) {
            body.append(String.format("%n%nYour comment:%n%s", order.getComment()));
        }
        send(List.of(dealerEmail), subject, body.toString());
    }

    @Async
    public void sendStatusChangedToDealer(Order order, String dealerEmail) {
        if (dealerEmail == null || dealerEmail.isBlank()) return;

        String statusLabel = dealerStatusLabel(order.getStatus());
        String subject = "Your order status has changed: " + statusLabel;
        String body = String.format(
            "Your order #%s status has been changed to: %s.%n%n" +
            "Order total: %s %s",
            order.getId(),
            statusLabel,
            order.getTotal().toPlainString(),
            order.getCurrency()
        );
        send(List.of(dealerEmail), subject, body);
    }

    private void send(List<String> to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to.toArray(String[]::new));
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private void sendWithAttachment(List<String> to, String subject, String body, byte[] attachment, String filename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to.toArray(String[]::new));
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(filename, new ByteArrayDataSource(attachment,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
        }
    }

    private Map<UUID, String> resolveImageUrls(Order order) {
        List<UUID> productIds = order.getItems().stream()
            .map(OrderItem::getProductId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        if (productIds.isEmpty()) return Collections.emptyMap();
        return mediaFileRepository.findByProductIds(productIds).stream()
            .collect(Collectors.toMap(
                m -> m.getProduct().getId(),
                m -> exportBaseUrl + m.getUrl(),
                (first, second) -> first // keep primary image (lowest sort_order)
            ));
    }

    private String buildFilename(String dealerName, Order order) {
        String safeName = dealerName.replaceAll("[^A-Za-z0-9_\\-]", "_");
        return safeName + "_order_" + order.getId() + ".xlsx";
    }

    private String dealerStatusLabel(OrderStatus status) {
        return switch (status) {
            case SUBMITTED    -> "Submitted";
            case IN_PROCESSING -> "In Processing";
            case CONFIRMED    -> "Confirmed";
            case REJECTED     -> "Rejected";
        };
    }
}
