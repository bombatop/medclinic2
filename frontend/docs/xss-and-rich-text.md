# XSS surface and rich text

## Ongoing checks

From the repo root, periodically search the frontend for dangerous sinks:

```bash
rg "v-html|innerHTML" frontend/src
```

There should be no matches unless a feature explicitly requires HTML rendering.

## Policy

- **Default:** Use Vue text interpolation (`{{ }}`) only. The `vue/no-v-html` ESLint rule is enabled to block accidental `v-html`.
- **If you need HTML from the server or rich text editors:** sanitize before render (e.g. [DOMPurify](https://github.com/cure53/DOMPurify) with a strict allowlist), narrow **Content-Security-Policy** at the edge, and treat it as a security review item.

## PrimeVue / components

Prefer components that render plain text for user-supplied strings. Any third-party widget that sets `innerHTML` should be audited the same way as `v-html`.
