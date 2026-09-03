# Phase 1 — Delivery Provider SPI and GitHub Pull Request Adapter

**Status:** Planned  
**Date:** 2026-09-03

## 1. Goal

Extend the current executable baseline from `ReadyForDelivery` to a provider-backed `Delivered` state.

The phase introduces:

- a provider-neutral Delivery Provider SPI;
- GitHub Pull Request as the first concrete provider;
- persisted `ChangeDelivery` evidence;
- retry/idempotency rules for external submission;
- tests that prove GitHub-specific semantics remain outside the generic core.

Target path:

```text
Reviewed
→ ReadyForDelivery
→ Delivery Provider
→ GitHub branch / proposed source change
→ Pull Request
→ ChangeDelivery
→ Delivered
```

## 2. Starting point

The current CAR already models:

```text
Detected
→ Guided
→ Proposed
→ CandidateBuilt
→ Reviewed
→ ReadyForDelivery
→ Delivered
→ Merged
→ Verified
```

The current executable slice stops at `ReadyForDelivery`. `ChangeDelivery` already carries provider-neutral fields such as provider, external ID, URI, and status.

Phase 1 should extend behavior without replacing these domain concepts with GitHub-specific types.

## 3. Core design

### 3.1 Delivery Provider SPI

Introduce the smallest coherent provider boundary that can consume a delivery-ready proposal and return delivery evidence.

Conceptually:

```text
DeliveryRequest
  proposal identity
  source/candidate references
  proposed patch reference
  semantic diff reference/summary
  candidate review reference/summary
  provider configuration reference

DeliveryProvider
  deliver(request)
    -> ChangeDelivery
```

The final Scala/CML shape should follow existing CNCF/Cozy conventions rather than forcing this exact interface.

The provider boundary must not require the core to understand GitHub repository, branch, PR, reviewer, or merge semantics.

### 3.2 Delivery state transition

Add executable handling for:

```text
ReadyForDelivery
→ Delivered
```

The transition must require successful persisted delivery evidence.

A failed external submission must not leave the proposal in `Delivered`.

### 3.3 Retry and idempotency

External delivery is side-effecting and may be retried after timeout or process failure.

Define a stable correlation key sufficient to avoid accidental duplicate Pull Requests.

Possible inputs include:

- proposal ID;
- provider ID;
- target repository;
- base branch;
- candidate/source revision;
- existing external delivery ID where available.

The implementation must distinguish:

- retry of the same delivery;
- explicit new delivery after a prior one was closed/rejected;
- provider failure before external creation;
- provider success where local persistence failed afterward.

Document any CNCF runtime limitation encountered here as a candidate primitive rather than hiding it in provider-specific code.

## 4. GitHub Pull Request adapter

Implement GitHub as the first provider.

Responsibilities include:

- identify target repository and base branch;
- materialize the proposed source change on a non-canonical branch;
- push/create the branch as required;
- create a Pull Request;
- return provider-neutral `ChangeDelivery` evidence containing at least provider, external ID, URI, and status;
- reuse an existing PR for an idempotent retry where possible.

The adapter must not automatically merge the Pull Request in this phase.

Human approval remains outside the component.

## 5. Delivery payload and PR description

The GitHub adapter should support a PR body assembled from generic proposal evidence plus optional consumer-supplied semantic context.

Minimum useful context:

```text
Change Proposal
- proposal ID
- subject
- originating finding
- guidance summary

Candidate Review
- accepted decision
- review summary

Semantic Diff
- semantic summary

Source
- source/candidate artifact references
```

Do not make CBD-specific Design Diff fields part of the generic provider SPI. Phase 2 may supply richer provider metadata from `textus-cbd-support`.

## 6. Configuration and security

Provider credentials and authorization must remain external configuration/runtime concerns.

Do not persist tokens or secrets in `ChangeProposal` or `ChangeDelivery`.

The adapter should make repository/branch policy explicit enough to support authorization checks and safe tests.

## 7. Operations

Add only the operations required for the new executable slice.

Expected capability:

```text
deliverChange
```

Potential read support:

```text
getDelivery
```

If current generated entity/query operations already expose sufficient delivery state, avoid redundant operations.

Do not add merge or verification commands merely to complete the modeled lifecycle; those are later concerns unless needed for a minimal contract test.

## 8. Testing

Tests should cover at least:

- delivery is rejected unless proposal is `ReadyForDelivery`;
- accepted candidate evidence remains required;
- successful provider result persists `ChangeDelivery` and transitions to `Delivered`;
- provider failure does not transition to `Delivered`;
- duplicate/retry behavior does not create duplicate external deliveries in the representative test path;
- GitHub-specific types do not leak into core value/entity contracts;
- provider-neutral test double can exercise the same SPI without GitHub.

For real GitHub integration, prefer an explicit opt-in integration test or dedicated test repository rather than making ordinary unit tests mutate production repositories.

## 9. Documentation

Update:

- README with the new executable range;
- user/manual documentation for delivery;
- journal with implementation feedback;
- CNCF extraction ledger with any newly observed runtime gaps.

Document the final provider SPI and GitHub adapter boundary.

## 10. Non-goals

- automatic PR merge;
- replacing human review;
- CBD-specific CML generation;
- BoK-specific source generation;
- generic Git hosting abstraction beyond what the first provider needs;
- premature CNCF framework extraction;
- post-merge re-verification orchestration.

## 11. Exit criteria

Phase 1 is complete when:

1. the generic Delivery Provider SPI is implemented and tested;
2. a delivery-ready proposal can be delivered through a provider-neutral test implementation;
3. the GitHub adapter can create/reuse a Pull Request on a controlled repository;
4. `ChangeDelivery` is persisted and the proposal reaches `Delivered`;
5. external failure/retry behavior is documented and tested;
6. no GitHub-specific vocabulary is introduced into the core domain model;
7. `sbt --batch test cozyBuildCAR` and current CAR validation succeed;
8. newly discovered CNCF primitive candidates are recorded but not prematurely extracted.
