# Network baseline (inventory)

Use this as a checklist when profiling in DevTools (XHR/fetch).

| Area | Endpoints / pattern |
|------|---------------------|
| Appointment selects | `GET /main/doctors`, `GET /main/patients` with `page=0&size=500` — cached in Pinia `referenceData` (5 min TTL, invalidated on patient/doctor mutations). |
| Appointment table | `GET /main/appointments` with pagination + filters; superseded loads aborted via `AbortSignal`. |
| Timetable | Same appointments GET with `size=500`; abort on supersede / unmount. |

Re-check after changes: duplicate request count when revisiting Appointments, response sizes, and canceled requests when changing filters quickly.
