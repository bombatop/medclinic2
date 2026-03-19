package com.medclinic.auth.config;

import com.medclinic.auth.model.Role;
import com.medclinic.auth.model.User;
import com.medclinic.auth.repository.RoleRepository;
import com.medclinic.auth.repository.UserRepository;
import com.medclinic.auth.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRolesCode(RbacService.ROLE_ADMIN)) {
            return;
        }

        Role adminRole = roleRepository.findByCode(RbacService.ROLE_ADMIN)
                .orElse(null);
        if (adminRole == null) {
            log.warn("Admin role does not exist yet, skipping default admin seeding.");
            return;
        }

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .firstName("System")
                .lastName("Administrator")
                .email("admin@medclinic.local")
                .roles(new LinkedHashSet<>(Set.of(adminRole)))
                .build();

        userRepository.save(admin);
        log.warn("Default admin account created (username: admin, password: admin). Change the password immediately.");
    }
}
