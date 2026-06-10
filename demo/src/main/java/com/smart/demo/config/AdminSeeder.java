package com.smart.demo.config;

import com.smart.demo.model.Gender;
import com.smart.demo.model.Role;
import com.smart.demo.model.User;
import com.smart.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(1)
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminEmail = System.getenv().getOrDefault("ADMIN_EMAIL", "admin@opd.local");
        String adminUsername = System.getenv().getOrDefault("ADMIN_USERNAME", "admin");
        String adminPassword = System.getenv().getOrDefault("ADMIN_PASSWORD", "Admin@123");

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = new User();
        admin.setFullName("System Admin");
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.STAFF);
        admin.setAge(30);
        admin.setGender(Gender.OTHER);
        admin.setContactNumber(System.getenv().getOrDefault("ADMIN_CONTACT", "9000000000"));
        admin.setAddress("Smart OPD HQ");
        admin.setRegistrationDate(LocalDateTime.now());
        userRepository.save(admin);
    }
}