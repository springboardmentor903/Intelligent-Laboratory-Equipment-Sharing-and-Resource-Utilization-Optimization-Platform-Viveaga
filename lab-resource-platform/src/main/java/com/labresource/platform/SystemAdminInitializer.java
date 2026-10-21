
package com.labresource.platform;

import com.labresource.platform.entity.Role;
import com.labresource.platform.entity.User;
import com.labresource.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SystemAdminInitializer {

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner createSystemAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (userRepository.existsByEmail(adminEmail)) {
                System.out.println(
                        "System Admin account already exists."
                );
                return;
            }

            User admin = new User();
            admin.setFullName(adminName);
            admin.setEmail(adminEmail);
            admin.setPassword(
                    passwordEncoder.encode(adminPassword)
            );
            admin.setRole(Role.SYSTEM_ADMINISTRATOR);

            userRepository.save(admin);

            System.out.println(
                    "System Admin account created successfully."
            );
        };
    }
}
