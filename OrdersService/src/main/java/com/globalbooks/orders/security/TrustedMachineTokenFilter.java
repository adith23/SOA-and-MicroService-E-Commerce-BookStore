package com.globalbooks.orders.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Accepts a pre-configured bearer token for trusted machine-to-machine calls from ODE.
 */
@Component
public class TrustedMachineTokenFilter extends OncePerRequestFilter {

    @Value("${integration.security.trusted-machine-token:}")
    private String trustedMachineToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (trustedMachineToken != null
                && !trustedMachineToken.isBlank()
                && header != null
                && header.equals("Bearer " + trustedMachineToken)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    "ode-machine-client",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTEGRATION"))
                );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
