import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const TRACKER = /^\[(SCRUM-\d+|CC-OPT-\d+)\] (.+)$/;
const CONVENTIONAL = /^(feat|fix|test|docs|refactor|chore|style)(?:\([a-z][a-z0-9-]*\))?: ([a-z][^\r\n]+)$/;
const PROMOTIONS = new Set(['develop:staging', 'staging:main']);
const GENERIC = /^(?:update|changes|fix stuff|final|prueba)$/i;

/** Validate new PR titles without rewriting historic Git commits. */
export function validatePullRequestTitle(title, headRef, baseRef) {
  if (typeof title !== 'string' || !title.trim()) {
    return { valid: false, reason: 'PR title must be non-empty.' };
  }

  const tracked = TRACKER.exec(title);
  const isPromotion = PROMOTIONS.has(headRef + ':' + baseRef);
  if (!tracked && !isPromotion) {
    return {
      valid: false,
      reason: 'A work PR must start with [SCRUM-123] or [CC-OPT-004].',
    };
  }

  const conventionalTitle = tracked ? tracked[2] : title;
  const match = CONVENTIONAL.exec(conventionalTitle);
  if (!match) {
    return {
      valid: false,
      reason: 'Use type(scope): english description or type: english description; type must be feat, fix, test, docs, refactor, chore or style.',
    };
  }

  const description = match[2];
  if (
    description !== description.trim() ||
    description.length < 12 ||
    !description.includes(' ') ||
    GENERIC.test(description)
  ) {
    return {
      valid: false,
      reason: 'Use a specific English description of at least 12 characters and two words, without surrounding spaces.',
    };
  }

  return { valid: true, reason: 'PR title follows the project convention.' };
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const result = validatePullRequestTitle(
    process.env.PR_TITLE,
    process.env.PR_HEAD_REF,
    process.env.PR_BASE_REF,
  );
  const log = result.valid ? console.log : console.error;
  log(result.reason);
  if (!result.valid) process.exitCode = 1;
}
