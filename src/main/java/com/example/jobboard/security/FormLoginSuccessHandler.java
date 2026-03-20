package com.example.jobboard.security;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles successful form (email/password) logins.
 *
 * On sign-in, if the session had a PENDING (guest) user, their cards and history
 * are merged into the now-authenticated REGISTERED account before the session is
 * updated. This is the moment of "proven ownership" — the user has provided
 * correct credentials, so it is safe to transfer the guest data.
 */
@Component
public class FormLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            AppUser registered = userService.findByEmail(authentication.getName());

            Long existingId = (Long) session.getAttribute("appUserId");
            if (existingId != null && !existingId.equals(registered.getId())) {
                // Session had a different user — check if it's a PENDING guest
                AppUser maybeGuest = null;
                try { maybeGuest = userService.findById(existingId); } catch (Exception ignored) {}
                if (maybeGuest != null && "PENDING".equals(maybeGuest.getStatus())) {
                    userService.mergePendingIntoExisting(maybeGuest, registered);
                }
            }

            // Always point the session at the REGISTERED user
            session.setAttribute("appUserId", registered.getId());
            session.setAttribute("appUserEmail", registered.getEmail());

            // Bind the browser's persistent ps_sid to this account for 90-day auto-login
            String guestToken = (String) session.getAttribute("guestSessionToken");
            if (guestToken != null && !guestToken.isEmpty()) {
                userService.renewSessionToken(registered, guestToken);
            }
        }

        response.setStatus(HttpStatus.OK.value());
    }
}
