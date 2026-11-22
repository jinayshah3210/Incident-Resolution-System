package com.resolveit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless API security. Authentication is the signed JWT resolved by CurrentUserArgumentResolver;
 * role and ownership checks live in the services (AccessGuard and PortalUser.canView).
 */
@Configuration
public class SecurityConfig {
    @Bean
    UserDetailsService noFormLogin() {
        return username -> { throw new UsernameNotFoundException("Use the ResolveIT portal login endpoint."); };
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.deny()).contentTypeOptions(Customizer.withDefaults()))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .build();
    }
}
