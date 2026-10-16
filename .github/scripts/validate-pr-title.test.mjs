import test from 'node:test';
import assert from 'node:assert/strict';
import { validatePullRequestTitle } from './validate-pr-title.mjs';

const check = (title, head = 'feature/scrum-346-sample', base = 'develop') =>
  validatePullRequestTitle(title, head, base).valid;

test('accepts a tracked fix PR', () => {
  assert.equal(check('[SCRUM-320] docs: reconcile CP-09 closure traceability with RF05'), true);
});

test('accepts a tracked scoped PR title', () => {
  assert.equal(check('[SCRUM-346] chore(ci): validate future PR titles'), true);
});

test('accepts a tracked change request', () => {
  assert.equal(check('[CC-OPT-004] feat: generate expediente PDF safely'), true);
});

test('accepts develop to staging promotion without an individual Jira ID', () => {
  assert.equal(check('chore(release): promote candidate to staging', 'develop', 'staging'), true);
});

test('accepts staging to main promotion without an individual Jira ID', () => {
  assert.equal(check('chore(release): promote validated build to main', 'staging', 'main'), true);
});

test('rejects missing Jira ID for ordinary work', () => {
  assert.equal(check('docs(governance): update repository guidelines'), false);
});

test('rejects invalid type', () => {
  assert.equal(check('[SCRUM-346] misc: add commit validation logic'), false);
});

test('rejects generic or too-short description', () => {
  assert.equal(check('[SCRUM-346] chore: update'), false);
});

test('rejects invalid case in type', () => {
  assert.equal(check('[SCRUM-346] Docs: validate future PR titles'), false);
});

test('rejects untracked titles for temporary branches even if titled as a release', () => {
  assert.equal(check('chore(release): promote candidate to staging', 'feature/new-release', 'staging'), false);
});

test('rejects blank or multiline titles', () => {
  assert.equal(check(''), false);
  assert.equal(check('[SCRUM-346] docs: validate future PR titles\nunsafe'), false);
});
