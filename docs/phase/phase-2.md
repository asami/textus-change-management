# Phase 2 — `textus-cbd-support` End-to-End Consumer Integration

**Status:** Planned  
**Date:** 2026-09-03

## 1. Goal

Use `textus-cbd-support` as the first real consumer of `textus-change-management` and prove the complete CBD improvement path:

```text
CBD Finding
→ CML Patch
→ Candidate CDM
→ Design Diff
→ Candidate Review
→ ChangeProposal
→ GitHub Pull Request
```

This phase validates the boundary between domain-specific design semantics and the generic change-management lifecycle.

## 2. Architectural responsibility split

### `textus-cbd-support`

Owns:

- CBD Review Finding semantics;
- Design Guidance;
- CML as canonical source;
- CML patch generation;
- Candidate CML validation/build;
- Candidate Component Design Model (CDM) construction;
- Design Diff;
- Candidate Design Review;
- mapping domain evidence into generic change artifacts/review values;
- presentation of design context for the Pull Request.

### `textus-change-management`

Owns:

- proposal identity and lifecycle;
- generic source/patch/candidate artifact references;
- semantic diff evidence retention;
- candidate review evidence retention;
- delivery readiness;
- Delivery Provider invocation;
- persisted `ChangeDelivery` state.

### GitHub adapter

Owns:

- branch/change materialization;
- Pull Request creation;
- provider-specific external identifiers and URI;
- retry/correlation behavior.

The generic core must not parse CML or interpret CDM/Design Diff content.

## 3. Canonical-source invariant

For CBD, the canonical design source is CML.

The normal path must therefore be:

```text
Current CML
    |
    v
Proposed CML Patch
    |
    v
Candidate CML
    |
    v
Candidate CDM
```

The Candidate CDM is a projection used for understanding and review. It is not a replacement canonical source.

The Pull Request ultimately proposes CML/source changes.

## 4. Finding to proposal

Define how a CBD Review Finding becomes or references a generic `ChangeProposal`.

The mapping should preserve at least:

- stable finding/reference identity where available;
- finding summary;
- affected design subject;
- source CML target;
- design guidance or rationale.

Avoid copying the entire CBD review model into Change Management. Keep a source/evidence reference back to CBD Support for richer detail.

## 5. CML patch generation

The first representative case should be narrow and deterministic enough to test end to end.

Suitable examples include one of:

- change an Entity relationship from composition to aggregation;
- introduce/fix an Entity classification relation;
- add a missing State Machine transition;
- correct an Aggregate membership/boundary declaration.

The phase should choose one representative scenario and carry it through the full path before generalizing patch production.

Requirements:

- generated/proposed patch is a distinct `ChangeArtifact`;
- source target and proposed patch remain independently identifiable;
- patch application creates a candidate source tree/artifact without mutating canonical `main`.

## 6. Candidate Component Design Model

Build or acquire a Candidate CDM from the proposed CML change.

The Candidate CDM should support the model areas defined by CBD Design Support, including as applicable:

- Use Cases;
- Entity classification: generalization, trait, powertype;
- composition/aggregation;
- Aggregates;
- Views;
- State Machines;
- Workflows;
- traceability to operations/events/artifacts.

The first slice only needs the subset required by the chosen representative finding, but the contract should not block later expansion.

## 7. Design Diff

Compute a semantic/design-level comparison between the current and candidate CDM.

The Design Diff is distinct from the CML/text diff.

Example:

```text
Design Diff

Entity relationships
  Order -> Customer
    composition -> aggregation

Aggregate impact
  Customer removed from Order ownership boundary

Workflow impact
  no change

Review impact
  composition-boundary warning: resolved
```

The generic Change Management record should receive a summary plus optional payload/evidence reference, not a CBD-specific object graph embedded in its core contract.

## 8. Candidate Review

Run CBD review against the candidate before delivery readiness.

The review should answer:

- did the target Finding resolve?;
- were new Findings introduced?;
- is the candidate acceptable for human PR review?;
- what quality/design impact changed?;
- is the semantic change consistent with the source patch?

Only an accepted Candidate Review should allow `markReadyForDelivery`.

## 9. ChangeProposal mapping

The complete generic evidence chain should be populated:

```text
ChangeProposal
├─ originatingFinding  -> CBD Finding summary/reference
├─ guidance            -> Design Guidance summary
├─ sourceTarget        -> current CML/source reference
├─ proposedPatch       -> CML patch reference
├─ candidateArtifact   -> candidate CML/build reference
├─ semanticDiff        -> Design Diff summary/reference
├─ reviewResult        -> Candidate Design Review summary/reference
└─ readiness           -> Ready
```

Do not collapse the Design Diff into the source patch or the Candidate Review into the Design Diff.

## 10. GitHub Pull Request delivery

After the proposal reaches `ReadyForDelivery`, use the Phase 1 GitHub adapter.

The resulting PR should contain the source change and enough semantic context for a human reviewer to understand why the CML changed.

Recommended PR summary sections:

```text
Finding
Design Guidance
Source/CML Change Summary
Design Diff
Candidate Review
Traceability / Evidence
```

The PR remains a human approval boundary.

No automatic merge is required.

## 11. End-to-end representative test

Create one controlled end-to-end test/demo using a representative CBD finding.

Expected observable sequence:

```text
1. Finding selected
2. ChangeProposal created
3. Design Guidance recorded
4. CML patch created and attached
5. Candidate CML/CDM built
6. Design Diff attached
7. Candidate Review accepted
8. proposal marked ReadyForDelivery
9. GitHub Pull Request created
10. proposal marked Delivered with PR reference
```

Where a real GitHub mutation is undesirable for ordinary tests, split into:

- deterministic component/integration tests through `ReadyForDelivery`;
- explicit opt-in GitHub delivery test for steps 9–10.

## 12. Review/Visualization integration

Where practical, expose the proposal from the CBD Web Dashboard so a Finding can show:

```text
Finding
→ Proposed Change
→ Candidate Design Diff
→ Candidate Review
→ Pull Request
```

This is not required to become a full editing UI in Phase 2, but source/design/change traceability should be preserved for later visualization.

## 13. Feedback into `textus-change-management`

Phase 2 is specifically intended to challenge the generic abstraction.

Record any friction where CBD Support needs to:

- encode domain semantics into generic strings unnaturally;
- duplicate orchestration that clearly belongs in Change Management;
- bypass lifecycle state;
- work around missing artifact/correlation concepts;
- depend on GitHub-specific behavior through the generic core.

Use these observations to refine the Change Management contract.

## 14. CNCF extraction evidence

After the real consumer works, review the extraction ledger.

Candidates may include:

- external-action correlation and retry;
- durable artifacts/results;
- workflow approval/readiness gates;
- state-machine execution support;
- post-merge/re-verification scheduling.

Do not extract to CNCF unless the Phase 1 provider work and Phase 2 CBD consumer both demonstrate a framework-level need.

## 15. Non-goals

- full automatic redesign of arbitrary CML;
- automatic merge;
- replacing human design review;
- BoK consumer integration;
- complete implementation of every CBD Design Support visualization;
- moving the full change lifecycle into CNCF.

## 16. Exit criteria

Phase 2 is complete when:

1. one real CBD Finding is carried end to end to a GitHub Pull Request;
2. CML remains the canonical source;
3. Candidate CDM is built from proposed source rather than manually fabricated;
4. Design Diff is distinct from source diff;
5. Candidate Review is run before delivery readiness;
6. the generic `ChangeProposal` core contains no CBD/CML-specific vocabulary;
7. the GitHub adapter is reused without CBD-specific branching in its core SPI;
8. automated tests cover the domain-to-generic mapping and candidate review path;
9. validation/build succeeds for affected CARs;
10. architectural feedback and CNCF extraction candidates are documented.
