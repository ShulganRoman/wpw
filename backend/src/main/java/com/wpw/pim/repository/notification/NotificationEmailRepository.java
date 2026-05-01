package com.wpw.pim.repository.notification;

import com.wpw.pim.domain.notification.NotificationEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationEmailRepository extends JpaRepository<NotificationEmail, UUID> {

    List<NotificationEmail> findByActiveTrue();
}
