<!--
Describe only what this PR actually changes. Keep applicable fields and write
"N/A" with a short reason when a section does not apply. Do not invent results,
approvals or deployments. Never include passwords, tokens, real personal data,
private configuration values or sensitive vulnerability details.
-->

## Tracking and purpose

- **Work item / change request:** <!-- SCRUM-123, CC-OPT-001, or other authorized identifier -->
- **Type:** <!-- feature / fix / hotfix / docs / test / refactor / chore / release -->
- **Purpose:** <!-- Problem, requirement, incident, or reason for this change -->
- **Scope and exclusions:** <!-- What is included and intentionally not included -->

## Changes made

<!-- Summarize important changes; mention relevant files/modules where helpful. -->

- 

**Affected areas** (select all that apply):

- [ ] Backend / API / security
- [ ] Frontend / UI
- [ ] Database / migration / stored data
- [ ] CI / configuration / deployment
- [ ] Tests / QA
- [ ] Documentation
- [ ] Other: <!-- specify or N/A -->

## Acceptance and traceability

<!-- Link or identify the applicable acceptance criteria, defect reproduction,
change request, or expected outcome. For documentation-only work, state the
expected document/configuration result. -->

- **Criteria / expected outcome:**
- **Outcome and remaining gap:**

## Verification and evidence

<!-- Only list checks actually executed. Distinguish local checks from CI and
from independent QA. Use PASS, FAIL, NOT RUN, or N/A truthfully. -->

| Check / case | Command or procedure and environment | Result | Evidence / reference |
| --- | --- | --- | --- |
| <!-- e.g. backend unit tests --> | <!-- exact command or manual steps --> | <!-- PASS / FAIL / NOT RUN / N/A --> | <!-- link, report, commit, or reason --> |

- **Checks not run and reason:**
- **Relevant screenshots or test artifacts:** <!-- For UI or other visible changes, if applicable -->

## Impact and compatibility

<!-- Use N/A when not applicable. Do not include secret values. -->

- **API, UI, permissions or behavior:**
- **Database / migration and data compatibility:**
- **Environment / configuration changes:** <!-- Variable names only; no values -->
- **Documentation updated or follow-up item:**

## Deployment, recovery and release (when applicable)

<!-- For ordinary code PRs with no deployment in scope, use N/A. For promotions,
releases or hotfixes, record the verified state, not an anticipated success. -->

- **Destination and deployment steps:**
- **Staging / integration validation:**
- **Rollback or recovery approach:**
- **Required change / release authorization:** <!-- Recorded approval or N/A; never assume -->
- **Release version, baseline and deployed commit:** <!-- If verified; otherwise pending / N/A -->

## Risks and pending work

- **Known risks, limitations or unresolved findings:**
- **Follow-up issue(s), if any:**

## Review and merge checklist

<!-- Do not check items that have not happened yet. GitHub branch protections
and recorded reviews, not checkbox marks, decide whether a PR can be merged. -->

- [ ] Scope matches the linked work item; unrelated changes are excluded.
- [ ] Diff contains no secrets, personal data, unnecessary files or credentials.
- [ ] Applicable tests/checks and any omissions are documented accurately.
- [ ] Required docs, migrations and compatibility/recovery considerations are addressed or linked.
- [ ] Required CI checks and independent review are completed before merge.
- [ ] Applicable release/production authorization is documented before promotion.
- [ ] Conflicts and required review conversations are resolved before merge.
