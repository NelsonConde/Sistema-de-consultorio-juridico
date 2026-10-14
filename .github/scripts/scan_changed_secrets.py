"""Fail a pull request on potential secrets without printing findings.

Only files changed by the PR are inspected. The protected backend application
configuration file is explicitly excluded. This is a prospective control, not
a historical repository audit or an attestation about production secrets.
"""

import json
import os
from pathlib import Path
import re
import subprocess
import sys


PROTECTED_PATH = "backend/app/src/main/resources/application.properties"


def main() -> int:
    base_ref = os.environ.get("BASE_REF", "")
    if not re.fullmatch(r"[A-Za-z0-9._/-]+", base_ref):
        print("Unable to determine a valid pull-request base branch.")
        return 2

    try:
        changed = subprocess.run(
            ["git", "diff", "--name-only", "--diff-filter=ACMR", "-z",
             f"origin/{base_ref}...HEAD"],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            timeout=30,
        ).stdout
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired):
        print("Could not resolve the pull-request change set.")
        return 2

    files = []
    for raw_name in changed.split(b"\x00"):
        if not raw_name:
            continue
        name = os.fsdecode(raw_name).replace("\\", "/")
        if name == PROTECTED_PATH:
            continue
        path = Path(name)
        if path.is_file() and not path.is_symlink():
            files.append(name)

    if not files:
        print("No eligible changed files to scan.")
        return 0

    try:
        process = subprocess.run(
            ["detect-secrets", "scan", "--all-files", *files],
            check=False,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            text=True,
            timeout=120,
        )
        if process.returncode != 0:
            print("Secret scanner could not complete the inspection.")
            return 2
        report = json.loads(process.stdout)
        findings = sum(len(entries) for entries in report.get("results", {}).values())
    except (subprocess.TimeoutExpired, ValueError, TypeError, OSError):
        print("Secret scanner did not return a usable result.")
        return 2

    print(f"Eligible changed files examined: {len(files)}.")
    print(f"Potential secret findings: {findings}.")
    if findings:
        print("Review potential findings privately; no secret values or details are published.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
