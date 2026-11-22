package com.resolveit.web.controller;

import com.resolveit.domain.user.PortalUser;
import com.resolveit.security.CurrentUser;
import com.resolveit.repository.SystemSettingRepository;
import com.resolveit.service.CatalogService;
import com.resolveit.service.SettingsService;
import com.resolveit.web.dto.HealthDto;
import com.resolveit.web.dto.ServiceDto;
import com.resolveit.web.dto.SupportContactDto;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Health, public service catalog and help-desk contact. */
@RestController
@RequestMapping("/api")
public class SystemController {
    private final CatalogService catalog;
    private final SettingsService settings;
    private final SystemSettingRepository settingRows;

    public SystemController(CatalogService catalog, SettingsService settings, SystemSettingRepository settingRows) {
        this.catalog = catalog;
        this.settings = settings;
        this.settingRows = settingRows;
    }

    @GetMapping("/health")
    public HealthDto health() {
        String database;
        try {
            database = settingRows.count() >= 0 ? "UP" : "DOWN";
        } catch (RuntimeException e) {
            database = "DOWN";
        }
        return new HealthDto("UP", "ResolveIT API", database, Instant.now());
    }

    @GetMapping("/catalog/services")
    public List<ServiceDto> services() { return catalog.catalog(); }

    @GetMapping("/catalog/categories")
    public List<String> categories() { return catalog.categoryNames(); }

    @GetMapping("/support/contact")
    public SupportContactDto support(@CurrentUser PortalUser user) { return settings.supportContact(); }
}
