## AI collaboration rules (Cursor)

These rules describe **how AI should work in this repo**, independent of any individual Cursor account.

### 1. Planning and scope

- For non‑trivial tasks (more than 1–2 files or obvious edits), **start from a plan**:
  - Create or extend a plan in `.cursor/plans/`.
  - Keep each plan focused on one feature or refactor.
  - Treat plans as living documents for the duration of a feature branch.
- Prefer **feature branches** such as:
  - `feature/appointments-page`
  - `feature/admin-panel`
  - `feature/notification-emails`

### 2. Git & commits

- **Merge messages**: Use the format `Merge <source-branch> into <target-branch>` (e.g. `Merge refresh-token into frontend`, `Merge rules-config into frontend`).
- **Always merge with `--no-ff`** so a merge commit is created; never fast-forward when merging feature branches.
- Keep commits **small and coherent**:
  - One behavioral change or feature per commit.
  - Avoid mixing infra/formatting with business logic in the same commit.
- When using AI to stage changes:
  - Stage only files directly related to the current feature.
  - Leave any ambiguous changes unstaged for manual review.
- Never rewrite history on shared branches from AI (no force‑push to `main`).

### 3. Code style & patterns

- Follow existing patterns whenever introducing new code:
  - For **frontend** see `frontend-rules.md`.
  - For **backend & infrastructure** see `backend-infra-rules.md` and `docker-rules.md`.
- Prefer **minimal changes** when fixing bugs:
  - Avoid opportunistic refactors unless explicitly requested.
  - If a refactor is needed, create a separate plan/branch for it.

### 4. Safety and environment

- Assume **docker‑based development** by default:
  - Prefer changes that keep `docker-compose.yml` as the primary entrypoint.
  - Keep local‑only tooling dependencies (e.g. Node/Java on host) optional.
- Do not introduce new external services or third‑party SaaS APIs without an explicit plan and justification.

### 5. Domain rules (reserved)

- Do **not** infer or encode domain/business rules here.
  - Keep those in a dedicated rules file maintained manually by the project owner.
  - AI may propose updates, but the owner must approve and commit them.

