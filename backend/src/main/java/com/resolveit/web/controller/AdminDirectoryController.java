package com.resolveit.web.controller;

import com.resolveit.domain.user.PortalUser;
import com.resolveit.security.CurrentUser;
import com.resolveit.service.CatalogService;
import com.resolveit.service.SettingsService;
import com.resolveit.service.UserService;
import com.resolveit.web.dto.*;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Administrator management of users, services, categories and configuration. */
@RestController
@RequestMapping("/api/admin")
public class AdminDirectoryController {
    private final UserService users;
    private final CatalogService catalog;
    private final SettingsService settings;

    public AdminDirectoryController(UserService users, CatalogService catalog, SettingsService settings) {
        this.users = users;
        this.catalog = catalog;
        this.settings = settings;
    }

    @GetMapping("/users")
    public List<UserDto> directory(@CurrentUser PortalUser user, @RequestParam(required = false) String role) { return users.directory(user, role); }

    @PostMapping("/users")
    public UserDto create(@CurrentUser PortalUser user, @RequestBody ManagedUserRequest body) { return users.create(user, body); }

    @PutMapping("/users/{id}")
    public UserDto update(@CurrentUser PortalUser user, @PathVariable String id, @RequestBody ManagedUserRequest body) {
        return users.update(user, id, body);
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deactivate(@CurrentUser PortalUser user, @PathVariable String id) {
        users.deactivate(user, id);
        return Map.of("success", true, "id", id);
    }

    @PostMapping("/users/{id}/reactivate")
    public UserDto reactivate(@CurrentUser PortalUser user, @PathVariable String id) { return users.reactivate(user, id); }

    @GetMapping("/services")
    public List<ServiceDto> services(@CurrentUser PortalUser user) { return catalog.adminServices(user); }

    @PostMapping("/services")
    public ServiceDto addService(@CurrentUser PortalUser user, @RequestBody ServiceRequest body) { return catalog.createService(user, body); }

    @GetMapping("/services/{id}")
    public ServiceDto service(@CurrentUser PortalUser user, @PathVariable long id) { return catalog.serviceDetail(user, id); }

    @PutMapping("/services/{id}")
    public ServiceDto editService(@CurrentUser PortalUser user, @PathVariable long id, @RequestBody ServiceRequest body) {
        return catalog.updateService(user, id, body);
    }

    @DeleteMapping("/services/{id}")
    public Map<String, Object> removeService(@CurrentUser PortalUser user, @PathVariable long id) { return catalog.removeService(user, id); }

    @PostMapping("/services/{id}/technicians")
    public ServiceDto link(@CurrentUser PortalUser user, @PathVariable long id, @RequestBody Map<String, String> body) {
        return catalog.addServiceTechnician(user, id, body.get("technicianId"));
    }

    @DeleteMapping("/services/{id}/technicians/{technicianId}")
    public ServiceDto unlink(@CurrentUser PortalUser user, @PathVariable long id, @PathVariable String technicianId) {
        return catalog.removeServiceTechnician(user, id, technicianId);
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories(@CurrentUser PortalUser user) { return catalog.adminCategories(user); }

    @PostMapping("/categories")
    public CategoryDto addCategory(@CurrentUser PortalUser user, @RequestBody CategoryRequest body) { return catalog.createCategory(user, body); }

    @PutMapping("/categories/{id}")
    public CategoryDto editCategory(@CurrentUser PortalUser user, @PathVariable long id, @RequestBody CategoryRequest body) {
        return catalog.updateCategory(user, id, body);
    }

    @DeleteMapping("/categories/{id}")
    public CategoryDto removeCategory(@CurrentUser PortalUser user, @PathVariable long id) { return catalog.deactivateCategory(user, id); }

    @PutMapping("/categories/{id}/keywords")
    public CategoryDto keywords(@CurrentUser PortalUser user, @PathVariable long id, @RequestBody KeywordsRequest body) {
        return catalog.replaceKeywords(user, id, body);
    }

    @GetMapping("/settings")
    public Map<String, String> settings(@CurrentUser PortalUser user) { return settings.all(user); }

    @PutMapping("/settings")
    public Map<String, String> updateSettings(@CurrentUser PortalUser user, @RequestBody Map<String, String> body) {
        return settings.update(user, body);
    }

    @GetMapping("/strategies")
    public Map<String, Map<String, String>> strategies(@CurrentUser PortalUser user) {
        settings.all(user);
        return settings.strategies();
    }
}
