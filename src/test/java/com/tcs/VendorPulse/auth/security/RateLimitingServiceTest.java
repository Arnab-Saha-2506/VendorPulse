package com.tcs.VendorPulse.auth.security;

import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
    }

    @Test
    void testUnderLimitDoesNotThrow() {
        String key = "test@example.com";
        for (int i = 0; i < 4; i++) {
            assertDoesNotThrow(() -> rateLimitingService.checkLoginRateLimit(key));
            rateLimitingService.recordFailedLogin(key);
        }
        assertDoesNotThrow(() -> rateLimitingService.checkLoginRateLimit(key));
    }

    @Test
    void testReachingLimitThrowsRateLimited() {
        String key = "test@example.com";
        for (int i = 0; i < 5; i++) {
            rateLimitingService.recordFailedLogin(key);
        }

        ApiException ex = assertThrows(ApiException.class, () ->
                rateLimitingService.checkLoginRateLimit(key));
        assertEquals(ErrorCode.RATE_LIMITED, ex.getErrorCode());
    }

    @Test
    void testResetClearsLimit() {
        String key = "test@example.com";
        for (int i = 0; i < 5; i++) {
            rateLimitingService.recordFailedLogin(key);
        }
        assertThrows(ApiException.class, () -> rateLimitingService.checkLoginRateLimit(key));

        rateLimitingService.resetLoginAttempts(key);
        assertDoesNotThrow(() -> rateLimitingService.checkLoginRateLimit(key));
    }
}

