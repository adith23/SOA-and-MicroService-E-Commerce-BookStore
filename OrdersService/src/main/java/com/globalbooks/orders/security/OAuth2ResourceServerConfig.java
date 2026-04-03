package com.globalbooks.orders.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class OAuth2ResourceServerConfig {

    private final TrustedMachineTokenFilter trustedMachineTokenFilter;

    public OAuth2ResourceServerConfig(TrustedMachineTokenFilter trustedMachineTokenFilter) {
        this.trustedMachineTokenFilter = trustedMachineTokenFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (stateless REST API)
            .csrf(AbstractHttpConfigurer::disable)

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
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            )
            .addFilterBefore(trustedMachineTokenFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
