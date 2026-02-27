package com.medclinic.auth.config;

import com.medclinic.auth.model.Role;
import com.medclinic.auth.model.User;
import com.medclinic.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .firstName("System")
                .lastName("Administrator")
                .email("admin@medclinic.local")
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.warn("Default admin account created (username: admin, password: admin). Change the password immediately.");
    }
}
