package io.github.alexanderjabka.imageservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    
    public Long extractUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
            try {
                return Long.parseLong(userIdHeader.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid user ID in header: " + userIdHeader);
            }
        }
        
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return 1L;
        }
        
        throw new IllegalArgumentException("User ID not found in request headers");
    }
    
    public boolean hasValidUserId(HttpServletRequest request) {
        try {
            extractUserId(request);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

