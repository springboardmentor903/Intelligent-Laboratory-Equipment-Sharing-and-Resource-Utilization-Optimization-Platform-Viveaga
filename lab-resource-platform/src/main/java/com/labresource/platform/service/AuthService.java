
package com.labresource.platform.service;

import com.labresource.platform.dto.LoginRequest;
import com.labresource.platform.dto.LoginResponse;
import com.labresource.platform.dto.RegisterRequest;
import com.labresource.platform.entity.Institution;
import com.labresource.platform.entity.Role;
import com.labresource.platform.entity.User;
import com.labresource.platform.repository.InstitutionRepository;
import com.labresource.platform.repository.RoleRequestRepository;
import com.labresource.platform.repository.UserRepository;
import com.labresource.platform.security.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RoleRequestRepository roleRequestRepository;

    @Autowired
    private RoleRequestService roleRequestService;

    @Transactional
    public String register(RegisterRequest request) {

        // Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role requestedRole = request.getRequestedRole();

        // System Administrator cannot register through public registration
        if (requestedRole == Role.SYSTEM_ADMINISTRATOR) {
            throw new RuntimeException(
                    "System Administrator role cannot be requested"
            );
        }

        // Institution is mandatory
        if (request.getInstitutionId() == null) {
            throw new RuntimeException("Please select an institution");
        }

        Institution institution = institutionRepository
                .findById(request.getInstitutionId())
                .orElseThrow(() ->
                        new RuntimeException("Institution not found")
                );

        // Create user
        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // Default role until requested role is approved
        user.setRole(Role.RESEARCHER_STUDENT);

        user.setInstitution(institution);
        user.setDepartment(null);

        User savedUser = userRepository.save(user);

        // Create approval request for non-student roles
        if (requestedRole != null
                && requestedRole != Role.RESEARCHER_STUDENT) {

            roleRequestService.createRequestForUser(
                    savedUser,
                    requestedRole
            );

            return "Registration successful. Your role request is pending approval.";
        }

        // Student registration is automatic
        return "Registration successful. You can now log in.";
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid email or password"
                        )
                );

        // Validate password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        // Validate selected institution
        if (user.getInstitution() == null) {

            // Only System Administrator can have no institution
            if (user.getRole() != Role.SYSTEM_ADMINISTRATOR) {
                throw new RuntimeException(
                        "Your account does not have an institution assigned"
                );
            }

            // System Administrator must not select an institution
            if (request.getInstitutionId() != null) {
                throw new RuntimeException(
                        "System Administrator should not select an institution"
                );
            }

        } else {

            if (request.getInstitutionId() == null) {
                throw new RuntimeException(
                        "Please select your institution"
                );
            }

            if (!user.getInstitution().getId()
                    .equals(request.getInstitutionId())) {

                throw new RuntimeException(
                        "Selected institution does not match your account"
                );
            }
        }

        // Block login if the user has a pending role request
        boolean hasPendingRequest =
                roleRequestRepository.findByStatus("PENDING")
                        .stream()
                        .anyMatch(roleRequest ->
                                roleRequest.getUser()
                                        .getId()
                                        .equals(user.getId())
                        );

        if (hasPendingRequest) {
            throw new RuntimeException(
                    "Your role request is pending approval"
            );
        }

        // Generate JWT token
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
                token,
                user.getInstitution() != null
                        ? user.getInstitution().getId()
                        : null
        );
    }
}
