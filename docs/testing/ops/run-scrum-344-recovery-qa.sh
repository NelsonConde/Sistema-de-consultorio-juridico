#!/usr/bin/env bash
# Independent QA Execution Runner for SCRUM-344
# Simulates loss only in disposable environment; never contacts production.
set -euo pipefail
umask 077

repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)"

for executable in java python3 bash; do
    command -v "$executable" >/dev/null || { echo "Missing executable: $executable" >&2; exit 1; }
done

printf '=== SCRUM-344: Coordinated Recovery and Presigned URL TTL Verification ===\n'
printf 'Timestamp: %s\n' "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
printf 'Java version: %s\n' "$(java -version 2>&1 | head -n 1)"

cd "$repository_root/backend/app"

if [[ -x "/tmp/legal-cases-tools/apache-maven-3.9.14/bin/mvn" ]]; then
    MVN_CMD="/tmp/legal-cases-tools/apache-maven-3.9.14/bin/mvn"
else
    MVN_CMD="./mvnw"
fi

"$MVN_CMD" -B \
    -Dtest=DocumentCoordinatedRecoveryQaTest \
    -Dlogging.level.root=WARN \
    test

printf '\n=== SCRUM-344 Execution completed successfully ===\n'
