#!/usr/bin/env bash
# Creates only a new synthetic cluster; never accepts a caller's database URL.
set -euo pipefail
umask 077
repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)"
for executable in initdb pg_ctl createdb python3; do
    command -v "$executable" >/dev/null || { echo "Missing executable: $executable" >&2; exit 1; }
done
qa_directory="$(mktemp -d "${TMPDIR:-/tmp}/legal-cases-sprint7-qa.XXXXXXXX")"
cleanup() {
    local result=$?
    trap - EXIT
    if [[ -f "$qa_directory/data/postmaster.pid" ]]; then
        if ! pg_ctl -D "$qa_directory/data" -m fast -w stop >/dev/null; then
            echo "Could not stop QA PostgreSQL; retained $qa_directory" >&2
            exit 1
        fi
    fi
    rm -rf -- "$qa_directory"
    exit "$result"
}
trap cleanup EXIT
qa_port="$(python3 - <<'PY'
import socket
with socket.socket() as listener:
    listener.bind(('127.0.0.1', 0))
    print(listener.getsockname()[1])
PY
)"
mkdir "$qa_directory/socket"
initdb -D "$qa_directory/data" -A trust --no-locale -U legal_cases_qa > "$qa_directory/init.log"
pg_ctl -D "$qa_directory/data" -l "$qa_directory/server.log" \
    -o "-h 127.0.0.1 -p $qa_port -k $qa_directory/socket" -w start
createdb -h 127.0.0.1 -p "$qa_port" -U legal_cases_qa legal_cases_qa_sprint7
export LEGAL_CASES_QA_DB_URL="jdbc:postgresql://127.0.0.1:$qa_port/legal_cases_qa_sprint7"
printf 'Synthetic QA only; '
pg_config --version
cd "$repository_root/backend/app"
bash ./mvnw -B -Dtest=Sprint7LocalQaPostgreSqlTest,EstudianteCommandServiceConciliacionTest,EstudianteExcelServiceConciliacionTest,EstudianteSelectorPaginadoTest,ConsultaControllerAuthorizationTest \
    -Dlogging.level.root=WARN test
