package com.example.jobboard.security;

import com.example.jobboard.model.AppUser;
import com.example.jobboard.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    public OAuth2LoginSuccessHandler() {
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = token.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String googleSub = oauth2User.getAttribute("sub");
        String displayName = oauth2User.getAttribute("name");

        // Check if there is a PENDING (guest trial) user in the session — convert if so
        AppUser appUser;
        Long pendingId = (Long) request.getSession().getAttribute("appUserId");
        if (pendingId != null) {
            com.example.jobboard.model.AppUser pendingUser = null;
            try { pendingUser = userService.findById(pendingId); } catch (Exception ignored) {}
            if (pendingUser != null && "PENDING".equals(pendingUser.getStatus())) {
                appUser = userService.convertPendingUserViaGoogle(pendingUser, email, googleSub, displayName);
            } else {
                appUser = userService.findOrCreateGoogleUser(email, googleSub, displayName);
            }
        } else {
            appUser = userService.findOrCreateGoogleUser(email, googleSub, displayName);
        }

        // Store the AppUser id in the session so all requests can resolve it
        request.getSession().setAttribute("appUserId", appUser.getId());

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
