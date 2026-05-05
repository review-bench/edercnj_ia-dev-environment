# Architecture Plan — story-0077-0007

## Overview
Feature template system with domain entities, validator, and CI smoke gate.

## Hexagonal Structure

```
domain/feature/
  Feature.java              — entity (featureId, capabilityId, useCases, acceptanceCriteria)
  UseCase.java              — value object (actor, action, benefit)
  AcceptanceCriterion.java  — value object (gherkin scenario text)
  FeatureValidator.java     — domain service (validates Feature completeness)

application/feature/
  FeatureToStoryDecompositionUseCase.java — derives story count from use cases

ai/templates/
  _TEMPLATE-FEATURE.md     — 7-section template

ai/examples/
  example-feature-oauth2-integration.md
  example-feature-mfa-support.md

ci/smoke/
  feature-template-smoke.sh — validates 7 sections in template + example
```

## Key Decisions
- UseCase and AcceptanceCriterion are value objects (no ID, structural equality)
- Feature has List<UseCase> (3–8) and List<AcceptanceCriterion> (10+)
- FeatureValidator hard-blocks: empty useCases, empty acceptanceCriteria
- Story count derived as useCases.size() (clamped to 3–7)
