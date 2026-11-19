package com.tcs.VendorPulse.auth.jwt;

import com.tcs.VendorPulse.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "VendorPulseSuperSecureSecretKeyThatIsAtLeast32BytesLongForHS256Algorithm",
                900000,
                604800000
        );
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        String sessionId = UUID.randomUUID().toString();

        String token = jwtService.generateAccessToken(
                userId, "procurement@vendorpulse.com", "Procurement Manager",
                UserRole.PROCUREMENT_MANAGER, supplierId, sessionId
        );

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));

        Claims claims = jwtService.extractClaims(token);
        assertNotNull(claims);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("procurement@vendorpulse.com", claims.get("email"));
        assertEquals("PROCUREMENT_MANAGER", claims.get("role"));
        assertEquals(supplierId.toString(), claims.get("supplierId"));
        assertEquals(sessionId, claims.get("sid"));
    }

    @Test
    void testGenerateRefreshToken() {
        UUID userId = UUID.randomUUID();
        String sessionId = UUID.randomUUID().toString();

        String token = jwtService.generateRefreshToken(userId, sessionId);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));

        Claims claims = jwtService.extractClaims(token);
        assertNotNull(claims);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("REFRESH", claims.get("type"));
        assertEquals(sessionId, claims.get("sid"));
    }

    @Test
    void testInvalidTokenFails() {
        assertFalse(jwtService.isTokenValid("invalid.token.structure"));
        assertNull(jwtService.extractClaims("invalid.token.structure"));
    }

    @Test
    void testTokenHashing() {
        String token = "my-secret-token";
        String hash1 = jwtService.hashToken(token);
        String hash2 = jwtService.hashToken(token);

        assertNotNull(hash1);
        assertEquals(64, hash1.length()); // SHA-256 hex string length
        assertEquals(hash1, hash2);
    }
}

