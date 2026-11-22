package com.resolveit.config;

import com.resolveit.security.CurrentUserArgumentResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS for the Vite frontend and the @CurrentUser argument resolver. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final CurrentUserArgumentResolver currentUserResolver;
    private final String[] origins;

    public WebConfig(CurrentUserArgumentResolver currentUserResolver,
                     @Value("${resolveit.cors-origins:http://localhost:5173}") String origins) {
        this.currentUserResolver = currentUserResolver;
        this.origins = origins.split(",");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS").allowedHeaders("*");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserResolver);
    }
}
