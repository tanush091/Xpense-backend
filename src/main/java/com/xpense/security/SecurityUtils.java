package com.xpense.security;

import com.xpense.service.UserProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String getCurrentUserId(String fallbackUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof String s && !s.isEmpty()) {
                return s;
            }
        }
        return (fallbackUserId != null && !fallbackUserId.isEmpty()) ? fallbackUserId : UserProfileService.DEFAULT_USER_ID;
    }

    public static String getAuthenticatedUserId() {
        return getCurrentUserId(null);
    }
}
