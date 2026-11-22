package com.resolveit.web.controller;

import com.resolveit.domain.user.PortalUser;
import com.resolveit.security.CurrentUser;
import com.resolveit.service.AuthService;
import com.resolveit.web.dto.LoginRequest;
import com.resolveit.web.dto.LoginResponse;
import com.resolveit.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Login, registration, current user and logout. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body) { return auth.login(body.login(), body.password(), body.role()); }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest body) { return auth.register(body); }

    @GetMapping("/me")
    public Map<String, Object> me(@CurrentUser PortalUser user) { return Map.of("user", auth.me(user)); }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.logout(authorization);
        return Map.of("success", true);
    }
}
