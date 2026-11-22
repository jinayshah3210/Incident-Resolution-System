package com.resolveit.integration;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveit.domain.incident.Incident;
import com.resolveit.exception.GlobalExceptionHandler;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.CannotCreateTransactionException;

/** HTTP-level tests: authentication, lockout, role and ownership checks, validation and error mapping. */
@AutoConfigureMockMvc
class ApiSecurityIntegrationTest extends IntegrationTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired GlobalExceptionHandler handler;

    private String token(String login, String role) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("login", login, "password", "Password@123", "role", role))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    @Test void demoLoginsWorkForAllThreePortals() throws Exception {
        assertNotNull(token("A001", "ADMIN"));
        assertNotNull(token("e101@resolveit.test", "EMPLOYEE"));
        assertNotNull(token("T201", "TECHNICIAN"));
    }

    @Test void t15WrongPasswordAndWrongPortalGiveTheSameGeneric401() throws Exception {
        for (Map<String, String> body : new Map[]{Map.of("login", "E101", "password", "nope", "role", "EMPLOYEE"),
                Map.of("login", "E101", "password", "Password@123", "role", "ADMIN")}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_LOGIN"))
                    .andExpect(jsonPath("$.message").value("Email or password is incorrect."));
        }
    }

    @Test void t15FiveFailuresLockTheAccount() throws Exception {
        String bad = json.writeValueAsString(Map.of("login", "E102", "password", "nope", "role", "EMPLOYEE"));
        for (int n = 0; n < 4; n++) mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(bad)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(bad)).andExpect(status().isLocked());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("login", "E102", "password", "Password@123", "role", "EMPLOYEE"))))
                .andExpect(status().isLocked()).andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test void missingOrTamperedTokenIs401() throws Exception {
        mvc.perform(get("/api/incidents/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/incidents/mine").header("Authorization", "Bearer abc.def.ghi")).andExpect(status().isUnauthorized());
    }

    @Test void t16EmployeeCannotReadAnotherEmployeesIncident() throws Exception {
        Incident i = payment();
        mvc.perform(get("/api/incidents/" + i.getPublicId()).header("Authorization", token("E102", "EMPLOYEE")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("UNAUTHORIZED_OPERATION"));
        mvc.perform(get("/api/incidents/" + i.getPublicId()).header("Authorization", token("E101", "EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.incident.publicId").value(i.getPublicId()));
    }

    @Test void t17EmployeeCannotUseAdminOrTechnicianEndpoints() throws Exception {
        String e = token("E101", "EMPLOYEE");
        for (String path : new String[]{"/api/admin/services", "/api/admin/audit", "/api/admin/settings", "/api/admin/users",
                "/api/admin/reports/overview", "/api/admin/sla-violations", "/api/technician/offers", "/api/technician/workload"}) {
            mvc.perform(get(path).header("Authorization", e)).andExpect(status().isForbidden());
        }
    }

    @Test void t17TechnicianCannotActOnSomeoneElsesIncident() throws Exception {
        Incident i = payment();
        String outsider = i.getTechnicianId().equals("T201") ? "T202" : "T201";
        mvc.perform(post("/api/incidents/" + i.getPublicId() + "/accept").header("Authorization", token(outsider, "TECHNICIAN")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/incidents/" + i.getPublicId() + "/start").header("Authorization", token("E101", "EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test void t18RegistrationIgnoresTheRoleField() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "New Person", "email", "new.person@example.test",
                                "password", "Secure123", "role", "ADMIN"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.user.id", startsWith("E")));
    }

    @Test void weakRegistrationPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Weak", "email", "weak@example.test", "password", "passwordonly"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test void t19InvalidIncidentReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/incidents").header("Authorization", token("E101", "EMPLOYEE")).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Bad", "description", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists()).andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test void t20DuplicateOverHttpIs409WithExistingId() throws Exception {
        Incident first = payment();
        mvc.perform(post("/api/incidents").header("Authorization", token("E101", "EMPLOYEE")).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", PAYMENT_TITLE, "description", PAYMENT_TEXT))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_INCIDENT"))
                .andExpect(jsonPath("$.details.existingIncidentId").value(first.getPublicId()));
    }

    @Test void invalidTransitionOverHttpIs409() throws Exception {
        Incident i = payment();
        mvc.perform(post("/api/incidents/" + i.getPublicId() + "/resolve").header("Authorization", token(i.getTechnicianId(), "TECHNICIAN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"summary\":\"Fixed already\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test void unknownIncidentIs404() throws Exception {
        mvc.perform(get("/api/incidents/INC424242").header("Authorization", token("A001", "ADMIN")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test void t28DatabaseFailureBecomes503WithoutStackTrace() {
        var response = handler.database(new CannotCreateTransactionException("connection refused"), new MockHttpServletRequest("GET", "/api/x"));
        assertEquals(503, response.getStatusCode().value());
        assertEquals("DATABASE_UNAVAILABLE", response.getBody().code());
        assertFalse(response.getBody().message().contains("connection refused"));
        assertNotNull(response.getBody().correlationId());
    }

    @Test void t26SearchFiltersAreRoleScoped() throws Exception {
        Incident mine = payment();
        create("E102", "Wifi is slow on floor three", "The wifi is slow on floor three for some users today.", null);
        String admin = token("A001", "ADMIN");
        mvc.perform(get("/api/incidents/search").param("priority", "P1").header("Authorization", admin))
                .andExpect(jsonPath("$[*].publicId", hasItem(mine.getPublicId())));
        mvc.perform(get("/api/incidents/search").param("service", "Network").param("status", "ASSIGNED").header("Authorization", admin))
                .andExpect(jsonPath("$[*].serviceName", everyItem(is("Network"))));
        mvc.perform(get("/api/incidents/search").param("q", mine.getPublicId()).param("category", "Application")
                        .param("slaStatus", "ok").param("technician", mine.getTechnicianId())
                        .param("from", "2000-01-01T00:00:00Z").param("to", "2100-01-01T00:00:00Z").header("Authorization", admin))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/incidents/search").header("Authorization", token("E102", "EMPLOYEE")))
                .andExpect(jsonPath("$[*].reporterId", everyItem(is("E102"))));
        mvc.perform(get("/api/incidents/search").param("slaStatus", "sideways").header("Authorization", admin))
                .andExpect(status().isBadRequest());
    }

    @Test void notificationsCanBeRemovedOnlyByTheirOwner() throws Exception {
        payment();
        String e = token("E101", "EMPLOYEE");
        String list = mvc.perform(get("/api/notifications").header("Authorization", e)).andReturn().getResponse().getContentAsString();
        String id = json.readTree(list).get(0).get("id").asText();
        mvc.perform(delete("/api/notifications/" + id).header("Authorization", token("E102", "EMPLOYEE"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/notifications/" + id).header("Authorization", e)).andExpect(status().isOk());
        mvc.perform(delete("/api/notifications").header("Authorization", e)).andExpect(status().isOk());
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", e)).andExpect(jsonPath("$.count").value(0));
    }

    @Test void removedChatStubEndpointsAreGone() throws Exception {
        mvc.perform(get("/api/chat/thread").header("Authorization", token("E101", "EMPLOYEE"))).andExpect(status().isNotFound());
    }
}
