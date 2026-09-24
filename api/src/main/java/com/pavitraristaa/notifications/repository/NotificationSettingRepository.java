package com.pavitraristaa.notifications.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.notifications.entity.NotificationSetting;
import com.pavitraristaa.notifications.entity.NotificationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    List<NotificationSetting> findByUser(UserAccount user);

    Optional<NotificationSetting> findByUserAndNotificationType(UserAccount user, NotificationType notificationType);
}
