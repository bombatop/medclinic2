---
description: "Auth roles, user lifecycle, admin seeder, auth flows for MedClinic 2"
alwaysApply: false
globs: ["backend/auth-service/**/*", "frontend/src/stores/auth.ts", "frontend/src/api/users.ts", "frontend/src/views/LoginView.vue", "frontend/src/views/UsersView.vue", "frontend/src/views/ProfileView.vue", "frontend/src/router/**/*"]
---

## Domain: Auth & accounts

### Roles

- `ADMIN`, `EMPLOYEE` (`Role.java`).
- Stored on `User.role`, propagated via JWT as `role` claim, gateway injects `X-User-Role`.
- **ADMIN**: Full user management (`/auth/users`), employee admin in main-service.
- **EMPLOYEE**: No admin endpoints; can log in, refresh, view/update profile, change password, use client/appointment APIs.

### User lifecycle

- **Entity**: `User` (auth-service). Fields: username (unique), password (hashed), first/last name, email (unique), phone, role, active, createdAt, updatedAt.
- **Create** (admin only): username/email unique, password encoded, role ADMIN/EMPLOYEE, new users `active = true`.
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

- Auth-service: `@PreAuthorize` for `/auth/users` (admin-only).
- Main-service: `RequestContext.isAdmin()` (from gateway headers) guards employee admin ops.
- Clients and appointments: any authenticated user.
