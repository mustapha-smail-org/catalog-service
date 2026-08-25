package com.citypulse.catalog.config;

import com.citypulse.catalog.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Gate for the admin read endpoints. Compares the caller-supplied token against
 * the configured secret in constant time. Denies by default when no secret is
 * configured, so the endpoints never expose data on a misconfigured deploy.
 */
@Component
public class AdminTokenGuard {

    private final byte[] expectedToken;

    public AdminTokenGuard(AdminProperties properties) {
        String token = properties.token();
        this.expectedToken = token == null || token.isBlank()
                ? null
                : token.getBytes(StandardCharsets.UTF_8);
    }

    public void requireValidToken(String suppliedToken) {
        if (expectedToken == null) {
            throw new UnauthorizedException("Admin access is not configured");
        }
        byte[] supplied = suppliedToken == null
                ? new byte[0]
                : suppliedToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedToken, supplied)) {
            throw new UnauthorizedException("Invalid admin token");
        }
    }
}
