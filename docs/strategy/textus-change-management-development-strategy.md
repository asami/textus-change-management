# Textus Change Management Development Strategy

**Status:** Active design strategy  
**Date:** 2026-09-03

## 1. Purpose

`textus-change-management` provides the reusable lifecycle for model-driven change proposals:

```text
Finding
→ Guidance
→ Proposed Patch
→ Candidate Model/Artifact
→ Semantic Diff
→ Candidate Review
→ Delivery
→ Human Review
→ Merge
→ Re-verification
```

The component is intentionally provider-neutral and domain-neutral. It owns the governed change lifecycle and evidence chain, while consumer components own domain semantics and delivery adapters own external provider details.

The current executable baseline reaches `ReadyForDelivery`. The next development stages are therefore:

1. establish the generic Delivery Provider SPI and a GitHub Pull Request adapter;
2. connect `textus-cbd-support` as the first real consumer and run the full CBD improvement loop end to end.

## 2. Architectural boundary

```text
textus-cbd-support --------+
                           |
textus-bok ----------------+--> textus-change-management --> CNCF
                           |
other model consumers -----+
```

### `textus-change-management`

Owns:

- `ChangeProposal` lifecycle;
- proposal state and evidence retention;
- delivery readiness;
- provider-neutral delivery contract;
- merge/delivery status representation;
- re-verification lifecycle;
- orchestration boundaries between producer/reviewer/delivery/verifier roles.

Does not own:

- CML parsing or generation;
- Component Design Model semantics;
- BoK Information Model semantics;
- semantic interpretation of a domain diff;
- GitHub-specific concepts in the core model;
- autonomous human approval or merge policy.

### Consumer components

Consumers own the domain-specific parts of the loop.

For `textus-cbd-support`:

```text
CBD Finding
→ Design Guidance
→ CML Patch
→ Candidate CDM
→ Design Diff
→ Candidate Review
```

For `textus-bok`:

```text
Knowledge Finding
→ Knowledge Guidance
→ BoK source patch
→ Candidate Information Model
→ Knowledge Diff
→ Candidate Review
```

### Delivery provider

A Delivery Provider converts a delivery-ready proposal into an external review/change surface.

The first provider is GitHub Pull Request, but the SPI must remain usable for GitLab, a local patch queue, document publication workflow, or another external change system.

## 3. Development sequence

### Phase 1 — Delivery Provider SPI + GitHub Pull Request Adapter

Goal:

Extend the current `ReadyForDelivery` baseline through provider-specific delivery while keeping GitHub-specific concepts outside the core domain model.

Target flow:

```text
ReadyForDelivery
→ Delivery Provider
→ Git branch/change materialization
→ Pull Request creation
→ ChangeDelivery
→ Delivered
```

Phase 1 should prove:

- a provider-neutral delivery SPI;
- GitHub as the first adapter;
- persisted delivery evidence;
- idempotent/retry-safe delivery behavior;
- separation of delivery creation from merge/human review;
- transition from `ReadyForDelivery` to `Delivered`.

Human review and merge remain external actions. Merge observation may be introduced only to the degree needed to establish the later `Merged` transition contract.

See `docs/phase/phase-1.md`.

### Phase 2 — `textus-cbd-support` End-to-End Consumer

Goal:

Use `textus-cbd-support` as the first real consumer and run one complete design-improvement path:

```text
CBD Finding
→ CML Patch
→ Candidate CDM
→ Design Diff
→ Candidate Review
→ ChangeProposal
→ GitHub Pull Request
```

This phase is deliberately cross-component. It is where the current abstractions are tested against real domain semantics.

Phase 2 should prove:

- CBD findings can initialize or reference a `ChangeProposal`;
- CML remains the canonical source;
- candidate CML is built and projected into a Candidate Component Design Model;
- Design Diff is retained separately from source diff;
- Candidate Review occurs before delivery readiness;
- `textus-change-management` receives only provider-neutral artifacts/evidence;
- the GitHub adapter creates the PR after the proposal is ready;
- the resulting PR preserves source diff and semantic/design context for human review.

See `docs/phase/phase-2.md`.

## 4. Candidate-before-delivery invariant

The most important invariant is:

> A change must be semantically evaluated before it is submitted to an external delivery/review provider.

Therefore ordinary source diff is not sufficient.

```text
Source Patch
    |
    v
Candidate Build
    |
    v
Candidate Semantic Model
    |
    v
Semantic Diff
    |
    v
Candidate Review
    |
    v
ReadyForDelivery
```

The delivery provider consumes a ready proposal; it must not bypass candidate review.

## 5. Pull Request policy

The normal Git-based policy is:

> AI proposes; humans accept canonical changes through Pull Request review.

The initial GitHub adapter must therefore:

- avoid direct writes to canonical `main` as its normal delivery path;
- create/use a change branch;
- submit a Pull Request against the configured base branch;
- retain the PR URI and external ID in `ChangeDelivery`;
- avoid automatic merge in the baseline;
- support retry without unintentionally creating duplicate PRs.

The core model should continue to say `Delivery`, not `PullRequest`.

## 6. Semantic context in delivery

A Pull Request should carry more than source diff when semantic evidence is available.

For CBD, the eventual PR context should be able to summarize:

```text
Source Diff
  CML changes

Design Diff
  Entity classification changes
  composition/aggregation changes
  Aggregate/View changes
  state-machine changes
  Workflow changes
  Use Case traceability changes

Candidate Review
  findings resolved
  findings introduced
  quality impact
```

The exact rendering belongs to the consumer/provider integration layer, not the generic `ChangeProposal` core.

## 7. CNCF extraction strategy

Do not preemptively move the change workflow into CNCF.

Use Phases 1 and 2 to gather evidence about mechanisms that are truly generic. Potential candidates include:

- resumable external-action workflow;
- durable external-action correlation;
- idempotent provider execution;
- approval/readiness gates;
- generic artifact/result references;
- merge/event correlation;
- re-verification scheduling;
- state-machine transition execution support.

For each candidate, record:

- concrete use in `textus-change-management`;
- use by at least two domain consumers where possible;
- duplication or friction in local implementation;
- why the primitive belongs in the runtime rather than a CAR.

Only then propose CNCF extraction.

## 8. Success criteria for the strategy

The strategy succeeds when:

1. `textus-change-management` can deliver a reviewed candidate through a provider-neutral SPI;
2. GitHub Pull Request works as the first provider implementation;
3. `textus-cbd-support` completes the real CBD loop end to end;
4. CML remains the canonical CBD source;
5. source diff, semantic/design diff, and candidate review remain distinct evidence;
6. domain semantics stay outside the generic change-management core;
7. CNCF extraction decisions are based on implementation evidence rather than speculation.
