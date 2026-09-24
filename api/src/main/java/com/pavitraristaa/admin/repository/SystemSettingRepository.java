package com.pavitraristaa.admin.repository;

import com.pavitraristaa.admin.entity.SystemSetting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findBySettingKey(String settingKey);

    List<SystemSetting> findAllByOrderBySettingKeyAsc();
}
