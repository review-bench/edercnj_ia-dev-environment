# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-05-08

This is the first release of `ia-dev-kit` — a focused CLI that installs a complete Claude Code
development environment into any project with a single command.

### What it does

`ia-dev-kit generate` copies a curated set of Claude Code resources directly into your project's
`.claude/` directory. No configuration files, no YAML, no internet connection required. Everything
ships inside the JAR and lands exactly where Claude Code expects it.

One command. One result. A project ready for AI-assisted development.

### What's included

Running `ia-dev-kit generate` delivers **567 resources** across 9 categories:

| Category   | Count | What you get                                                                         |
|------------|------:|--------------------------------------------------------------------------------------|
| Agents     |    14 | Specialized AI personas — architect, tech-lead, security-engineer, QA, SRE, and more |
| Hooks      |     1 | Post-compile check — catches Java compilation errors immediately after every edit     |
| Knowledge  |   166 | Domain knowledge packs for governance, security, architecture, and lifecycle patterns |
| Rules      |     5 | Core coding and architecture standards loaded into every Claude Code session          |
| Scripts    |    44 | Audit, preflight, and setup utilities for CI and local development                   |
| Settings   |     1 | `settings.json` with permissions and the post-compile hook wired up                  |
| Skills     |   273 | Slash commands covering the full development lifecycle                               |
| Templates  |    62 | Planning, review, and documentation templates                                        |
| Root Files |     1 | `CLAUDE.md` — the executive summary Claude Code loads on every conversation          |

### CLI reference

```text
ia-dev-kit generate [OPTIONS]

  -o, --output <DIR>    Target project directory (default: current directory)
  -f, --force           Overwrite existing files
      --dry-run         Simulate without writing any files
  -v, --verbose         List each file as it is copied

  -V, --version         Print version and exit
  -h, --help            Show help and exit
```

### Installation

```bash
bash install.sh          # Mac / Linux — installs ia-dev-kit to ~/.local/share/ia-dev-kit/
install.ps1              # Windows
bash install.sh --uninstall  # Remove everything
```

Requires Java 21 or later.
