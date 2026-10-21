
package com.labresource.platform.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS configuration for the React frontend
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                // Enable CORS before handling authentication
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))

                // JWT-based stateless authentication
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, authException) -> {
                                    response.sendError(
                                            HttpStatus.UNAUTHORIZED.value(),
                                            "Unauthorized: Please provide a valid JWT token"
                                    );
                                }
                        )
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth

                        // Registration and login are public
                        .requestMatchers("/api/auth/**").permitAll()

                        // Student
                        .requestMatchers("/api/test/student")
                        .hasRole("RESEARCHER_STUDENT")

                        // Technician
                        .requestMatchers("/api/test/technician")
                        .hasRole("LAB_TECHNICIAN")

                        // Lab Manager
                        .requestMatchers("/api/test/manager")
                        .hasRole("LAB_MANAGER")

                        // Department Head
                        .requestMatchers("/api/test/department-head")
                        .hasRole("DEPARTMENT_HEAD")

                        // Institution Administrator
                        .requestMatchers("/api/test/institution-admin")
                        .hasRole("INSTITUTION_ADMINISTRATOR")

                        // System Administrator
                        .requestMatchers("/api/test/system-admin")
                        .hasRole("SYSTEM_ADMINISTRATOR")

                        // Only System Admin can review role requests
                        .requestMatchers("/api/role-requests/pending")
                        .hasRole("SYSTEM_ADMINISTRATOR")

                        .requestMatchers("/api/role-requests/*/review")
                        .hasRole("SYSTEM_ADMINISTRATOR")

                        // Authenticated users can access role requests
                        .requestMatchers("/api/role-requests")
                        .authenticated()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // OAuth2 login
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl(
                                "http://localhost:5173",
                                true
                        )
                )

                // Add JWT filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
