---
description: "Business logic, auth roles, user lifecycle, patients, employees, appointments, and domain flows for MedClinic 2"
alwaysApply: false
globs: ["backend/**/*.java", "frontend/src/**/*.{ts,vue}"]
---

## Domain rules (draft)

> This file captures **business logic and flows** as they exist today. Treat it as documentation to guide future changes. The project owner should review any modifications carefully.

### 1. Auth & accounts

#### Roles

- Roles: `ADMIN`, `EMPLOYEE` (`backend/auth-service/.../Role.java`).
- Stored on `User.role`, propagated via JWT as `role` claim, and injected by the gateway as `X-User-Role`.
- `ADMIN`:
  - Full user management via `/auth/users` (create, list, update, activate/deactivate).
  - Employee admin operations in main-service (`/employees` create/update/activate/deactivate).
- `EMPLOYEE`:
  - No admin endpoints.
  - Can log in, refresh, view/update own profile, change own password.
  - Can use client and appointment APIs (see below).

#### User lifecycle

- **Entity**: `User` (auth-service).
- Fields: username (unique), password (hashed), first/last name, email (unique), phone, role, active, createdAt, updatedAt.
- **Create** (admin only, `/auth/users`):
  - Username and email must be unique.
  - Password is encoded before persisting.
  - Role must be one of `ADMIN`/`EMPLOYEE`.
  - New users start as `active = true`.
- **Read**:
  - Admin: all users and by id.
  - Any authenticated user: `/auth/me` returns current user (by username).
- **Update**:
  - Admin: update any user profile fields (no role changes today).
  - Self: update own profile via `/auth/me`.
  - Email changes must remain unique; conflicts are rejected.
- **Activate / deactivate**:
  - Admin-only, by id.
  - Deactivation is soft (no delete); inactive users cannot log in or refresh tokens.
- **Change password**:
  - Self-service via `/auth/me/password`.
  - Requires correct current password; otherwise rejected.

#### Default admin (seeded)

- **AdminSeeder** (`backend/auth-service/.../config/AdminSeeder.java`) runs on auth-service startup.
- If no user with role `ADMIN` exists, it creates one:
  - **Username**: `admin`
  - **Password**: `admin`
  - **Email**: `admin@medclinic.local`
- Change this password after first login (e.g. via `/auth/me/password`).

**Reset admin password to default (`admin` / `admin`):**

1. Delete the seeded admin user from the auth DB, then restart auth-service so the seeder recreates it:
   ```bash
   docker exec medclinic-postgres-auth psql -U auth_user -d auth_service_db -c "DELETE FROM users WHERE username = 'admin';"
   docker restart medclinic-auth-service
   ```
2. Wait for auth-service to be healthy; the default admin account will exist again with password `admin`.

#### Auth flows

- **Login** (`/auth/login`):
  - Validates username/password and that user is `active`.
  - Issues access & refresh tokens with `userId`, `username`, `role`.
- **Refresh** (`/auth/refresh`):
  - Accepts a refresh token; validates type, user existence, and `active` flag.
  - Issues new access & refresh tokens with same semantics.
- **Profile**:
  - `/auth/me`: returns `UserResponse` including role and active flag.
  - `/auth/me` (PUT): partial profile update with email uniqueness enforcement.

---

### 2. Patients (clients)

#### Entity & fields

- **Entity**: `Client` (main-service).
- Fields: firstName, lastName, phone (required); email, notes (optional); createdAt, updatedAt.

#### Behavior

- **Create** (`POST /clients`):
  - Requires authentication.
  - `firstName`, `lastName`, `phone` must be non-blank.
  - `email`, `notes` optional.
- **Read**:
  - `/clients`: list all.
  - `/clients/{id}`: fetch by id or 404 `"Client not found"`.
- **Update** (`PUT /clients/{id}`):
  - Applies only non-null fields from `UpdateClientRequest`.
  - Blank `email`/`notes` are normalized to `null`.
- **Delete** (`DELETE /clients/{id}`):
  - Hard delete; no `active` flag.

There are currently no uniqueness constraints at service level for phone/email beyond DB schema.

---

### 3. Doctors (employees)

#### Entity & link to auth

- **Entity**: `Employee` (main-service).
- Fields: authUserId (unique, required), firstName, lastName (required), specialization (optional), active, createdAt, updatedAt.
- `authUserId` links to `User.id` in auth-service; enforced as one-to-one (unique).

#### Behavior

- **Create** (`POST /employees`, admin-only):
  - Validates that no other employee exists for the same `authUserId`.
  - Creates employee with `active = true`.
- **Read**:
  - `/employees`: list all employees (any authenticated user).
  - `/employees/{id}`: get by employee id.
  - `/employees/me`: resolve employee by current user’s `userId` (via `authUserId`).
- **Update** (`PUT /employees/{id}`, admin-only):
  - Partial update: only non-null name/specialization fields applied.
- **Activate / deactivate**:
  - Admin-only; toggles `active` flag.
  - `active` is respected by appointment creation (see next section).

---

### 4. Appointments

#### Entity & status

- **Entity**: `Appointment` (main-service).
- Links:
  - `employee`: `Employee` (doctor).
  - `client`: `Client` (patient).
- Fields: startTime, endTime, status, notes, createdAt, updatedAt.
- Status enum: `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.
  - Default: if status is null at create time, it becomes `SCHEDULED`.

#### Creation rules (`POST /appointments`)

- Request: `employeeId`, `clientId`, `startTime`, `endTime`, optional `notes`.
- Business rules:
  1. `endTime` must be strictly after `startTime` (else 400).
  2. Employee must exist (404) and be `active == true` (409 if not).
  3. Client must exist (404).
  4. Overlaps:
     - Uses repository query to find existing appointments for the **same employee** where:
       - Status is not `CANCELLED`.
       - Time ranges overlap (`start < newEnd` AND `end > newStart`).
     - If any found ⇒ reject with 409 `"Employee already has an appointment in this time slot"`.
  5. If valid:
     - Create appointment with default status `SCHEDULED`.
     - Publish an event `"CREATED"` (see notifications section).

#### Reading & listing

- `GET /appointments/{id}`: fetch by id or 404 `"Appointment not found"`.
- `GET /appointments/employee/{employeeId}`: list all appointments for a given doctor.
- `GET /appointments/client/{clientId}`: list all appointments for a given patient.

#### Status changes

- `PATCH /appointments/{id}/status?status=<AppointmentStatus>`:
  - Fetch appointment or 404.
  - Set `status` to requested enum, **no transition restrictions** enforced.
  - Save and publish `"STATUS_<STATUS>"` event.

Current code allows arbitrary transitions (e.g. `CANCELLED` → `IN_PROGRESS`); stricter FSM rules would need to be added explicitly.

---

### 5. Notifications

#### Event model

- `AppointmentEvent` (shared-lib) contains:
  - `appointmentId`, `eventType`
  - `employeeId`, `employeeName`
  - `clientId`, `clientName`, `clientPhone`, `clientEmail`
  - `startTime`, `endTime`, `status`

#### Publishing (main-service)

- On create (`createAppointment`):
  - Publishes event with `eventType = "CREATED"`.
- On status update (`updateStatus`):
  - Publishes event with `eventType = "STATUS_<STATUS>"`.

#### Consumption (notification-service)

- Single consumer `handleAppointmentEvent`:
  - Logs a summary for every event.
  - For `eventType`:
    - `"CREATED"`:
      - Logs “NOTIFICATION: New appointment” with details.
      - If `clientPhone` present ⇒ logs “Would send SMS…”.
      - If `clientEmail` present ⇒ logs “Would send email…”.
    - `"STATUS_CANCELLED"`:
      - Logs cancellation message.
    - `"STATUS_COMPLETED"`:
      - Logs completion message.
    - Other status events (`STATUS_IN_PROGRESS`, etc.) are logged but have no special handling.

Real SMS/email sending is not yet implemented; the current behavior is logging only, but the event model is ready to support real channels.

---

### 6. Document service (planned)

- Currently only the Spring Boot application class exists; no controllers or models.
- Intended responsibilities (implied by project description):
  - Generate or manage documents such as:
    - Visit summaries.
    - Reports (PDF/Excel).
  - Likely keyed by appointments, patients, or doctors.
- Any implementation should:
  - Define clear document models separate from appointment/patient entities.
  - Consider consuming `AppointmentEvent` or dedicated events for completed visits.

---

### 7. Cross-service & admin flows

#### Linking user ↔ doctor

- Admin flow to create a new doctor today:
  1. Create a `User` in auth-service with role `EMPLOYEE`.
  2. Create an `Employee` in main-service with `authUserId = User.id`.
  3. Doctor logs in using that username/password; application can map them to employee via `/employees/me`.

#### Role enforcement

- Auth-service:
  - Uses method-level security (`@PreAuthorize`) for `/auth/users` (admin-only).
- Main-service:
  - Uses `RequestContext.isAdmin()` (from gateway headers) to guard employee admin operations.
  - All other operations (clients, appointments) are open to any authenticated user.

Any new admin feature (e.g., bulk operations, advanced reporting) should:

- Reuse the same enforcement mechanism (role in JWT → `X-User-Role` → `RequestContext` or `@PreAuthorize`).
- Provide a clear separation between admin and non-admin endpoints and UIs.

