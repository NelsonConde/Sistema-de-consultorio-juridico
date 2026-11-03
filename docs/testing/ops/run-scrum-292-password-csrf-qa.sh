#!/usr/bin/env bash
# Independent QA Execution Runner for SCRUM-292
# Verifies password change, session invalidation policy, CSRF handling, and zero credential exposure.
set -euo pipefail
umask 077

repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)"

for executable in java bash; do
    command -v "$executable" >/dev/null || { echo "Missing executable: $executable" >&2; exit 1; }
done

printf '=== SCRUM-292: Password Change, CSRF, and Zero Secret Exposure Verification ===\n'
printf 'Timestamp: %s\n' "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
printf 'Java version: %s\n' "$(java -version 2>&1 | head -n 1)"

cd "$repository_root/backend/app"

if [[ -x "/tmp/legal-cases-tools/apache-maven-3.9.14/bin/mvn" ]]; then
    MVN_CMD="/tmp/legal-cases-tools/apache-maven-3.9.14/bin/mvn"
else
    MVN_CMD="./mvnw"
fi

"$MVN_CMD" -B \
    -Dtest="PasswordChangeCsrfPolicyQaTest,AuthControllerCsrfTest,AuthServicePasswordSessionTest,CsrfConfigTest,LoggingSafetyConfigurationTest,PasswordResetServiceAuthVersionTest" \
    -Dlogging.level.root=WARN \
    test

printf '\n=== SCRUM-292 Execution completed successfully ===\n'
