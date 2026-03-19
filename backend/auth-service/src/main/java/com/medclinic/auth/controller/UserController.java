package com.medclinic.auth.controller;

import com.medclinic.auth.dto.CreateUserRequest;
import com.medclinic.auth.dto.PageResponse;
import com.medclinic.auth.dto.UpdateUserRequest;
import com.medclinic.auth.dto.UpdateUserRolesRequest;
import com.medclinic.auth.dto.UserResponse;
import com.medclinic.auth.dto.UserRolesResponse;
import com.medclinic.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * User management API. Endpoints accept optional X-User-Id header for audit actor.
 * When present (set by gateway from JWT), it is used in rbac_audit_log; when absent,
 * actor is resolved from Authentication.getName() via username lookup.
 */
@RestController
@RequestMapping("/auth/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_users.manage')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request,
                                                   Authentication authentication,
                                                   @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request, authentication.getName(), actorUserId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_users.read_all')")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(userService.getAllUsers(search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_users.read_all')")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_users.manage')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateUserRequest request,
                                                   Authentication authentication,
                                                   @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
        return ResponseEntity.ok(userService.updateUser(id, request, authentication.getName(), actorUserId));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('PERM_users.manage')")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id,
                                               Authentication authentication,
                                               @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
        userService.deactivateUser(id, authentication.getName(), actorUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('PERM_users.manage')")
    public ResponseEntity<Void> activateUser(@PathVariable Long id,
                                             Authentication authentication,
                                             @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
        userService.activateUser(id, authentication.getName(), actorUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('PERM_users.read_all')")
    public ResponseEntity<UserRolesResponse> getUserRoles(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserRoles(id));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('PERM_users.manage_roles')")
    public ResponseEntity<UserRolesResponse> updateUserRoles(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateUserRolesRequest request,
                                                             Authentication authentication,
                                                             @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
        return ResponseEntity.ok(userService.updateUserRoles(id, request, authentication.getName(), actorUserId));
    }
}
