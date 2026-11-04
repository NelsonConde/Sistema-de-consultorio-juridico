#!/usr/bin/env python3
"""Exercise built migration artifacts against a disposable synthetic PostgreSQL, never an app datasource."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import time
import uuid
import zipfile

APP = Path(__file__).resolve().parents[2]
JAVA = shutil.which("java")
MIGRATIONS = APP / "target/legal-cases-backend-migrations.jar"
BACKEND = APP / "target/legal-cases-backend.jar"
EXPECTED_SQL_MIGRATIONS = 3


def docker(*arguments, input_text=None):
    result = subprocess.run(["docker", *arguments], input=input_text, text=True,
                            capture_output=True, timeout=60, check=False)
    if result.returncode:
        raise RuntimeError("Synthetic container tool failed: " + result.stderr)
    return result.stdout.strip()


def job(operation, environment, expected=0):
    result = subprocess.run([JAVA, "-jar", str(MIGRATIONS), operation], cwd=APP,
                            env=environment, text=True, capture_output=True, timeout=60, check=False)
    if result.returncode != expected:
        raise RuntimeError("Packaged migration command failed: " + result.stdout + result.stderr)
    if environment["MIGRATION_DB_PASSWORD"] in result.stdout + result.stderr:
        raise AssertionError("Migration credential leaked")
    if environment["MIGRATION_DB_URL"] in result.stdout + result.stderr:
        raise AssertionError("Migration URL leaked")
    prefix = "MIGRATION_OK" if expected == 0 else "MIGRATION_FAILED"
    if not result.stdout.startswith(prefix):
        raise AssertionError("Missing sanitized command result")
    print(result.stdout.strip())


def main():
    for jar, main_class in [(BACKEND, "co.edu.ufps.legal_cases.App"),
                            (MIGRATIONS, "co.edu.ufps.legal_cases.migration.DatabaseMigrationCommand")]:
        with zipfile.ZipFile(jar) as archive:
            manifest = archive.read("META-INF/MANIFEST.MF").decode().replace("\r\n ", "")
            if "Start-Class: " + main_class not in manifest:
                raise AssertionError("Incorrect executable entrypoint")
            archive.getinfo("BOOT-INF/classes/co/edu/ufps/legal_cases/migration/DatabaseMigrationCommand.class")
        print("ARTIFACT_SHA256 " + jar.name + " " + hashlib.sha256(jar.read_bytes()).hexdigest())

    container = None
    try:
        container = docker("run", "--rm", "-d", "--name", "scrum332-smoke-" + uuid.uuid4().hex[:12],
                           "--label", "scrum332.synthetic=true", "-p", "127.0.0.1::5432",
                           "-e", "POSTGRES_USER=migration_owner", "-e", "POSTGRES_PASSWORD=synthetic-job-owner",
                           "-e", "POSTGRES_DB=artifact_smoke", "postgres:16-alpine")
        ready = False
        for _ in range(120):
            status = subprocess.run(["docker", "exec", container, "pg_isready", "-h", "127.0.0.1", "-U", "migration_owner",
                                     "-d", "artifact_smoke"], capture_output=True, timeout=10, check=False)
            if status.returncode == 0:
                ready = True
                break
            time.sleep(0.25)
        if not ready:
            raise RuntimeError("Synthetic PostgreSQL did not become ready")
        port = docker("port", container, "5432/tcp").splitlines()[0].rsplit(":", 1)[1]
        # Intentionally do not inherit DB_*, mail, storage, Java options or other app secrets.
        environment = {"PATH": os.environ.get("PATH", ""), "LANG": "C.UTF-8",
                       "MIGRATION_DB_URL": "jdbc:postgresql://127.0.0.1:" + port + "/artifact_smoke",
                       "MIGRATION_DB_USERNAME": "migration_owner", "MIGRATION_DB_PASSWORD": "synthetic-job-owner",
                       "MIGRATION_ENVIRONMENT": "synthetic-artifact", "MIGRATION_RELEASE": "0" * 40}
        for operation in ("migrate", "migrate", "validate"):
            job(operation, environment)
        wrong = dict(environment, MIGRATION_DB_PASSWORD="invalid-synthetic-password")
        job("validate", wrong, expected=1)
        job("clean", environment, expected=1)
        prefix = ("exec", container, "psql", "-U", "migration_owner", "-d", "artifact_smoke", "-v", "ON_ERROR_STOP=1")
        count = docker(*prefix, "-Atc", 'SELECT count(*) FROM "DB_consultorioJuridico".flyway_schema_history WHERE type=\'SQL\' AND success')
        if count != str(EXPECTED_SQL_MIGRATIONS):
            raise AssertionError(
                "Unexpected number of applied SQL migrations: " + count
            )
        docker(*prefix, "-c", "CREATE ROLE app_runtime LOGIN PASSWORD 'synthetic-runtime-only'")
        # Model a legacy login that once had broad grants; normalize rather than retain TRUNCATE/TRIGGER.
        docker(*prefix, "-c", 'GRANT ALL ON ALL TABLES IN SCHEMA "DB_consultorioJuridico" TO app_runtime;'
               'GRANT ALL ON ALL SEQUENCES IN SCHEMA "DB_consultorioJuridico" TO app_runtime;')
        docker("exec", "-i", container, "psql", "-U", "migration_owner", "-d", "artifact_smoke", "-v",
               "ON_ERROR_STOP=1", "-v", "runtime_role=app_runtime", "-f", "-",
               input_text=(APP / "db/operations/grant_runtime_permissions.sql").read_text())
        privileges = docker(*prefix, "-Atc", "SELECT has_schema_privilege('app_runtime','DB_consultorioJuridico','CREATE'),"
                             "has_table_privilege('app_runtime','\"DB_consultorioJuridico\".flyway_schema_history','UPDATE'),"
                             "has_table_privilege('app_runtime','\"DB_consultorioJuridico\".audit_logs','TRUNCATE')")
        if privileges != "f|f|f":
            raise AssertionError("Runtime retained DDL or writable migration history")
        docker(*prefix, "-c", "CREATE ROLE unsafe_parent SUPERUSER; CREATE ROLE unsafe_runtime; GRANT unsafe_parent TO unsafe_runtime")
        rejected = subprocess.run(["docker", "exec", "-i", container, "psql", "-U", "migration_owner", "-d", "artifact_smoke",
                                   "-v", "ON_ERROR_STOP=1", "-v", "runtime_role=unsafe_runtime", "-f", "-"],
                                  input=(APP / "db/operations/grant_runtime_permissions.sql").read_text(),
                                  text=True, capture_output=True, timeout=60, check=False)
        if rejected.returncode == 0 or "Runtime must not be privileged" not in rejected.stderr:
            raise AssertionError("Unsafe role membership was not rejected")
        print("SCRUM-332 packaged JAR migrate/repeat/validate/credential-rejection/clean-rejection/psql-role-grants PASS")
    finally:
        if container:
            docker("rm", "-f", container)
            print("SCRUM-332 packaged JAR synthetic container cleanup PASS")


if __name__ == "__main__":
    main()
