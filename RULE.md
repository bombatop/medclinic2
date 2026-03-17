## Cursor rules for MedClinic 2

This project uses Cursor heavily. When working here (with any account), follow these repo‑local rules:

- **Prefer plans for multi‑step work**  
  - For anything touching more than one file or feature, create or continue a plan in `.cursor/plans/`.  
  - Keep plans and commits focused on a single feature (e.g. “Appointments page”, “Notification service email sending”).

- **Use the existing Docker patterns**  
  - Backend microservices are built via the root `docker-compose.yml` and the shared `backend/Dockerfile`.  
  - Frontend uses `frontend/Dockerfile` + `nginx.conf` (Vite build → static files → Nginx).  
  - New services should copy these patterns instead of inventing new container layouts.

- **Follow frontend architecture rules**  
  - Vue 3 + PrimeVue + Vite; TypeScript everywhere.  
  - Each backend resource should have an API client in `frontend/src/api/` and a corresponding view in `frontend/src/views/`.  
  - Tables: use `DataTable` with local search/filtering and pagination (no backend paging yet).

- **Respect roles and admin boundaries**  
  - Roles are `ADMIN`, `DOCTOR`, and `RECEPTIONIST` (multi-role capable).  
  - Frontend gets the role from login and exposes `authStore.isAdmin`.  
  - Admin‑only features must be guarded both in the router (`meta.adminOnly`) and in the UI (hide buttons, links).

- **Do NOT encode business rules here (yet)**  
  - Keep this file focused on tooling, architecture, and collaboration.  
  - Domain/business logic rules (e.g. appointment flows) should go into a separate rules file that the maintainer curates.

See more detailed rules in `.cursor/rules/`.

### How Cursor uses `.cursor/rules/`

- **Automatic**: Rules in `.cursor/rules/` are Project Rules. Each file has frontmatter (`description`, `globs`, `alwaysApply`). Cursor applies them when relevant (e.g. when you edit files matching the globs, or when the description matches the task). `ai-collaboration.md` is set to **Always Apply** so planning and scope guidance is always in context.
- **Manual**: In Chat, type `@` and pick a rule (e.g. `@domain-rules.md` or `@.cursor/rules/domain-rules.md`) to attach it to the conversation.
- **Settings**: Open **Cursor Settings → Rules** to see project rules and toggle them if needed.

