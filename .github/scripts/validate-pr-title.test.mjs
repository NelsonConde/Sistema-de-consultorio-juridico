import test from 'node:test';
import assert from 'node:assert/strict';
import { validatePullRequestTitle } from './validate-pr-title.mjs';

const REPO = 'NelsonConde/Sistema-de-consultorio-juridico';
const check = (title, head = 'chore/scrum-346-sample', base = 'develop', headRepo = REPO, baseRepo = REPO) =>
  validatePullRequestTitle(title, head, base, headRepo, baseRepo).valid;

test('allows a tracked documentation PR', () => {
  assert.equal(check('[SCRUM-320] docs: reconcile CP-09 closure traceability with RF05'), true);
});
test('allows a scoped title for a tracked PR', () => {
  assert.equal(check('[SCRUM-346] chore(ci): validate future PR titles'), true);
});
test('allows an approved change request identifier', () => {
  assert.equal(check('[CC-OPT-004] feat: generate expediente PDF safely'), true);
});
test('allows style as defined in the branch manual', () => {
  assert.equal(check('[SCRUM-346] style(frontend): align dashboard headings'), true);
});
test('does not impose a minimum length absent from the manual', () => {
  assert.equal(check('[SCRUM-346] fix(auth): refresh'), true);
});
test('permits develop to staging promotion from the same repository', () => {
  assert.equal(check('chore(release): promote candidate to staging', 'develop', 'staging'), true);
});
test('permits staging to main promotion from the same repository', () => {
  assert.equal(check('chore(release): promote validated build to main', 'staging', 'main'), true);
});
test('rejects spoofed promotion branch from a fork', () => {
  assert.equal(check('chore(release): promote candidate to staging', 'develop', 'staging', 'attacker/fork'), false);
});
test('rejects spoofed staging-to-main promotion from a fork', () => {
  assert.equal(check('chore(release): promote validated build to main', 'staging', 'main', 'attacker/fork'), false);
});
test('rejects a non-promotion branch without identifier', () => {
  assert.equal(check('chore(release): promote candidate to staging', 'feature/release', 'staging'), false);
});
test('rejects an ordinary PR without work identifier', () => {
  assert.equal(check('docs(governance): update contribution guide'), false);
});
test('rejects an unknown type', () => {
  assert.equal(check('[SCRUM-346] misc: add commit validation'), false);
});
test('rejects generic descriptions from the manual', () => {
  assert.equal(check('[SCRUM-346] chore: update'), false);
  assert.equal(check('[SCRUM-346] docs: final'), false);
});
test('rejects capitalized type', () => {
  assert.equal(check('[SCRUM-346] Docs: update contribution rules'), false);
});
test('rejects empty titles and descriptions', () => {
  assert.equal(check(''), false);
  assert.equal(check('[SCRUM-346] docs: '), false);
});
test('rejects multiline title and surrounding whitespace', () => {
  assert.equal(check('[SCRUM-346] docs: update contribution rules\nunsafe'), false);
  assert.equal(check('[SCRUM-346] docs:  update contribution rules'), false);
});
test('rejects malformed scope', () => {
  assert.equal(check('[SCRUM-346] fix(FE): update user menu'), false);
});
