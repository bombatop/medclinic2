# Auth, CORS, and cookies

## Model

- **Access token:** JWT in memory (Pinia) after login or after a successful **refresh**. Sent as `Authorization: Bearer <access>` on API requests via the shared Axios instance ([`src/api/http.ts`](../src/api/http.ts)).
- **Refresh token:** HttpOnly cookie (`refresh_token`, path `/api/auth`) set by the auth service on login and refresh. The browser sends it automatically on `POST /api/auth/auth/refresh` and `POST /api/auth/auth/logout` when `withCredentials: true` (used for login, refresh, and the shared client).

## Local development

- SPA (Vite) is typically on `http://localhost:3000`; the gateway on `http://localhost:8080`. The gateway CORS config must list the SPA origin and use `allowCredentials: true` so the refresh cookie can be set and sent.

## Production

- Serve the SPA and call the API over **HTTPS**.
- Set **`VITE_API_BASE_URL`** to an `https://` gateway URL (see [`.env.example`](../.env.example)).
- Ensure gateway **allowed origins** include the real SPA origin (not `*` when using credentials).
- If the SPA and API are on **different registrable domains**, the refresh cookie will need `SameSite=None` and `Secure`, and CORS must be configured accordingly (backend change).

## Threat model note

The access token in memory is still reachable by malicious script running on the same origin (XSS). Persisting it in `localStorage` was removed to avoid **persistent** theft after XSS and to reduce exposure across sessions. True HttpOnly access tokens would require gateway/backend support.
