# ia-dev-environment

The source-of-truth **store** for a complete Claude Code development environment:
rules, skills, agents, knowledge packs, templates, and `settings.json`.

Install it into any project with a single command:

```bash
bin/install-claude-resources.sh --output /path/to/your/project
```

Your project gains rules, skills, agents, templates, and a `CLAUDE.md` — everything
Claude Code needs to assist with engineering work from the very first conversation.

---

## Table of Contents

- [What it does](#what-it-does)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [What gets installed](#what-gets-installed)
- [Repository layout](#repository-layout)
- [License](#license)

---

## What it does

This repository **is** the resource store. The canonical content lives in `resources/`
and is version-controlled. `bin/install-claude-resources.sh` performs a verbatim
recursive copy of `resources/` into a target project's `.claude/` directory, plus a
`CLAUDE.md` stub. The result is a fully configured environment:

- **Rules** — coding standards and architecture boundaries loaded into every Claude Code session
- **Skills** — slash commands (`/x-implement-story`, `/x-review-pr`, `/x-plan-architecture`, and more) covering the full development lifecycle
- **Agents** — specialized AI personas for architecture, security, QA, SRE, and other roles
- **Knowledge packs** — domain knowledge for governance, security patterns, lifecycle contracts, and CI tooling
- **Templates** — planning, review, and documentation templates ready to use
- **Settings** — `settings.json` with permissions pre-configured

There is no build step and no templating: `{{PLACEHOLDER}}` tokens in skill bodies
are resolved at runtime by Claude itself. No internet access is required.

---

## Prerequisites

- `bash` and standard POSIX tools (`find`, `cp`) — already present on macOS and Linux

---

## Installation

Clone the repository and run the installer against your target project:

```bash
git clone https://github.com/edercnj/ia-dev-environment.git
cd ia-dev-environment
bin/install-claude-resources.sh --output /path/to/your/project
```

To make this repository's own Claude Code session use the resources directly
(the local `.claude/` is gitignored):

```bash
bin/install-claude-resources.sh --output . --force
```

---

## Usage

```
bin/install-claude-resources.sh [OPTIONS]

  -o, --output <DIR>    Target project directory (default: current directory)
  -f, --force           Overwrite existing files (default: skip existing)
      --dry-run         Simulate without writing any files
  -v, --verbose         List each file as it is copied or skipped
  -h, --help            Show help and exit
```

Examples:

```bash
# Install into the current directory
bin/install-claude-resources.sh

# Install into a specific project
bin/install-claude-resources.sh --output /path/to/your/project

# Preview without writing anything
bin/install-claude-resources.sh --dry-run --verbose

# Overwrite existing files
bin/install-claude-resources.sh --force
```

After running, the installer prints a summary of what was installed:

```
Pipeline: Success (3940ms)

  Category                Count
  ──────────────────────  ─────
  Agents                     14
  Knowledge                 176
  Root Files                  1
  Rules                       4
  Scripts                    43
  Settings                    1
  Skills                    303
  Templates                  63
  ──────────────────────  ─────
  Total                     605
```

---

## What gets installed

```
<your-project>/
├── CLAUDE.md                    # Executive summary — loaded automatically by Claude Code
└── .claude/
    ├── settings.json            # Permissions
    ├── agents/                  # 14 AI personas
    ├── knowledge/               # 176 domain knowledge packs
    ├── rules/                   # core rules
    ├── scripts/                 # 43 audit and utility scripts
    ├── skills/                  # 303 slash commands
    └── templates/               # 63 planning and review templates
```

### Agents

14 specialized AI personas used by skills during code reviews, architecture planning,
security audits, and other tasks:

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
| `/x-plan-architecture` | Architecture plan with component and sequence diagrams |
| `/x-epic-create` | Decomposes a specification into epic, stories, and implementation map |
| `/x-plan-tests` | Generates a Double-Loop TDD test plan |
| `/x-execute-tests` | Runs tests and reports coverage gaps |
| `/x-audit-code` | Full codebase quality audit across specialist dimensions |
| `/x-generate-release-changelog` | Generates changelog from Conventional Commits |
| `/x-troubleshoot-operations` | Systematic diagnosis of compilation, test, and runtime failures |

The full list of skills is available in `resources/skills/`.

---

## Repository layout

```
ia-dev-environment/
├── CLAUDE.md                    # This repo's own project guide (rich)
├── CLAUDE.template.md           # 6-line stub installed into consumer projects
├── bin/
│   └── install-claude-resources.sh
└── resources/                   # ← SOURCE OF TRUTH (canonical, version-controlled)
    ├── agents/
    ├── knowledge/
    ├── rules/
    ├── scripts/
    ├── skills/
    ├── templates/
    └── settings.json
```

Edit content under `resources/` and re-run the installer to propagate changes.
The `.claude/` directory is a local, gitignored install — never edit it directly.

---

## License

MIT
