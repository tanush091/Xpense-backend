package com.xpense.security;

import com.xpense.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * The signed-in user's id, taken only from the verified JWT (ARCHITECTURE rule 3).
     * There is no fallback user: a request without a valid token is rejected.
     */
    public static String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String s
                && !s.isEmpty() && !"anonymousUser".equals(s)) {
            return s;
        }
        throw new UnauthorizedException("Please sign in to continue.");
    }
}
