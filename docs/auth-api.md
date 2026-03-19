# Auth API

## X-User-Id Header

RBAC admin and user management endpoints accept an optional `X-User-Id` header for audit logging.

- **When present:** The gateway sets this header from the JWT `userId` claim. It is used as the audit actor ID in `rbac_audit_log` and `role_assignment_audit`.
- **When absent:** The actor is resolved from `Authentication.getName()` via username lookup in the user repository. If the username is not found, actor ID is recorded as 0.

**Affected endpoints:**
- `POST/PUT/DELETE /auth/rbac/roles` (create, update, delete role)
- `PUT /auth/rbac/roles/{id}/permissions`
- `GET /auth/rbac/audit`
- `POST /auth/users` (create user)
- `PUT /auth/users/{id}` (update user)
- `PATCH /auth/users/{id}/activate`, `PATCH /auth/users/{id}/deactivate`
- `PUT /auth/users/{id}/roles`
