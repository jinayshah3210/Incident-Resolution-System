package com.resolveit.domain.user;

import com.resolveit.domain.incident.Incident;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Abstract base of every ResolveIT account (Employee, Technician, Administrator).
 * Inheritance: JPA JOINED strategy, one table per subclass plus the shared users table.
 * Encapsulation: the password hash and lockout counters are private and only change through behaviour methods.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class PortalUser {
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(10);

    @Id
    @Column(length = 20)
    private String id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, unique = true, length = 160)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(length = 120)
    private String department;
    @Column(length = 40)
    private String phone;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PortalUser() { }

    protected PortalUser(String id, String name, String email, String rawPassword, String department) {
        this.id = id;
        this.name = name;
        this.email = email.toLowerCase(Locale.ROOT);
        this.passwordHash = PASSWORD_ENCODER.encode(rawPassword);
        this.department = department;
        this.createdAt = Instant.now();
    }

    /** Polymorphism: each subclass reports its own portal role. */
    public abstract Role getRole();

    /** Polymorphism: ownership rules differ per role (employee = reporter, technician = assignee, admin = all). */
    public abstract boolean canView(Incident incident);

    public String getDashboardPath() { return getRole().getDashboardPath(); }

    public boolean hasRole(Role role) { return getRole() == role; }

    /** Checks a password without ever exposing the hash; a success clears the failure counter. */
    public synchronized boolean verifyPassword(String rawPassword, Instant now) {
        if (!active || isLocked(now) || rawPassword == null || !PASSWORD_ENCODER.matches(rawPassword, passwordHash)) {
            return false;
        }
        failedLoginAttempts = 0;
        lockedUntil = null;
        return true;
    }

    public boolean verifyPassword(String rawPassword) { return verifyPassword(rawPassword, Instant.now()); }

    public synchronized void recordFailedLogin(Instant now) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) lockedUntil = now.plus(LOCK_DURATION);
    }

    public synchronized boolean isLocked(Instant now) {
        if (lockedUntil == null) return false;
        if (now.isBefore(lockedUntil)) return true;
        lockedUntil = null;
        failedLoginAttempts = 0;
        return false;
    }

    public boolean isLocked() { return isLocked(Instant.now()); }

    public void deactivate() { active = false; }

    public synchronized void activate() {
        active = true;
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public void updateContact(String name, String department, String phone) {
        if (name != null && !name.isBlank()) this.name = name.strip();
        if (department != null) this.department = department.strip();
        if (phone != null) this.phone = phone.strip();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDepartment() { return department; }
    public String getPhone() { return phone; }
    public boolean isActive() { return active; }
    public synchronized int getFailedLoginAttempts() { return failedLoginAttempts; }
    public synchronized Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }

    @Override public boolean equals(Object other) {
        return other instanceof PortalUser user && id != null && id.equals(user.id);
    }

    @Override public int hashCode() { return id == null ? 0 : id.hashCode(); }
}
