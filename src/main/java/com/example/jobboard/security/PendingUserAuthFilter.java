package com.example.jobboard.security;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates PENDING (guest trial) users with Spring Security by reading
 * the appUserId from the HttpSession. This allows existing card/alert endpoints
 * to work for PENDING users without any endpoint-level changes.
 */
@Component
public class PendingUserAuthFilter extends OncePerRequestFilter {

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Skip if Spring Security already has an authenticated principal
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()
                && !"anonymousUser".equals(existing.getName())) {
            chain.doFilter(request, response);
            return;
        }

        // If session has appUserId (set by SessionController or OAuth), authenticate via session
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("appUserId") != null) {
            try {
                Long userId = (Long) session.getAttribute("appUserId");
                AppUser user = userService.findById(userId);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // If user lookup fails, proceed unauthenticated
            }
        }

        chain.doFilter(request, response);
    }
}
