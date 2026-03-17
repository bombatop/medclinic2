---
description: "Appointments entity, creation rules, overlap check, status for MedClinic 2"
alwaysApply: false
globs: ["backend/main-service/**/*Appointment*", "backend/main-service/**/*appointment*", "frontend/src/api/appointments.ts", "frontend/src/views/AppointmentsView.vue"]
---

## Domain: Appointments

### Entity

- **Appointment** (main-service). Links: employee (Employee), client (Client).
- Fields: startTime, endTime, status, notes, createdAt, updatedAt.
- Status: `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`. Default at create: `SCHEDULED`.

### Creation (`POST /appointments`)

Request: employeeId, clientId, startTime, endTime, optional notes.

Rules:
1. `endTime` > `startTime` (else 400).
2. Employee exists (404) and `active == true` (409 if not).
3. Client exists (404).
4. No overlap: same employee, status ≠ CANCELLED, time ranges overlap → 409 `"Employee already has an appointment in this time slot"`.
5. If valid: create with status `SCHEDULED`; publish `"CREATED"` event.

### Read

- `GET /appointments/{id}`: by id or 404.
- `GET /appointments/employee/{employeeId}`: list for doctor.
- `GET /appointments/client/{clientId}`: list for patient.

### Status (`PATCH /appointments/{id}/status?status=<status>`)

- Fetch or 404; set status; save; publish `"STATUS_<STATUS>"` event.
- No transition restrictions (arbitrary transitions allowed; FSM would need explicit addition).
