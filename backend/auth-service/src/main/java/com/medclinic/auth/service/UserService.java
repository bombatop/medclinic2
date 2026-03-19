package com.medclinic.auth.service;

import com.medclinic.auth.dto.ChangePasswordRequest;
import com.medclinic.auth.dto.CreateUserRequest;
import com.medclinic.auth.dto.UpdateUserRequest;
import com.medclinic.auth.dto.UserResponse;
import com.medclinic.auth.dto.UserRolesResponse;
import com.medclinic.auth.dto.UpdateUserRolesRequest;
import com.medclinic.auth.exception.ConflictException;
import com.medclinic.auth.exception.ResourceNotFoundException;
import com.medclinic.auth.model.Role;
import com.medclinic.auth.model.RoleAssignmentAudit;
import com.medclinic.auth.model.User;
import com.medclinic.auth.repository.RoleAssignmentAuditRepository;
import com.medclinic.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleAssignmentAuditRepository roleAssignmentAuditRepository;
    private final RbacService rbacService;
    private final RbacAdminService rbacAdminService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already taken");
        }

        Set<Role> roles = resolveRequestedRoles(request);
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .roles(new LinkedHashSet<>(roles))
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.from(userRepository.save(applyProfileUpdate(user, request)));
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(true);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public UserResponse updateUserByUsername(String username, UpdateUserRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.from(userRepository.save(applyProfileUpdate(user, request)));
    }

    private User applyProfileUpdate(User user, UpdateUserRequest request) {
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.email() != null) {
            if (!request.email().equals(user.getEmail()) && userRepository.existsByEmail(request.email())) {
                throw new ConflictException("Email already taken");
            }
            user.setEmail(request.email());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        return user;
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserRolesResponse getUserRoles(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Set<Role> roles = rbacService.resolveRoles(user);
        return new UserRolesResponse(user.getId(), user.getUsername(), rbacService.toRoleCodes(roles));
    }

    @Transactional
    public UserRolesResponse updateUserRoles(Long userId,
                                             UpdateUserRolesRequest request,
                                             String actorUsername,
                                             Long actorUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Set<Role> beforeRoles = rbacService.resolveRoles(user);
        Set<Role> afterRoles = rbacService.resolveRoleEntitiesByCodes(request.roles());

        user.setRoles(new LinkedHashSet<>(afterRoles));
        userRepository.save(user);

        RoleAssignmentAudit audit = RoleAssignmentAudit.builder()
                .actorUserId(resolveActorUserId(actorUsername, actorUserId))
                .actorUsername(actorUsername)
                .targetUserId(user.getId())
                .rolesBefore(rbacService.joinRoles(beforeRoles))
                .rolesAfter(rbacService.joinRoles(afterRoles))
                .build();
        roleAssignmentAuditRepository.save(audit);
        rbacAdminService.logAudit(
                actorUsername,
                actorUserId,
                "USER_ROLES_UPDATED",
                "USER",
                String.valueOf(user.getId()),
                "before=" + rbacService.joinRoles(beforeRoles) + ";after=" + rbacService.joinRoles(afterRoles)
        );

        return new UserRolesResponse(user.getId(), user.getUsername(), rbacService.toRoleCodes(afterRoles));
    }

    private Set<Role> resolveRequestedRoles(CreateUserRequest request) {
        return rbacService.resolveRoleEntitiesByCodes(request.roles());
    }

    private Long resolveActorUserId(String actorUsername, Long actorUserId) {
        if (actorUserId != null) {
            return actorUserId;
        }
        return userRepository.findByUsername(actorUsername)
                .map(User::getId)
                .orElse(0L);
    }
}
