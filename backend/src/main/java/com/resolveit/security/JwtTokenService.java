package com.resolveit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveit.exception.AuthenticationRequiredException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Issues and verifies HS256 JSON Web Tokens with a four-hour lifetime; logout revokes a token in this process. */
@Component
public class JwtTokenService {
    private static final String HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
    public static final Duration LIFETIME = Duration.ofHours(4);
    private final byte[] secret;
    private final Clock clock;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Set<String> revoked = ConcurrentHashMap.newKeySet();

    @Autowired
    public JwtTokenService(@Value("${resolveit.jwt-secret:local-demo-signing-secret-change-in-env-please}") String secret) {
        this(secret, Clock.systemUTC());
    }

    public JwtTokenService(String secret, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public String issue(String subject, String role) {
        try {
            long expiry = clock.instant().plus(LIFETIME).getEpochSecond();
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mapper.writeValueAsBytes(Map.of("sub", subject, "role", role, "exp", expiry)));
            String signingInput = HEADER + "." + payload;
            return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(signingInput));
        } catch (Exception e) {
            throw new IllegalStateException("Could not issue an authentication token.", e);
        }
    }

    public String subject(String token) {
        if (token == null || revoked.contains(token)) throw new AuthenticationRequiredException();
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !HEADER.equals(parts[0])) throw new AuthenticationRequiredException();
            byte[] supplied = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(sign(parts[0] + "." + parts[1]), supplied)) throw new AuthenticationRequiredException();
            Map<?, ?> claims = mapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
            if (!(claims.get("exp") instanceof Number expiry) || expiry.longValue() <= clock.instant().getEpochSecond()) {
                throw new AuthenticationRequiredException();
            }
            if (!(claims.get("sub") instanceof String subject) || subject.isBlank()) throw new AuthenticationRequiredException();
            return subject;
        } catch (AuthenticationRequiredException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationRequiredException();
        }
    }

    public void revoke(String token) { if (token != null && !token.isBlank()) revoked.add(token); }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }
}
