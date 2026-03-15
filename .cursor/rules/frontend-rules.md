## Frontend rules (Vue + PrimeVue)

These rules describe patterns for the Vue 3 frontend under `frontend/`.

### 1. Stack & structure

- Framework: **Vue 3 + TypeScript** (script setup).
- UI: **PrimeVue** with Aura theme.
- State: **Pinia** (`stores/`).
- Routing: **Vue Router** (`router/index.ts`).
- Build: **Vite**.

Folder layout:

- Views/pages: `src/views/*.vue`
- Layouts: `src/layouts/*.vue`
- API clients: `src/api/*.ts`
- Stores: `src/stores/*.ts`
- Utilities: `src/utils/*.ts`

### 2. API client pattern

For each backend resource, create a dedicated client in `src/api/`:

- Use the shared Axios instance from `src/api/http.ts`.
- Export **typed** functions that:
  - Accept narrow typed request objects.
  - Return `Promise<SpecificType>` (never raw Axios responses).
- Example pattern:

```ts
export interface Thing { /* ... */ }

export function getThings(): Promise<Thing[]> {
  return http.get<Thing[]>('/main/things').then((res) => res.data)
}
```

Do **not** build URLs manually outside the API layer; views should call these helpers.

### 3. Views and layout

- All authenticated pages render inside `AppLayout.vue`:
  - Top Menubar + content slot.
  - Use `h1` for page titles and a short subtitle when helpful.
- For CRUD pages:
  - Use `DataTable` for lists.
  - Provide:
    - Search input (local filtering).
    - Pagination (client‑side).
    - Row actions as icon buttons (`pi pi-pencil`, `pi pi-trash`, etc.).
  - Use `Dialog` for create/edit forms with explicit validation/toasts.

### 4. Validation and UX

- Use small utility helpers (e.g. `isBlankInput`) for simple checks.
- Validation UX:
  - Show **toast warnings** for user‑facing validation errors.
  - Block submission when required fields are empty or invalid.
- For server responses:
  - Prefer a shared `getErrorMessage(err, fallback)` helper pattern.
  - Show error toasts with clear `summary` (“Save failed”, “Load failed”).

### 5. Auth & role handling

- Auth tokens and role live in `useAuthStore`:
  - `isAuthenticated` = token present.
  - `isAdmin` = role is `ADMIN`.
- Router guard:
  - `meta.public === true` for login page only.
  - `meta.adminOnly === true` for admin routes (e.g. `/admin/users`).
- UI:
  - Use `authStore.isAdmin` to hide admin‑only buttons/menus (e.g. “Add Doctor”, admin menu).
  - Never rely on UI checks alone; backend already enforces roles.

### 6. New pages

When adding a new page:

1. Create API client in `src/api/`.
2. Add a new route in `router/index.ts`.
3. Add a menu item in `AppLayout.vue` if appropriate.
4. Implement the view in `src/views/` following the patterns from existing pages (`PatientsView`, `DoctorsView`, `UsersView`, `ProfileView`).

