package com.example.jobboard.service;

import com.example.jobboard.model.GoogleCalendarToken;
import com.example.jobboard.repository.GoogleCalendarTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Handles the incremental-consent OAuth flow and token refresh for Google Calendar access. */
@Service
public class GoogleOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthService.class);
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v2/userinfo";

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    @Value("${google.calendar.scope}")
    private String scope;

    @Autowired
    private GoogleCalendarTokenRepository tokenRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record TokenResponse(String accessToken, String refreshToken, long expiresInSeconds) {}

    public String buildAuthorizationUrl(String state) {
        return AUTH_ENDPOINT
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&access_type=offline"
                + "&prompt=consent"
                + "&include_granted_scopes=true"
                + "&scope=" + encode(scope)
                + "&state=" + encode(state);
    }

    public TokenResponse exchangeCode(String code) throws IOException, InterruptedException {
        String body = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&code=" + encode(code)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + encode(redirectUri);
        return sendTokenRequest(body);
    }

    private TokenResponse refresh(GoogleCalendarToken token) throws IOException, InterruptedException {
        String body = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&refresh_token=" + encode(token.getRefreshToken())
                + "&grant_type=refresh_token";
        return sendTokenRequest(body);
    }

    private TokenResponse sendTokenRequest(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException("Google token endpoint returned " + response.statusCode() + ": " + response.body());
        }
        JsonNode json = objectMapper.readTree(response.body());
        String accessToken = json.path("access_token").asText(null);
        // Google omits refresh_token on a refresh call — caller keeps the existing one in that case.
        String refreshToken = json.hasNonNull("refresh_token") ? json.get("refresh_token").asText() : null;
        long expiresIn = json.path("expires_in").asLong(3600);
        return new TokenResponse(accessToken, refreshToken, expiresIn);
    }

    public String fetchEmail(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(USERINFO_ENDPOINT))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) return null;
            JsonNode json = objectMapper.readTree(response.body());
            return json.path("email").asText(null);
        } catch (Exception e) {
            logger.warn("Failed to fetch Google email for calendar connection: {}", e.getMessage());
            return null;
        }
    }

    /** Refreshes the token if it's within 60s of expiry, persisting the update. Returns a usable access token. */
    public String getValidAccessToken(GoogleCalendarToken token) throws IOException, InterruptedException {
        if (Instant.now().plusSeconds(60).isAfter(token.getTokenExpiry())) {
            TokenResponse refreshed = refresh(token);
            token.setAccessToken(refreshed.accessToken());
            if (refreshed.refreshToken() != null) {
                token.setRefreshToken(refreshed.refreshToken());
            }
            token.setTokenExpiry(Instant.now().plusSeconds(refreshed.expiresInSeconds()));
            tokenRepository.save(token);
        }
        return token.getAccessToken();
    }

    public void revoke(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/revoke?token=" + encode(accessToken)))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            logger.warn("Failed to revoke Google token (continuing with local disconnect anyway): {}", e.getMessage());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
