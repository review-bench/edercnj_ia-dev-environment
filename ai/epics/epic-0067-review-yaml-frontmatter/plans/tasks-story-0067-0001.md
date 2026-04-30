# Task Breakdown — story-0067-0001

**Story:** story-0067-0001

## Tasks

| ID | Description | Layer | Size | Branch |
| :--- | :--- | :--- | :--- | :--- |
| TASK-0067-0001-001 | Create `governance/schemas/review-frontmatter-1.0.json` | Governance | S | `feat/task-0067-0001-001-schema-create` |
| TASK-0067-0001-002 | Modify `_TEMPLATE-SPECIALIST-REVIEW.md` | Resources | S | `feat/task-0067-0001-002-template-specialist` |
| TASK-0067-0001-003 | Modify `_TEMPLATE-TECH-LEAD-REVIEW.md` | Resources | S | `feat/task-0067-0001-003-template-techlead` |
| TASK-0067-0001-004 | Create `ReviewFrontmatterSchemaTest.java` | Test | S | `feat/task-0067-0001-004-schema-test` |
| TASK-0067-0001-005 | Regen 20 golden files + `mvn test` GREEN | Test Fixture | M | `feat/task-0067-0001-005-regen-goldens` |

## ## File Footprint

```yaml
write:
  - governance/schemas/review-frontmatter-1.0.json
  - src/test/java/dev/iadev/governance/ReviewFrontmatterSchemaTest.java
modify:
  - src/main/resources/shared/templates/_TEMPLATE-SPECIALIST-REVIEW.md
  - src/main/resources/shared/templates/_TEMPLATE-TECH-LEAD-REVIEW.md
regen:
  - .claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md
  - .claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md
  - src/test/resources/golden/java-spring/.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md
  - src/test/resources/golden/java-spring/.claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md
  - src/test/resources/golden/java-quarkus/.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md
  - src/test/resources/golden/java-quarkus/.claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md
  - src/test/resources/golden/java-spring-hexagonal/.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md
  - src/test/resources/golden/java-spring-hexagonal/.claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md
  - src/test/resources/golden/java-spring-elasticsearch/.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md
  - src/test/resources/golden/java-spring-elasticsearch/.claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md
  - src/test/resources/golden/java-spring-fintech-pci/.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md
  - src/test/resources/golden/java-spring-fintech-pci/.claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md
  - src/test/resources/golden/java-spring-cqrs-es/.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md
  - src/test/resources/golden/java-spring-cqrs-es/.claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md
  - src/test/resources/golden/java-spring-clickhouse/.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md
  - src/test/resources/golden/java-spring-clickhouse/.claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md
  - src/test/resources/golden/java-spring-neo4j/.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md
  - src/test/resources/golden/java-spring-neo4j/.claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md
  - src/test/resources/golden/java-spring-event-driven/.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md
  - src/test/resources/golden/java-spring-event-driven/.claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md
  - src/test/resources/golden/java-spring/platform-claude-code/.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md
  - src/test/resources/golden/java-spring/platform-claude-code/.claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md
```
