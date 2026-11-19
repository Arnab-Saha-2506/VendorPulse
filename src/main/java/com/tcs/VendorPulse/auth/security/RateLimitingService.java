package com.tcs.VendorPulse.auth.security;

import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private static class AttemptTracker {
        int count;
        Instant windowStart;

        AttemptTracker(Instant start) {
            this.count = 1;
            this.windowStart = start;
        }
    }

    private final Map<String, AttemptTracker> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptTracker> otpAttempts = new ConcurrentHashMap<>();

    public void checkLoginRateLimit(String key) {
        if (isRateLimited(loginAttempts, key)) {
            throw new ApiException(ErrorCode.RATE_LIMITED);
        }
    }

    public void recordFailedLogin(String key) {
        recordFailure(loginAttempts, key);
    }

    public void resetLoginAttempts(String key) {
        loginAttempts.remove(key);
    }

    public void checkOtpRateLimit(String key) {
        if (isRateLimited(otpAttempts, key)) {
            throw new ApiException(ErrorCode.RATE_LIMITED);
        }
    }

    public void recordFailedOtp(String key) {
        recordFailure(otpAttempts, key);
    }

    private boolean isRateLimited(Map<String, AttemptTracker> map, String key) {
        AttemptTracker tracker = map.get(key);
        if (tracker == null) {
            return false;
        }
        Instant now = Instant.now();
        if (now.isAfter(tracker.windowStart.plusSeconds(WINDOW_SECONDS))) {
            map.remove(key);
            return false;
        }
        return tracker.count >= MAX_ATTEMPTS;
    }

    private void recordFailure(Map<String, AttemptTracker> map, String key) {
        Instant now = Instant.now();
        map.compute(key, (k, tracker) -> {
            if (tracker == null || now.isAfter(tracker.windowStart.plusSeconds(WINDOW_SECONDS))) {
                return new AttemptTracker(now);
            }
            tracker.count++;
            return tracker;
        });
    }
}

