# Dependency audit

Run regularly (e.g. weekly or before a release):

```bash
cd frontend
npm audit
npm audit --production
```

Treat **high** and **critical** findings as blocking unless explicitly accepted and documented. Patch `axios`, `vue`, and other runtime dependencies on vendor security advisories.

When you add CI/CD later, consider wiring `npm audit --audit-level=high` (and `npm run type-check` / `npm run lint`) into the pipeline.
