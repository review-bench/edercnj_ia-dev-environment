# Architecture Plan — story-0077-0008

## Overview
Add Source Feature + Inherited RNFs to EPIC template (v3). Domain: SourceFeatureReference VO, EpicV2V3Loader. 3 tasks.

## Key Decisions
- SourceFeatureReference is optional; absent = "N/A" (backwards compat)
- InheritedRNFs are read-only — validated by domain service, cannot be relaxed at epic level
- v2 epics auto-converted by EpicV2V3Loader transparently
