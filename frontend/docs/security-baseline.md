# Security baseline — do not regress

**Audience:** contributors and AI agents before opening a PR that touches auth, HTTP, env, or login flows.

## Invariants (shameful to break)

1. **Access token** — Only in **Pinia (memory)** after login or `bootstrapAuth()` / refresh. **Never** `localStorage` / `sessionStorage` for JWT or permission mirrors.
2. **Refresh token** — Only via **HttpOnly cookie** + `withCredentials` on auth endpoints; **never** read refresh token in JS.
3. **API client** — Normal calls go through [`src/api/http.ts`](../src/api/http.ts) (Bearer + `withCredentials` as configured). Do not bypass for “just this once” without a documented reason.
4. **Secrets** — No secrets in `VITE_*` or the bundle. Only public config (e.g. API base URL).
5. **Redirects after login** — Use [`safeRedirectAfterLogin`](../src/utils/navigation.ts); do not navigate from raw `route.query.redirect`.
6. **XSS** — No `v-html` with unsanitized content (ESLint `vue/no-v-html` is on). Rich text needs explicit review + sanitization.
7. **403** — Central handling lives in the HTTP layer; do not remove without replacing.

## After you think you are done

- [ ] `npm run type-check` and `npm run lint`.
- [ ] No new `localStorage` / `sessionStorage` for auth-related keys.
- [ ] No tokens or passwords logged or reflected in URLs.
- [ ] If you changed CORS, cookies, or gateway: re-read [`auth-cors-and-cookies.md`](auth-cors-and-cookies.md).

## Pointers

- Auth architecture: [auth-cors-and-cookies.md](auth-cors-and-cookies.md)  
- XSS / rich text: [xss-and-rich-text.md](xss-and-rich-text.md)  
- Dependencies: [dependency-audit.md](dependency-audit.md)
