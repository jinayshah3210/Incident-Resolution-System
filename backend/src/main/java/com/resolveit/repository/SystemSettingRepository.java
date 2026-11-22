package com.resolveit.repository;

import com.resolveit.domain.settings.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository pattern: key/value settings. */
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> { }
