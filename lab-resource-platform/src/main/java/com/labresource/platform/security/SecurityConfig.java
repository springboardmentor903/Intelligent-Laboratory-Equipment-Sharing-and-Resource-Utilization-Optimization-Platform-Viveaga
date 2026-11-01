
package com.labresource.platform.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

    // CORS configuration for React frontend
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
                "PATCH",
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

                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))

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

                .authorizeHttpRequests(auth -> auth

                        // Public registration and login
                        .requestMatchers("/api/auth/**").permitAll()

                        // Allow WebSocket handshake
                        .requestMatchers("/ws", "/ws/**").permitAll()

                        // Allow CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Student test endpoint
                        .requestMatchers("/api/test/student")
                        .hasRole("RESEARCHER_STUDENT")

                        // Technician test endpoint
                        .requestMatchers("/api/test/technician")
                        .hasRole("LAB_TECHNICIAN")

                        // Lab Manager test endpoint
                        .requestMatchers("/api/test/manager")
                        .hasRole("LAB_MANAGER")

                        // Department Head test endpoint
                        .requestMatchers("/api/test/department-head")
                        .hasRole("DEPARTMENT_HEAD")

                        // Institution Administrator test endpoint
                        .requestMatchers("/api/test/institution-admin")
                        .hasRole("INSTITUTION_ADMINISTRATOR")

                        // System Administrator test endpoint
                        .requestMatchers("/api/test/system-admin")
                        .hasRole("SYSTEM_ADMINISTRATOR")

                        // Only System Administrator can review role requests
                        .requestMatchers("/api/role-requests/pending")
                        .hasRole("SYSTEM_ADMINISTRATOR")

                        .requestMatchers("/api/role-requests/*/review")
                        .hasRole("SYSTEM_ADMINISTRATOR")

                        // Authenticated users can access role requests
                        .requestMatchers("/api/role-requests")
                        .authenticated()

                        // Only Lab Manager can add equipment
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/equipment"
                        )
                        .hasRole("LAB_MANAGER")

                        // Only Lab Manager can update equipment
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/equipment/**"
                        )
                        .hasRole("LAB_MANAGER")

                        // Only Lab Manager can change equipment status
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/equipment/*/status"
                        )
                        .hasRole("LAB_MANAGER")

                        // Only Lab Manager can approve or reject bookings
                        .requestMatchers(
                                "/api/bookings/*/approve",
                                "/api/bookings/*/reject"
                        )
                        .hasRole("LAB_MANAGER")

                        // All remaining endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Existing OAuth2 login configuration
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl(
                                "http://localhost:5173",
                                true
                        )
                )

                // Add JWT authentication filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
