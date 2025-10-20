// src/main/java/com/example/demo/config/SecurityConfig.java
package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/user/login",
        "/api/user/register",
        "/api/user/validate-session",
        "/api/user/logout",
        "/api/public/**"
    };

    private static final String[] AUTHENTICATED_ENDPOINTS = {
        "/api/user/current",
        "/api/piece/**",
        "/api/lesson/**",
        "/api/user/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS and disable CSRF for API
            .cors().and()
            .csrf().disable()

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(AUTHENTICATED_ENDPOINTS).authenticated()
                .anyRequest().permitAll()
            )

            // Disable default Spring login page, since you have your own AuthController
            .formLogin().disable()

            // Configure logout to return 200 OK instead of redirect
            .logout(logout -> logout
                .logoutUrl("/api/user/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    /**
     * Global CORS configuration to allow your React app on port 5173.
     * This is REQUIRED for fetch(..., { credentials: "include" }) to work.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:5173")); // your Vite dev server
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true); // allow cookies/session

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     *Password encoder for user passwords
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
