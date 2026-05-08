# ia-dev-kit

A CLI that installs a complete Claude Code development environment into any project with a single command.

```bash
ia-dev-kit generate
```

That's it. Your project gains rules, skills, agents, hooks, templates, and a `CLAUDE.md` — everything Claude Code needs to assist you with engineering work from the very first conversation.

---

## Table of Contents

- [What it does](#what-it-does)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [What gets generated](#what-gets-generated)
- [Development](#development)
- [License](#license)

---

## What it does

`ia-dev-kit` copies a curated bundle of Claude Code resources from the JAR into your project's
`.claude/` directory. The result is a fully configured development environment:

- **Rules** — coding standards and architecture boundaries loaded into every Claude Code session
- **Skills** — slash commands (`/x-implement-story`, `/x-review-pr`, `/x-arch-plan`, and more) covering the full development lifecycle
- **Agents** — specialized AI personas for architecture, security, QA, SRE, and other roles
- **Knowledge packs** — domain knowledge for governance, security patterns, lifecycle contracts, and CI tooling
- **Templates** — planning, review, and documentation templates ready to use
- **Hooks** — a post-compile check that catches Java compilation errors immediately after every edit
- **Settings** — `settings.json` with permissions and hooks pre-configured
- **`CLAUDE.md`** — the executive summary Claude Code loads automatically on every conversation

No configuration required. No internet access at generate time. Everything ships inside the JAR.

---

## Prerequisites

- Java 21 or later

---

## Installation

### Mac / Linux

```bash
git clone https://github.com/edercnj/ia-dev-environment.git
cd ia-dev-environment
bash install.sh
```

The installer builds the JAR, places it in `~/.local/share/ia-dev-kit/`, and adds a `ia-dev-kit`
wrapper to your PATH.

### Windows

```powershell
git clone https://github.com/edercnj/ia-dev-environment.git
cd ia-dev-environment
.\install.ps1
```

### Uninstall

```bash
bash install.sh --uninstall   # Mac / Linux
.\install.ps1 --uninstall     # Windows
```

### From source (no installer)

```bash
mvn clean package
java -jar target/ia-dev-kit-*.jar generate
```

---

## Usage

### Generate into the current directory

```bash
ia-dev-kit generate
```

### Generate into a specific project

```bash
ia-dev-kit generate --output /path/to/your/project
```

### Preview without writing anything

```bash
ia-dev-kit generate --dry-run --verbose
```

### Overwrite existing files

```bash
ia-dev-kit generate --force
```

### Full CLI reference

```
ia-dev-kit generate [OPTIONS]

  -o, --output <DIR>    Target project directory (default: current directory)
  -f, --force           Overwrite existing files
      --dry-run         Simulate without writing any files
  -v, --verbose         List each file as it is copied

  -V, --version         Print version and exit
  -h, --help            Show help and exit
```

After running, `ia-dev-kit` prints a summary of what was installed:

```
Pipeline: Success (65ms)

  Category                Count
  ──────────────────────  ─────
  Agents                     14
  Hooks                       1
  Knowledge                 166
  Root Files                  1
  Rules                       5
  Scripts                    44
  Settings                    1
  Skills                    273
  Templates                  62
  ──────────────────────  ─────
  Total                     567
```

---

## What gets generated

```
<your-project>/
├── CLAUDE.md                    # Executive summary — loaded automatically by Claude Code
└── .claude/
    ├── settings.json            # Permissions and hooks
    ├── agents/                  # 14 AI personas
    │   ├── architect.md
    │   ├── tech-lead.md
    │   ├── security-engineer.md
    │   └── ...
    ├── hooks/
    │   └── post-compile-check.sh
    ├── knowledge/               # 166 domain knowledge packs
    │   ├── governance/
    │   ├── security/
    │   ├── lifecycle/
    │   └── ...
    ├── rules/                   # 5 core rules
    │   └── 00-essentials.md
    ├── scripts/                 # 44 audit and utility scripts
    ├── skills/                  # 273 slash commands
    │   ├── x-implement-story/
    │   ├── x-review-pr/
    │   ├── x-arch-plan/
    │   └── ...
    └── templates/               # 62 planning and review templates
        ├── _TEMPLATE-EPIC.md
        ├── _TEMPLATE-STORY.md
        ├── _TEMPLATE-IMPLEMENTATION-PLAN.md
        └── ...
```

### Agents

14 specialized AI personas used by skills during code reviews, architecture planning, security
audits, and other tasks:

`architect` · `tech-lead` · `security-engineer` · `qa-engineer` · `sre-engineer` ·
`performance-engineer` · `java-developer` · `devops-engineer` · `database-engineer` ·
`api-engineer` · `observability-engineer` · `event-engineer` · `pentest-engineer` ·
`product-owner`

### Skills (slash commands)

Skills are invoked via `/skill-name` in the Claude Code chat. A selection of the most commonly used:

| Skill | Description |
|-------|-------------|
| `/x-implement-story` | Implements a story end-to-end: planning → TDD → review → PR |
| `/x-review-pr` | Tech lead review with GO/NO-GO decision |
| `/x-arch-plan` | Architecture plan with component and sequence diagrams |
| `/x-epic-decompose` | Decomposes a specification into epic, stories, and implementation map |
| `/x-test-plan` | Generates a Double-Loop TDD test plan |
| `/x-test-run` | Runs tests and reports coverage gaps |
| `/x-codebase-audit` | Full codebase quality audit across 6 dimensions |
| `/x-changelog` | Generates changelog from Conventional Commits |
| `/x-ops-troubleshoot` | Systematic diagnosis of compilation, test, and runtime failures |

The full list of 273 skills is available in `.claude/skills/` after generation.

---

## Development

```bash
# Build
mvn clean package

# Run tests with coverage
mvn verify

# Run a dry-run against a temp directory
java -jar target/ia-dev-kit-*.jar generate --output /tmp/test-output --dry-run --verbose
```

### Project structure

```
ia-dev-kit/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/dev/iadevkit/
│   │   │   ├── IaDevKitApplication.java    # CLI entry point
│   │   │   ├── VersionProvider.java
│   │   │   └── command/
│   │   │       └── GenerateCommand.java    # generate subcommand
│   │   └── resources/
│   │       ├── CLAUDE.md                   # template for generated CLAUDE.md
│   │       └── claude/                     # bundled .claude/ resources
│   │           ├── agents/
│   │           ├── hooks/
│   │           ├── knowledge/
│   │           ├── rules/
│   │           ├── scripts/
│   │           ├── skills/
│   │           ├── templates/
│   │           └── settings.json
│   └── test/
│       └── java/dev/iadevkit/
├── install.sh
└── install.ps1
```

### Coverage

| Metric | Threshold | Current |
|--------|-----------|---------|
| Line   | ≥ 85%     | 92%     |
| Branch | ≥ 80%     | 89%     |

Enforced by JaCoCo in `mvn verify`.

---

## License

MIT
