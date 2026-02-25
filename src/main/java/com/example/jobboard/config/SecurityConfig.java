package com.example.jobboard.config;

import com.example.jobboard.security.AppUserDetailsService;
import com.example.jobboard.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final int NINETY_DAYS_SECONDS = 90 * 24 * 60 * 60;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.remember-me.key}")
    private String rememberMeKey;

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        repo.setCreateTableOnStartup(false); // table created via schema.sql
        return repo;
    }

    @Bean
    public RememberMeServices rememberMeServices() {
        PersistentTokenBasedRememberMeServices services =
                new PersistentTokenBasedRememberMeServices(rememberMeKey, userDetailsService, persistentTokenRepository());
        services.setTokenValiditySeconds(NINETY_DAYS_SECONDS);
        services.setParameter("remember-me");
        services.setCookieName("JOBBOARD_REMEMBER_ME");
        return services;
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(java.util.List.of(
                    "null",
                    "https://wensonghu.github.io",
                    "http://localhost:8080"
                ));
                config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowCredentials(true);
                config.setAllowedHeaders(java.util.List.of("*"));
                return config;
            }))
            .authenticationProvider(daoAuthenticationProvider())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/", "/index.html", "/error",
                    "/oauth2/**", "/login/**", "/.well-known/**",
                    "/api/auth/status", "/api/auth/register", "/api/auth/verify-email",
                    "/api/support/**",
                    "/api/broadcast/current"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**"))
            )
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> {
                    // Resolve AppUser on form login and store in session
                    String email = auth.getName();
                    req.getSession().setAttribute("appUserEmail", email);
                    res.setStatus(HttpStatus.OK.value());
                })
                .failureHandler((req, res, ex) -> res.setStatus(HttpStatus.UNAUTHORIZED.value()))
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                    .baseUri("/oauth2/authorization")
                )
                .successHandler(oauth2LoginSuccessHandler)
                .failureUrl("/?auth_error=true")
            )
            .rememberMe(rm -> rm
                .rememberMeServices(rememberMeServices())
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .deleteCookies("JOBBOARD_REMEMBER_ME", "JSESSIONID")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
