#!/usr/bin/env bash
# Reproducible SCRUM-292 backend QA suite; output remains in the Maven reports.
set -euo pipefail
umask 077

repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$repository_root/backend/app"
command -v java >/dev/null || { printf 'Java 21 is required\n' >&2; exit 1; }
[[ -f ./mvnw ]] || { printf 'Missing Maven Wrapper\n' >&2; exit 1; }

printf 'SCRUM-292 QA verification using versioned Maven Wrapper\n'
./mvnw -B \
    -Dtest="PasswordChangeCsrfPolicyQaTest,AuthControllerCsrfTest,AuthServicePasswordSessionTest,CsrfConfigTest,CsrfFilterSecurityTest,JwtAuthenticationFilterAuthVersionTest,AuditAspectTest,LoggingSafetyConfigurationTest,PasswordResetServiceAuthVersionTest" \
    -Dlogging.level.root=WARN \
    test
