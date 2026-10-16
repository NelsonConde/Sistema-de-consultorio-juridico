import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const TRACKER = /^\[(SCRUM-\d+|CC-OPT-\d+)\] (.+)$/;
const CONVENTIONAL = /^(feat|fix|test|docs|refactor|chore|style)(?:\([a-z][a-z0-9-]*\))?: (.+)$/;
const PROMOTIONS = new Set(['develop:staging', 'staging:main']);
const GENERIC = new Set(['update', 'changes', 'fix stuff', 'final', 'prueba']);

/** Check the title using the project manual; do not assess historical commits. */
export function validatePullRequestTitle(title, headRef, baseRef, headRepository, baseRepository) {
  if (typeof title !== 'string' || !title.trim()) {
    return { valid: false, reason: 'PR title must be non-empty.' };
  }

  const tracked = TRACKER.exec(title);
  const sameRepository = Boolean(headRepository && baseRepository && headRepository === baseRepository);
  const isPromotion = sameRepository && PROMOTIONS.has(headRef + ':' + baseRef);
  if (!tracked && !isPromotion) {
    return { valid: false, reason: 'Work PR titles must start with [SCRUM-123] or [CC-OPT-004].' };
  }

  const match = CONVENTIONAL.exec(tracked ? tracked[2] : title);
  if (!match) {
    return {
      valid: false,
      reason: 'Use type(scope): description or type: description with an approved type: feat, fix, test, docs, refactor, chore or style.',
    };
  }

  // The final capture is the description; scope is optional for PR titles only.
  const detail = match[2];
  if (!detail || detail !== detail.trim() || GENERIC.has(detail.toLowerCase())) {
    return { valid: false, reason: 'Give a specific, non-generic description without surrounding spaces.' };
  }

  return { valid: true, reason: 'PR title follows the project convention.' };
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const result = validatePullRequestTitle(
    process.env.PR_TITLE,
    process.env.PR_HEAD_REF,
    process.env.PR_BASE_REF,
    process.env.PR_HEAD_REPOSITORY,
    process.env.PR_BASE_REPOSITORY,
  );
  (result.valid ? console.log : console.error)(result.reason);
  if (!result.valid) process.exitCode = 1;
}
