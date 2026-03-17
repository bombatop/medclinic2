---
description: "Auth roles, user lifecycle, admin seeder, auth flows for MedClinic 2"
alwaysApply: false
globs: ["backend/auth-service/**/*", "frontend/src/stores/auth.ts", "frontend/src/api/users.ts", "frontend/src/views/LoginView.vue", "frontend/src/views/UsersView.vue", "frontend/src/views/ProfileView.vue", "frontend/src/router/**/*"]
---

## Domain: Auth & accounts

### Roles

- `ADMIN`, `DOCTOR`, `RECEPTIONIST` (`Role.java`) with multi-role assignment.
- Stored on `User.roles`, propagated via JWT `roles`/`permissions` claims, gateway injects `X-User-Roles` and `X-User-Permissions`.
- **ADMIN**: Full user management (`/auth/users`), role assignment, employee admin in main-service.
- **DOCTOR**: Appointment self-write flows + participate permission.
- **RECEPTIONIST**: Read-oriented baseline in v1.

### User lifecycle

- **Entity**: `User` (auth-service). Fields: username (unique), password (hashed), first/last name, email (unique), phone, roles, active, createdAt, updatedAt.
- **Create** (admin only): username/email unique, password encoded, roles assigned, new users `active = true`.
- **Read**: Admin: all users and by id. Any user: `/auth/me` returns current user.
- **Update**: Admin: any profile. Self: `/auth/me`. Email must stay unique.
- **Activate/deactivate**: Admin-only, soft (inactive users cannot log in).
- **Change password**: Self via `/auth/me/password`; requires correct current password.

### Default admin (seeded)

- **AdminSeeder** runs on auth-service startup. If no ADMIN exists, creates: username `admin`, password `admin`, email `admin@medclinic.local`.
- To reset: see `docs/admin-reset.md`.

### Auth flows

- **Login** (`/auth/login`): Validates credentials and `active`; issues access + refresh tokens.
- **Refresh** (`/auth/refresh`): Validates refresh token, user existence, `active`; issues new tokens.
- **Profile**: `/auth/me` GET returns user; PUT updates profile with email uniqueness check.

### Role enforcement

- Auth-service: permission-based `@PreAuthorize` on `/auth/users` and role-assignment endpoints.
- Main-service: `RequestContext` reads roles/permissions from gateway headers for fine-grained checks.
- Clients and appointments: authenticated read by default; write is permission/ownership based.
