package com.resolveit.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.exception.*;
import com.resolveit.repository.ItServiceRepository;
import com.resolveit.repository.ServiceTechnicianRepository;
import com.resolveit.service.CatalogService;
import com.resolveit.service.ReportService;
import com.resolveit.service.UserService;
import com.resolveit.web.dto.KeywordsRequest;
import com.resolveit.web.dto.ManagedUserRequest;
import com.resolveit.web.dto.ServiceRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Directory, catalog, configuration and reports (ported from the original integration suite). */
class AdminManagementIntegrationTest extends IntegrationTestBase {
    @Autowired UserService userService;
    @Autowired CatalogService catalog;
    @Autowired ReportService reports;
    @Autowired ItServiceRepository services;
    @Autowired ServiceTechnicianRepository links;

    @Test void seededDirectoryAndCatalogAreComplete() {
        assertEquals(4, userService.directory(user("A001"), "ADMIN").size());
        assertEquals(12, userService.directory(user("A001"), "EMPLOYEE").size());
        assertEquals(13, userService.directory(user("A001"), "TECHNICIAN").size());
        assertEquals(9, services.count());
        assertFalse(links.findByService_Id(services.findByNameIgnoreCase("Payment").orElseThrow().getId()).isEmpty());
        assertThrows(UnauthorizedOperationException.class, () -> userService.directory(user("E101"), "ADMIN"));
    }

    @Test void serviceCrudIsDurableAndRejectsDuplicates() {
        var created = catalog.createService(user("A001"), new ServiceRequest("Office Printing", "Printer maintenance and repairs", "Hardware", 2, "printer", null));
        assertThrows(ConflictException.class, () -> catalog.createService(user("A001"), new ServiceRequest("Office Printing", "Printer maintenance and repairs", "Hardware", 2, null, null)));
        catalog.addServiceTechnician(user("A001"), created.id(), "T304");
        assertThrows(ConflictException.class, () -> catalog.addServiceTechnician(user("A001"), created.id(), "T304"));
        catalog.removeServiceTechnician(user("A001"), created.id(), "T304");
        assertTrue(users.existsById("T304"));
        assertEquals(false, catalog.removeService(user("A001"), created.id()).get("archived"));
    }

    @Test void referencedServiceIsArchivedAndCannotBeRenamed() {
        payment();
        long id = services.findByNameIgnoreCase("Payment").orElseThrow().getId();
        assertThrows(ConflictException.class, () -> catalog.updateService(user("A001"), id,
                new ServiceRequest("Payment Renamed", "Payment and checkout services", "Application", 4, null, null)));
        assertEquals(true, catalog.removeService(user("A001"), id).get("archived"));
    }

    @Test void slaSettingsValidateAndExistingDeadlinesStayStable() {
        assertThrows(ValidationException.class, () -> settings.update(user("A001"), Map.of("sla.P1.response.minutes", "0")));
        assertThrows(ValidationException.class, () -> settings.update(user("A001"), Map.of("sla.P1.response.minutes", "40")));
        assertThrows(ValidationException.class, () -> settings.update(user("A001"), Map.of("assignment.strategy", "COIN_FLIP")));
        settings.update(user("A001"), Map.of("sla.P1.response.minutes", "8", "sla.P1.resolution.minutes", "60"));
        Incident i = payment();
        var due = i.getResponseDueAt();
        settings.update(user("A001"), Map.of("sla.P1.response.minutes", "10"));
        assertEquals(due, incidents.findByPublicIdIgnoreCase(i.getPublicId()).orElseThrow().getResponseDueAt());
    }

    @Test void deactivatingATechnicianReassignsTheirOpenWork() {
        Incident i = payment();
        String tech = i.getTechnicianId();
        userService.deactivate(user("A001"), tech);
        assertNotEquals(tech, i.getTechnicianId());
        assertEquals(IncidentStatus.ASSIGNED, i.getStatus());
        assertThrows(InvalidCredentialsException.class, () -> auth.login(tech, "Password@123", "TECHNICIAN"));
        userService.reactivate(user("A001"), tech);
        assertTrue(user(tech).isActive());
    }

    @Test void adminCreatesTechnicianWithValidatedSkills() {
        var made = userService.create(user("A001"), new ManagedUserRequest("TECHNICIAN", "Maya Patel", "maya@example.test", "Secure123",
                "Apps", null, List.of("Application"), 5, 4, false));
        assertTrue(made.id().startsWith("T"));
        assertThrows(ValidationException.class, () -> userService.create(user("A001"), new ManagedUserRequest("TECHNICIAN", "Bad Skill",
                "bad@example.test", "Secure123", "Apps", null, List.of("Juggling"), 5, 1, false)));
    }

    @Test void newCategoryAndKeywordsClassifyWithoutCodeChanges() {
        var category = catalog.createCategory(user("A001"), new com.resolveit.web.dto.CategoryRequest("Facilities", "Building issues", true));
        catalog.replaceKeywords(user("A001"), category.id(), new KeywordsRequest(Map.of("aircon", 5, "elevator", 5)));
        Incident i = create("E101", "Aircon broken in meeting room", "The aircon and elevator are broken on floor two.", null);
        assertEquals("Facilities", i.getCategory());
    }

    @Autowired com.resolveit.seed.DemoIncidentSeeder demoSeeder;

    @Test void novemberDemoSeederBuildsAConsistentMixOfIncidents() {
        int created = demoSeeder.seedIfEmpty();
        assertTrue(created >= 30, "created " + created);
        var all = incidents.findAll();
        assertTrue(all.stream().anyMatch(i -> i.getStatus() == IncidentStatus.CLOSED));
        assertTrue(all.stream().anyMatch(i -> i.getStatus() == IncidentStatus.RESOLVED));
        assertTrue(all.stream().anyMatch(i -> i.getStatus() == IncidentStatus.IN_PROGRESS));
        assertTrue(all.stream().anyMatch(i -> i.getStatus() == IncidentStatus.ASSIGNED));
        assertTrue(violations.count() >= 4, "violations " + violations.count());
        assertTrue(escalations.count() >= 1);
        assertTrue(all.stream().allMatch(i -> i.getCreatedAt().toString().startsWith("2026-11")));
        assertEquals(0, demoSeeder.seedIfEmpty(), "idempotent");
        assertFalse(clock.isPinned());
    }

    @Test void reportsReflectLiveData() {
        payment();
        var overview = reports.overview(user("A001"));
        assertEquals(1, overview.total());
        assertEquals(1, overview.critical());
        assertEquals(1L, overview.services().get("Payment"));
        assertFalse(reports.months(user("A001")).isEmpty());
        assertThrows(ValidationException.class, () -> reports.month(user("A001"), "November"));
    }
}
