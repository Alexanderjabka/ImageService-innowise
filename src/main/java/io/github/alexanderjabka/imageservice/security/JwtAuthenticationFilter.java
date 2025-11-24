package io.github.alexanderjabka.imageservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userIdHeader = request.getHeader("X-User-Id");
        String usernameHeader = request.getHeader("X-User-Name");
        if (userIdHeader == null || userIdHeader.isBlank() || usernameHeader == null || usernameHeader.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authentication headers");
            return;
        }

        try {
            Long userId = Long.parseLong(userIdHeader);
            setAuthentication(request, new UserPrincipal(userId, usernameHeader));
            filterChain.doFilter(request, response);
        } catch (NumberFormatException e) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication headers");
        }
    }

    private void setAuthentication(HttpServletRequest request, UserPrincipal principal) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
