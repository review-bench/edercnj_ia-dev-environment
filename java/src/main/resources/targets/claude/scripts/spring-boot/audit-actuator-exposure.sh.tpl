#!/usr/bin/env bash
set -euo pipefail
# audit-actuator-exposure.sh — Spring Boot CIS: actuator exposure check
# Verifies that /actuator/env and /actuator/heapdump are NOT exposed in production profiles
# Build tool: {{BUILD_TOOL}} | Test command: {{TEST_COMMAND}}
case "${1:-}" in
  --self-check) exit 0 ;;
esac
violations=0
for config in application-prod.yml application-production.yml application.yml; do
  if [[ -f "$config" ]]; then
    if grep -q "actuator.*env\|actuator.*heapdump" "$config" 2>/dev/null; then
      echo "ACTUATOR_EXPOSURE_VIOLATION: $config exposes sensitive actuator endpoint" >&2
      violations=$((violations + 1))
    fi
  fi
done
exit $((violations > 0 ? 1 : 0))
