# Phase 2 Checklist — `textus-cbd-support` End-to-End Consumer Integration

## Consumer boundary

- [ ] Define CBD Finding → `ChangeProposal` mapping.
- [ ] Keep CML/CDM/Design Diff semantics in `textus-cbd-support`.
- [ ] Keep generic proposal lifecycle in `textus-change-management`.
- [ ] Reuse Phase 1 Delivery Provider SPI unchanged where possible.

## Representative change

- [ ] Select one narrow CBD Finding for the first end-to-end path.
- [ ] Generate a CML patch without mutating canonical `main`.
- [ ] Build candidate CML/source artifact.
- [ ] Build Candidate CDM from candidate source.
- [ ] Compute Design Diff between current and candidate CDM.
- [ ] Run Candidate Design Review.
- [ ] Confirm the target Finding is resolved and no unacceptable new Finding is introduced.

## ChangeProposal evidence

- [ ] Attach source target.
- [ ] Record Design Guidance.
- [ ] Attach proposed CML patch.
- [ ] Attach candidate artifact.
- [ ] Attach Design Diff as semantic diff evidence.
- [ ] Attach Candidate Review separately.
- [ ] Mark proposal ReadyForDelivery only after accepted review.

## GitHub delivery

- [ ] Invoke the Phase 1 GitHub adapter.
- [ ] Create/reuse a non-canonical change branch.
- [ ] Create Pull Request.
- [ ] Include Finding, Guidance, source summary, Design Diff, Candidate Review, and evidence links in PR context.
- [ ] Persist provider-neutral `ChangeDelivery`.
- [ ] Transition proposal to `Delivered`.
- [ ] Do not auto-merge.

## Tests

- [ ] Unit/integration test CBD → generic proposal mapping.
- [ ] Test candidate CDM is derived from candidate CML/source.
- [ ] Test Design Diff remains distinct from source diff.
- [ ] Test rejected candidate review prevents delivery readiness.
- [ ] Test generic Change Management model contains no CML/CDM-specific types.
- [ ] Test GitHub adapter needs no CBD-specific branch in its core implementation.
- [ ] Add explicit opt-in real GitHub delivery test/demo.

## Visualization / traceability

- [ ] Preserve Finding → ChangeProposal traceability.
- [ ] Preserve ChangeProposal → Design Diff traceability.
- [ ] Preserve ChangeProposal → Candidate Review traceability.
- [ ] Preserve ChangeProposal → Pull Request traceability.
- [ ] If practical, expose these links from the CBD Review/Dashboard surface.

## Feedback and validation

- [ ] Record abstraction friction in journal.
- [ ] Review and update CNCF extraction ledger.
- [ ] Validate `textus-change-management`.
- [ ] Validate `textus-cbd-support`.
- [ ] Run standard tests/CAR builds for both affected projects.
- [ ] Record exit evidence and deferred work.
