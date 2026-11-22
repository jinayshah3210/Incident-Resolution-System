package com.resolveit.service;

import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Role;
import com.resolveit.exception.AccountLockedException;
import com.resolveit.exception.AuthenticationRequiredException;
import com.resolveit.exception.ConflictException;
import com.resolveit.exception.InvalidCredentialsException;
import com.resolveit.exception.ValidationException;
import com.resolveit.factory.NewUserSpec;
import com.resolveit.factory.UserFactory;
import com.resolveit.repository.UserRepository;
import com.resolveit.security.JwtTokenService;
import com.resolveit.web.dto.LoginResponse;
import com.resolveit.web.dto.RegisterRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login with portal check and lockout, employee self-registration, token authentication and logout. */
@Service
public class AuthService {
    private final UserRepository users;
    private final JwtTokenService tokens;
    private final UserFactory factory;
    private final UserService userService;
    private final AuditService audit;
    private final Clock clock;

    public AuthService(UserRepository users, JwtTokenService tokens, UserFactory factory, UserService userService,
                       AuditService audit, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.factory = factory;
        this.userService = userService;
        this.audit = audit;
        this.clock = clock;
    }

    /** Wrong password, unknown account and wrong portal all produce the same generic 401 message. */
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, AccountLockedException.class})
    public LoginResponse login(String login, String password, String portal) {
        Role role = Role.parse(portal);
        Instant now = clock.instant();
        String key = login == null ? "" : login.strip();
        Optional<PortalUser> found = users.findByEmailIgnoreCase(key).or(() -> users.findByIdIgnoreCase(key));
        PortalUser user = found.orElse(null);
        if (user != null && user.isLocked(now)) throw new AccountLockedException();
        if (user == null || !user.hasRole(role) || !user.verifyPassword(password, now)) {
            if (user != null) {
                user.recordFailedLogin(now);
                if (user.isLocked(now)) {
                    audit.recordSystem(user.getId(), "ACCOUNT_LOCKED", "Account locked after " + PortalUser.MAX_FAILED_ATTEMPTS + " failed logins.");
                    throw new AccountLockedException();
                }
                audit.recordSystem(user.getId(), "LOGIN_FAILED", "Failed login attempt " + user.getFailedLoginAttempts() + ".");
            }
            throw new InvalidCredentialsException();
        }
        return new LoginResponse(tokens.issue(user.getId(), user.getRole().name()), userService.view(user));
    }

    /** Self-registration only ever creates an EMPLOYEE; any role sent by the client is ignored. */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = request.email() == null ? "" : request.email().strip().toLowerCase();
        if (request.name() == null || request.name().isBlank() || !email.contains("@") || !UserService.validPassword(request.password())) {
            throw new ValidationException("Enter a name, valid email and a password with at least 8 characters, one uppercase, one lowercase, and one digit.");
        }
        if (users.existsByEmailIgnoreCase(email)) throw new ConflictException("An account with this email already exists.");
        String id = userService.nextId(Role.EMPLOYEE);
        users.save(factory.create(Role.EMPLOYEE, NewUserSpec.employee(id, request.name().strip(), email, request.password(), request.department())));
        audit.recordSystem(id, "ACCOUNT_REGISTERED", "Employee " + id + " registered.");
        return login(email, request.password(), Role.EMPLOYEE.name());
    }

    @Transactional(readOnly = true)
    public PortalUser authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AuthenticationRequiredException("Please sign in to continue.");
        }
        String id = tokens.subject(authorizationHeader.substring(7).strip());
        PortalUser user = users.findById(id).orElseThrow(AuthenticationRequiredException::new);
        if (!user.isActive()) throw new AuthenticationRequiredException();
        return user;
    }

    @Transactional(readOnly = true)
    public com.resolveit.web.dto.UserDto me(PortalUser user) { return userService.view(user); }

    public void logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) tokens.revoke(authorizationHeader.substring(7).strip());
    }
}
