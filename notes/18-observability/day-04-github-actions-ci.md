# W18 D4 — GitHub Actions CI

## Definition & Why
CI runs the full build + test suite on every push and PR, in a clean environment, so "works on my machine" never ships. A green check is a precondition for merge; a red one blocks it. It also proves to a reviewer/recruiter that the project actually builds and its tests pass.

## Mental Model
A fresh VM, every time: check out the code, install exactly JDK 25 (matching the pom), restore the Maven cache, run `./mvnw -B verify`. `verify` is the Maven phase that compiles, runs unit tests *and* the integration tests (Testcontainers Postgres, embedded Kafka) — Docker is preinstalled on GitHub-hosted runners, so those just work.

## My Roost Code (today)
- `.github/workflows/ci.yml` — triggers on push to `main` and all PRs; `actions/setup-java@v4` with `distribution: temurin`, `java-version: 25`, `cache: maven`; runs `./mvnw -B verify`.

## Interview Points
- **`-B` / `--batch-mode`**: non-interactive; suppresses download-progress spam so logs stay readable.
- **Testcontainers in CI**: Docker is available on GitHub-hosted runners; on self-hosted you must verify it's installed.
- **Fail-fast**: stops on first failure — saves CI minutes but hides whether later stages would also fail. A trade-off.
- **PR vs nightly**: run fast/critical tests on every PR; push very slow suites to nightly.
- **Secrets**: `${{ secrets.NAME }}` — encrypted, injected as env/inputs, never printed.

## Common Mistakes
- JDK version in CI drifting from the pom → "compiles locally, fails in CI".
- No dependency cache → every run re-downloads the world (slow, flaky).
- Running Testcontainers on a runner without Docker.
- Echoing secrets into logs.

## Quick Revision + Q&A
**Q: Why `-B`?** Non-interactive batch mode; clean logs.
**Q: Do Testcontainers work in CI?** Yes on GitHub-hosted runners (Docker preinstalled).
**Q: Every PR or nightly for integration tests?** Critical paths every PR; heavy suites nightly.
