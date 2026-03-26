package com.globalbooks.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GlobalBooks OAuth2 Authorization Server
 * Uses Spring Authorization Server to issue JWT tokens.
 * Runs on port 9000.
 *
 * Q13: OAuth2 authorization server for securing OrdersService REST API.
 */
@SpringBootApplication
public class AuthServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
    }
}
