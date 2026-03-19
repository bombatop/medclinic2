package com.medclinic.auth.controller;

import com.medclinic.auth.dto.*;
import com.medclinic.auth.service.RbacAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * RBAC admin API. Endpoints accept optional X-User-Id header for audit actor.
 * When present (set by gateway from JWT), it is used in rbac_audit_log; when absent,
 * actor is resolved from Authentication.getName() via username lookup.
 */
@RestController
@RequestMapping("/auth/rbac")
@RequiredArgsConstructor
public class RbacAdminController {

    private final RbacAdminService rbacAdminService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('PERM_users.read_all')")
    public ResponseEntity<PageResponse<RoleResponse>> getRoles(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(rbacAdminService.getRoles(pageable)));
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('PERM_users.read_all')")
    public ResponseEntity<RoleResponse> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(rbacAdminService.getRole(id));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('PERM_users.manage_roles')")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request,
                                                   Authentication authentication,
                                                   @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
        RoleResponse response = rbacAdminService.createRole(request, authentication.getName(), actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('PERM_users.manage_roles')")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateRoleRequest request,
                                                   Authentication authentication,
                                                   @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
        return ResponseEntity.ok(rbacAdminService.updateRole(id, request, authentication.getName(), actorUserId));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('PERM_users.manage_roles')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id,
                                           Authentication authentication,
                                           @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
        rbacAdminService.deleteRole(id, authentication.getName(), actorUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERM_users.read_all')")
    public ResponseEntity<List<PermissionResponse>> getPermissions() {
        return ResponseEntity.ok(rbacAdminService.getPermissions());
    }

    @GetMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('PERM_users.read_all')")
    public ResponseEntity<RolePermissionsResponse> getRolePermissions(@PathVariable Long id) {
        return ResponseEntity.ok(rbacAdminService.getRolePermissions(id));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('PERM_users.manage_roles')")
    public ResponseEntity<RolePermissionsResponse> updateRolePermissions(@PathVariable Long id,
                                                                         @Valid @RequestBody UpdateRolePermissionsRequest request,
                                                                         Authentication authentication,
                                                                         @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
        return ResponseEntity.ok(
                rbacAdminService.updateRolePermissions(id, request, authentication.getName(), actorUserId)
        );
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('PERM_users.manage_roles')")
    public ResponseEntity<PageResponse<RbacAuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(rbacAdminService.getAuditLogs(actorUsername, action, from, to, pageable)));
    }
}
