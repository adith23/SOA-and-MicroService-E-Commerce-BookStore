package com.globalbooks.orders.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 Resource Server configuration for OrdersService.
 * Q13: OAuth2 configuration for securing the REST API.
 *
 * All /api/v1/orders/** endpoints require a valid JWT Bearer token
 * issued by Spring Authorization Server (AuthServer on port 9000).
 *
 * Token validation uses the JWK Set URI discovered from the issuer-uri.
 */
@Configuration
@EnableWebSecurity
public class OAuth2ResourceServerConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (stateless REST API)
            .csrf(csrf -> csrf.disable())

            // Stateless session – no server-side sessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow actuator/health endpoints without auth
                .requestMatchers("/actuator/**").permitAll()
                // All order endpoints require authentication
                .requestMatchers(HttpMethod.POST,   "/api/v1/orders").authenticated()
                .requestMatchers(HttpMethod.GET,    "/api/v1/orders/**").authenticated()
                // Deny everything else
                .anyRequest().authenticated()
            )

            // OAuth2 Resource Server with JWT validation
            // JWT issuer-uri is configured in application.yml
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            );

        return http.build();
    }
}
