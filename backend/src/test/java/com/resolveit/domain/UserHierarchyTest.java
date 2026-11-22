package com.resolveit.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.resolveit.TestData;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.user.AdminLevel;
import com.resolveit.domain.user.Administrator;
import com.resolveit.domain.user.Employee;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Role;
import com.resolveit.domain.user.Technician;
import com.resolveit.factory.NewUserSpec;
import com.resolveit.factory.UserFactory;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Inheritance, polymorphism and encapsulation of the account hierarchy. */
class UserHierarchyTest {
    private final Administrator admin = new Administrator("A001", "Alex Morgan", "a@example.test", "Password@123");
    private final Employee other = new Employee("E102", "Sam", "e102@example.test", "Password@123", "Ops");
    private final Technician otherTech = new Technician("T202", "Noah", "t202@example.test", "Password@123", "Ops", Set.of("Network"), 4);

    @Test void eachSubclassReportsItsRoleAndDashboard() {
        assertEquals(Role.EMPLOYEE, TestData.REPORTER.getRole());
        assertEquals("/employee", TestData.REPORTER.getDashboardPath());
        assertEquals(Role.TECHNICIAN, TestData.TECH.getRole());
        assertEquals("/technician", TestData.TECH.getDashboardPath());
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals("/admin", admin.getDashboardPath());
    }

    @Test void polymorphicOwnershipRules() {
        var incident = TestData.incidentIn(IncidentStatus.ASSIGNED);
        PortalUser[] allowed = {TestData.REPORTER, TestData.TECH, admin};
        for (PortalUser u : allowed) assertTrue(u.canView(incident), u.getId());
        assertFalse(other.canView(incident));
        assertFalse(otherTech.canView(incident));
    }

    @Test void passwordIsBcryptHashedAndVerified() {
        Employee e = new Employee("E900", "Test", "t@example.test", "Secret123", "Ops");
        assertNotEquals("Secret123", e.getPasswordHash());
        assertTrue(e.getPasswordHash().startsWith("$2a$10$"));
        assertTrue(e.verifyPassword("Secret123"));
        assertFalse(e.verifyPassword("wrong"));
    }

    @Test void fiveFailuresLockForFifteenMinutes() {
        Employee e = new Employee("E901", "Test", "t2@example.test", "Secret123", "Ops");
        Instant now = Instant.parse("2026-11-20T10:00:00Z");
        for (int n = 0; n < 5; n++) e.recordFailedLogin(now);
        assertTrue(e.isLocked(now.plusSeconds(60)));
        assertFalse(e.verifyPassword("Secret123", now.plusSeconds(60)));
        assertFalse(e.isLocked(now.plusSeconds(15 * 60 + 1)));
        assertTrue(e.verifyPassword("Secret123", now.plusSeconds(15 * 60 + 2)));
    }

    @Test void deactivatedAccountCannotAuthenticate() {
        Employee e = new Employee("E902", "Test", "t3@example.test", "Secret123", "Ops");
        e.deactivate();
        assertFalse(e.verifyPassword("Secret123"));
    }

    @Test void technicianSkillsAndCapacity() {
        assertTrue(TestData.TECH.canHandle("application"));
        assertFalse(TestData.TECH.canHandle("Database"));
        assertTrue(TestData.TECH.hasCapacity(7));
        assertFalse(TestData.TECH.hasCapacity(8));
        assertThrows(UnsupportedOperationException.class, () -> TestData.TECH.getSkills().add("Hacking"));
    }

    @Test void userFactoryBuildsTheRightSubclassWithoutASwitch() {
        UserFactory factory = new UserFactory();
        assertInstanceOf(Employee.class, factory.create(Role.EMPLOYEE, NewUserSpec.employee("E1", "A", "a1@x.test", "Password@123", "Ops")));
        assertInstanceOf(Technician.class, factory.create(Role.TECHNICIAN,
                NewUserSpec.technician("T1", "B", "b1@x.test", "Password@123", "Ops", Set.of("Network"), 5, 2, true)));
        PortalUser manager = factory.create(Role.ADMIN, NewUserSpec.admin("A9", "C", "c1@x.test", "Password@123", AdminLevel.IT_MANAGER));
        assertTrue(((Administrator) manager).isItManager());
    }
}
