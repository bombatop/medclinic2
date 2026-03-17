---
description: "Planning, scope, branches, commits, and how AI should collaborate in this repo"
alwaysApply: true
---

## AI collaboration rules (Cursor)

These rules describe **how AI should work in this repo**, independent of any individual Cursor account.

### 1. Planning and scope

- For non‑trivial tasks (more than 1–2 files), create a plan in `.cursor/plans/` and use feature branches (`feature/...`).

### 2. Git & commits

- **Commit messages**: lowercase, no tool-attribution text, describe plainly. Do not include .plan.md files in commits.
- **Merge messages**: Use format `Merge <source-branch> into <target-branch>` (e.g. `Merge refresh-token into frontend`).
- **Always merge with `--no-ff`** so a merge commit is created; never fast-forward when merging feature branches.
- Keep commits **small and coherent**:
  - One behavioral change or feature per commit.
  - Avoid mixing infra/formatting with business logic in the same commit.
- When using AI to stage changes:
  - Stage only files directly related to the current feature.
  - Leave any ambiguous changes unstaged for manual review.
- Never rewrite history on shared branches from AI (no force‑push to `main`).

### 3. Code style & patterns

- Follow existing patterns: `frontend-rules.md`, `backend-infra-rules.md`, `docker-rules.md`, `domain-*.md`.
- Prefer **minimal changes** when fixing bugs:
  - Avoid opportunistic refactors unless explicitly requested.
  - If a refactor is needed, create a separate plan/branch for it.

### 4. Safety and environment

- Assume **docker‑based development** by default:
  - Prefer changes that keep `docker-compose.yml` as the primary entrypoint.
  - Keep local‑only tooling dependencies (e.g. Node/Java on host) optional.
- Do not introduce new external services or third‑party SaaS APIs without an explicit plan and justification.

