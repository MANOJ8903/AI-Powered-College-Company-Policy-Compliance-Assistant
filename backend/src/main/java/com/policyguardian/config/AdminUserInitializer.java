package com.policyguardian.config;

import com.policyguardian.model.User;
import com.policyguardian.model.Role;
import com.policyguardian.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@policyguardian.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .department("Administration")
                    .build();
            userRepository.save(admin);
            System.out.println("Admin user initialized: username=admin, password=admin123");
        } else {
            userRepository.findByUsername("admin").ifPresent(admin -> {
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                System.out.println("Admin user password reset/verified to admin123");
            });
        }
    }
}
