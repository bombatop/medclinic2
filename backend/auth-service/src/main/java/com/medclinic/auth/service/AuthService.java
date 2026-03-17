package com.medclinic.auth.service;

import com.medclinic.auth.dto.AuthResponse;
import com.medclinic.auth.dto.LoginRequest;
import com.medclinic.auth.model.Permission;
import com.medclinic.auth.model.Role;
import com.medclinic.auth.model.User;
import com.medclinic.auth.repository.UserRepository;
import com.medclinic.auth.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final RbacService rbacService;

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        java.util.Set<Role> roles = rbacService.resolveRoles(user);
        java.util.Set<Permission> permissions = rbacService.resolvePermissions(roles);
        String accessToken = jwtUtils.generateAccessToken(
                user.getId(),
                user.getUsername(),
                rbacService.toRoleCodes(roles),
                rbacService.toPermissionCodes(permissions)
        );
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        return new AuthResponse(
                accessToken,
                refreshToken,
                rbacService.toRoleCodes(roles),
                rbacService.toPermissionCodes(permissions),
                user.getUsername()
        );
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtUtils.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        java.util.Set<Role> roles = rbacService.resolveRoles(user);
        java.util.Set<Permission> permissions = rbacService.resolvePermissions(roles);
        String newAccessToken = jwtUtils.generateAccessToken(
                user.getId(),
                user.getUsername(),
                rbacService.toRoleCodes(roles),
                rbacService.toPermissionCodes(permissions)
        );
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                rbacService.toRoleCodes(roles),
                rbacService.toPermissionCodes(permissions),
                user.getUsername()
        );
    }
}
