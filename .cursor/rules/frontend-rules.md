---
description: "Vue 3, PrimeVue, Vite, TypeScript, views, API clients, DataTable, and frontend structure for MedClinic 2"
alwaysApply: false
globs: ["frontend/**/*.{vue,ts,js}"]
---

## Frontend rules (Vue + PrimeVue)

### 1. Rebuild after frontend changes

After any change to frontend code, rebuild and restart: `docker compose build frontend && docker compose up -d frontend`. Never use `--with-dependencies` for frontend-only changes.

The **running** `medclinic-frontend` container is **Nginx only** — there is no `node`/`npm` inside it. The Node toolchain exists only in the image **build** stage ([`frontend/Dockerfile`](frontend/Dockerfile): `FROM node:22-alpine AS build`).

**Docker image build is gated:** `docker compose build frontend` runs **`npm run type-check`** and **`npm run lint:check`** (oxlint + eslint **without** `--fix`) before `vite build`. The build fails if TypeScript or lint errors exist.

Locally with Node: `npm run type-check`, `npm run lint` (with `--fix`), or `npm run lint:check` to verify without mutating files.

### 2. Stack & structure

- Vue 3 + TypeScript (script setup), PrimeVue (Aura), Pinia, Vue Router, Vite.
- Layout: `views/`, `layouts/`, `api/`, `stores/`, `utils/`.

### 3. API client pattern

For each backend resource, create a typed client in `src/api/` using `http.ts`. Export functions returning `Promise<Type>`; never raw Axios. Do not build URLs outside the API layer.

### 4. Views and layout

- All authenticated pages render inside `AppLayout.vue`:
  - Top Menubar + content slot.
  - Use `h1` for page titles and a short subtitle when helpful.
- For CRUD pages:
  - Use `DataTable` for lists.
  - Provide:
    - Search input (local filtering).
    - **Pagination (server-side, lazy loading)** — use `lazyParams` with `sortField`/`sortOrder`, pass `page`, `size`, `sort` to API; handle `@page` and `@sort` events.
    - Row actions as icon buttons (`pi pi-pencil`, `pi pi-trash`, etc.).
  - Use `Dialog` for create/edit forms with explicit validation/toasts.

### 5. Validation and UX

- Toast for validation errors; block submit when invalid. Use `getErrorMessage(err, fallback)` for server errors.

### 6. Auth & role handling

- `useAuthStore`: `isAuthenticated`, `roles`, `permissions`, `hasPermission(code)`, `canAccessAdmin`, `canManageRbac`. Router: `meta.public` (login only), `meta.adminOnly` (requires `canAccessAdmin`), `meta.permission` (single permission code). Use `authStore.hasPermission(code)` or `authStore.canAccessAdmin` for UI; backend enforces.
- **Security baseline (non-negotiable):** access token and role/permission **snapshots stay in memory only** — not `localStorage`. Refresh uses **HttpOnly cookie** + `bootstrapAuth()` on startup. Before merging auth/HTTP/env/login changes, follow [`frontend/docs/security-baseline.md`](frontend/docs/security-baseline.md).

### 7. Avoid

- Client-side pagination; DIY when PrimeVue/Spring provides it; workarounds for architectural limits.

### 8. New pages

1. API client in `src/api/`. 2. Route in `router/index.ts`. 3. Menu in `AppLayout.vue` if needed. 4. View in `src/views/` (follow `PatientsView`, `DoctorsView`, `UsersView`).
