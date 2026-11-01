
package com.labresource.platform.security;

import io.jsonwebtoken.Claims;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        // STEP 1: Authenticate STOMP connection
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authorization =
                    accessor.getFirstNativeHeader("Authorization");

            if (authorization == null ||
                    !authorization.startsWith("Bearer ")) {
                throw new IllegalArgumentException(
                        "Missing WebSocket JWT token"
                );
            }

            String token = authorization.substring(7);

            try {
                Claims claims = jwtService.extractClaims(token);

                String email = claims.getSubject();
                String role = claims.get("role", String.class);

                if (email == null || role == null) {
                    throw new IllegalArgumentException(
                            "Invalid JWT claims"
                    );
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + role
                                        )
                                )
                        );

                accessor.setUser(authentication);

            } catch (io.jsonwebtoken.JwtException |
                     IllegalArgumentException exception) {

                throw new IllegalArgumentException(
                        "Invalid or expired WebSocket JWT token",
                        exception
                );
            }
        }

        // STEP 2: Authorize subscriptions
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            String destination = accessor.getDestination();

            if (!(accessor.getUser() instanceof Authentication authentication)) {
                throw new IllegalArgumentException(
                        "Unauthenticated subscription rejected"
                );
            }

            boolean allowedDestination =
                    "/topic/equipment-status".equals(destination);

            boolean allowedRole =
                    authentication.getAuthorities().stream()
                            .anyMatch(authority -> {
                                String role = authority.getAuthority();

                                return role.equals("ROLE_LAB_MANAGER")
                                        || role.equals("ROLE_LAB_TECHNICIAN")
                                        || role.equals("ROLE_DEPARTMENT_HEAD")
                                        || role.equals("ROLE_INSTITUTION_ADMINISTRATOR")
                                        || role.equals("ROLE_SYSTEM_ADMINISTRATOR");
                            });

            if (!allowedDestination || !allowedRole) {
                throw new IllegalArgumentException(
                        "You are not authorized to subscribe to this topic"
                );
            }
        }

        return message;
    }
}
