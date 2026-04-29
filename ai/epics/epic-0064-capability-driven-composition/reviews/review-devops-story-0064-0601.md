ENGINEER: DevOps
STORY: story-0064-0601 (audit-capability-graph.sh + Phase 6 partial)
SCORE: 14/18 (DEVOPS-09 N/A — CLI tool, not long-running service)

STATUS: Rejected

### PASSED
- [DEVOPS-01] Multi-stage Dockerfile — builder (eclipse-temurin:21-jdk-alpine) → runtime (eclipse-temurin:21-jre-alpine); build artifacts not in final image
- [DEVOPS-02] Non-root user — `addgroup appgroup && adduser appuser` before COPY/ENTRYPOINT; USER appuser set
- [DEVOPS-03] Minimal base image — eclipse-temurin:21-jre-alpine (JRE-only, Alpine-based) in runtime stage
- [DEVOPS-05] No secrets in image layers — no credentials, tokens, or API keys in Dockerfile or entrypoint
- [DEVOPS-08] Health probe configured — HEALTHCHECK --interval=30s --timeout=5s --retries=3 present in Dockerfile
- [DEVOPS-10] Config externalized — no hardcoded host/port/path; CLI args and env vars used for configuration

### FAILED
- [DEVOPS-04] .dockerignore missing
  - Finding: `ls -la .dockerignore` → MISSING. Build context includes java/target/, .git/, plans/, ai/epics/, governance/ unnecessarily
  - Fix: create .dockerignore with at minimum: `.git`, `java/target/`, `*.md`, `plans/`, `ai/`, `governance/`, `.claude/`, `docs/`; reduces build context and prevents accidental inclusion of sensitive planning artifacts

### PARTIAL
- [DEVOPS-06] Image not pinned to digest
  - Finding: FROM eclipse-temurin:21-jdk-alpine (tag only, no SHA digest). Tag can be mutated by upstream; non-reproducible builds
  - Fix: pin to digest: `FROM eclipse-temurin:21-jdk-alpine@sha256:<digest>` in builder stage
- [DEVOPS-07] No resource limits defined
  - Finding: this is a CLI tool (not a deployed container), so Kubernetes resource limits are not directly applicable; however, no docker run --memory/--cpus guidance in README or CI
  - Recommendation: document expected resource envelope for CI runners using this image

### N/A
- DEVOPS-09: CLI tool, not a long-running service requiring SIGTERM/connection draining
