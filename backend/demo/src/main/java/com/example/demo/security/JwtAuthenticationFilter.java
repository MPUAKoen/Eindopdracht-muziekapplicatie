package com.example.demo.security;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean pieceDeleteRequest = "DELETE".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI() != null
                && request.getRequestURI().startsWith("/api/pieces/");

        if (authHeader != null
                && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authHeader.substring(7).trim();

            var email = jwtService.extractEmail(token);
            if (email.isEmpty()) {
                if (pieceDeleteRequest) {
                    logger.warn("Rejected piece delete {} because JWT token was invalid or expired", request.getRequestURI());
                }
            } else {
                var user = userRepository.findByEmail(email.get());
                if (user.isEmpty()) {
                    if (pieceDeleteRequest) {
                        logger.warn("Rejected piece delete {} because JWT subject {} was not found", request.getRequestURI(), email.get());
                    }
                } else {
                    authenticateRequest(user.get(), request);
                    if (pieceDeleteRequest) {
                        logger.info("Authenticated piece delete {} as {}", request.getRequestURI(), email.get());
                    }
                }
            }
        } else if (pieceDeleteRequest) {
            logger.warn("Rejected piece delete {} because Authorization header was missing", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateRequest(User user, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
