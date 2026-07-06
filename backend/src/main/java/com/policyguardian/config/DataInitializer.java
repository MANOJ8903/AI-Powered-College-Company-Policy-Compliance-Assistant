package com.policyguardian.config;

import com.policyguardian.model.Role;
import com.policyguardian.model.User;
import com.policyguardian.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@policyguardian.org")
                    .password(passwordEncoder.encode("adminpassword"))
                    .role(Role.ADMIN)
                    .department("HR")
                    .build();
            userRepository.save(admin);
            System.out.println("Seeded default administrator account: admin / adminpassword");
        }
    }
}
