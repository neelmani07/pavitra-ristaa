package com.pavitraristaa.notifications.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.notifications.entity.Notification;
import com.pavitraristaa.notifications.entity.NotificationType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByUuid(UUID uuid);

    Page<Notification> findByUserOrderByCreatedAtDesc(UserAccount user, Pageable pageable);

    Page<Notification> findByUserAndReadFalseOrderByCreatedAtDesc(UserAccount user, Pageable pageable);

    Page<Notification> findByUserAndTypeOrderByCreatedAtDesc(UserAccount user, NotificationType type, Pageable pageable);

    Page<Notification> findByUserAndTypeAndReadFalseOrderByCreatedAtDesc(UserAccount user, NotificationType type, Pageable pageable);

    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = :now where n.user = :user and n.read = false")
    int markAllRead(@Param("user") UserAccount user, @Param("now") java.time.Instant now);
}
