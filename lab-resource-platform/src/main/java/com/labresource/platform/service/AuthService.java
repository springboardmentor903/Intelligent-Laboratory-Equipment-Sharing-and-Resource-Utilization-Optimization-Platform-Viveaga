
package com.labresource.platform.service;

import com.labresource.platform.dto.LoginRequest;
import com.labresource.platform.dto.LoginResponse;
import com.labresource.platform.dto.RegisterRequest;
import com.labresource.platform.entity.Role;
import com.labresource.platform.entity.RoleRequest;
import com.labresource.platform.entity.User;
import com.labresource.platform.repository.RoleRequestRepository;
import com.labresource.platform.repository.UserRepository;
import com.labresource.platform.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RoleRequestRepository roleRequestRepository;

    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // Default role until the requested role is approved
        user.setRole(Role.RESEARCHER_STUDENT);

        userRepository.save(user);

        // Read the role requested during registration
        Role requestedRole = request.getRequestedRole();

        if (requestedRole != null
                && requestedRole != Role.RESEARCHER_STUDENT) {

            if (requestedRole == Role.SYSTEM_ADMINISTRATOR) {
                throw new RuntimeException(
                        "System Administrator role cannot be requested"
                );
            }

            RoleRequest roleRequest = new RoleRequest();
            roleRequest.setUser(user);
            roleRequest.setRequestedRole(requestedRole);
            roleRequest.setStatus("PENDING");

            roleRequestRepository.save(roleRequest);
        }

        return "Registration successful. Requested role is pending approval.";
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }


        if (roleRequestRepository.findByStatus("PENDING")
                .stream()
                .anyMatch(r ->
                        r.getUser().getId().equals(user.getId()))) {
            throw new RuntimeException(
                    "Your role request is pending approval");
        }


        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new LoginResponse(
                "Login successful",
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                token
        );
    }
}
