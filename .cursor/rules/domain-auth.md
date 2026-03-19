---
description: "Auth roles, user lifecycle, admin seeder, auth flows for MedClinic 2"
alwaysApply: false
globs: ["backend/auth-service/**/*", "frontend/src/stores/auth.ts", "frontend/src/api/users.ts", "frontend/src/api/rbac.ts", "frontend/src/views/LoginView.vue", "frontend/src/views/UsersView.vue", "frontend/src/views/ProfileView.vue", "frontend/src/views/RolesView.vue", "frontend/src/views/RolePermissionsView.vue", "frontend/src/views/RbacAuditView.vue", "frontend/src/router/**/*"]
---

## Domain: Auth & accounts

### Role model (DB-backed, enterprise RBAC)

- **Roles** are DB entities (`Role` in `roles` table), not enums. Fields: `id`, `code`, `name`, `description`, `active`, `system`.
- **Baseline roles** `ADMIN`, `DOCTOR`, `RECEPTIONIST` are seeded by `RbacSeeder` on startup. Custom roles can be created via RBAC admin API.
- **User–role**: `User.roles` is `ManyToMany` with `Role` via `user_roles` join table. Users can have multiple roles.
- **Role–permission**: `role_permissions` table maps `Role` to `PermissionEntity`. Editable via RBAC admin API (`PUT /auth/rbac/roles/{id}/permissions`).

### Permission model

- **Permission catalog** is code-defined (`Permission` enum) and seeded into `permissions` table as `PermissionEntity` by `RbacSeeder`.
- **Business-action permissions** (e.g. `users.manage`, `appointment.create_self`) are used for authorization. No groups or hierarchies.
- **Role–permission mapping** is stored in DB and editable in UI; permission definitions stay in code.

### Baseline role semantics

- **ADMIN**: Full user management, role assignment, employee admin. Has all permissions.
- **DOCTOR**: Appointment self-write flows + participate permission; read-only for others.
- **RECEPTIONIST**: Read-oriented baseline (appointment.read_all, employee.read_all).

### User lifecycle

- **Entity**: `User` (auth-service). Fields: username (unique), password (hashed), first/last name, email (unique), phone, roles (Set&lt;Role&gt;), active, createdAt, updatedAt.
- **Create** (admin only): username/email unique, password encoded, roles assigned by code (e.g. `["ADMIN", "DOCTOR"]`), new users `active = true`.
- **Read**: Admin: all users and by id. Any user: `/auth/me` returns current user.
- **Update**: Admin: any profile. Self: `/auth/me`. Email must stay unique.
- **Role assignment**: `GET/PUT /auth/users/{id}/roles`; accepts role codes. Requires `users.manage_roles`. Audited in `rbac_audit_log`.
- **Activate/deactivate**: Admin-only, soft (inactive users cannot log in).
- **Change password**: Self via `/auth/me/password`; requires correct current password.

### Default admin (seeded)

- **AdminSeeder** runs after **RbacSeeder**. If no user has ADMIN role, creates: username `admin`, password `admin`, email `admin@medclinic.local`.
- To reset: see `docs/admin-reset.md` or `scripts/cleanup-reseed.ps1`.

### Auth flows

- **Login** (`/auth/login`): Validates credentials and `active`; issues access + refresh tokens. JWT claims: `userId`, `roles` (codes), `permissions` (codes).
- **Refresh** (`/auth/refresh`): Validates refresh token, user existence, `active`; issues new tokens. Refresh tokens have `type: "refresh"` and must not be used as access tokens; gateway rejects them.
- **Profile**: `/auth/me` GET returns user; PUT updates profile with email uniqueness check.

### JWT and gateway propagation

- **Access token**: carries `roles` and `permissions` as string arrays. No single `role` claim.
- **Gateway** (`JwtAuthGatewayFilter`): validates JWT, rejects refresh tokens, injects `X-User-Id`, `X-Username`, `X-User-Roles`, `X-User-Permissions`.
- **Auth-service** (`GatewayHeaderFilter`): when called directly (e.g. internal), can use JWT from `Authorization` or trust gateway headers.

### RBAC admin API

- **Roles**: `GET/POST/PUT/DELETE /auth/rbac/roles`. Create custom roles; system roles (ADMIN, DOCTOR, RECEPTIONIST) have `system: true`.
- **Permissions**: `GET /auth/rbac/permissions` returns catalog. Permissions are code-defined; mapping is DB-backed.
- **Role permissions**: `GET/PUT /auth/rbac/roles/{id}/permissions`. Replace mapping with set of permission codes.
- **Audit**: `GET /auth/rbac/audit` with filters (actorUsername, action, from, to). Records role/user changes.
- All require `users.read_all` or `users.manage_roles` as appropriate.

### Role enforcement

- **Auth-service**: permission-based `@PreAuthorize("hasAuthority('PERM_...')")` on `/auth/users` and RBAC endpoints.
- **Main-service**: `RequestContext` reads roles/permissions from gateway headers. Use `hasPermission(code)` for fine-grained checks.
- **Frontend**: `authStore.roles`, `authStore.permissions`, `authStore.hasPermission(code)`, `authStore.canAccessAdmin`, `authStore.canManageRbac`. Role options come from `GET /auth/rbac/roles`.
