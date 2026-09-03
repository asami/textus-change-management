# Phase 1 Checklist — Delivery Provider SPI and GitHub Pull Request Adapter

## Contract

- [ ] Confirm `ChangeDelivery` remains provider-neutral.
- [ ] Define the minimal Delivery Provider SPI.
- [ ] Define stable delivery correlation/idempotency semantics.
- [ ] Define failure semantics for external creation vs local persistence.
- [ ] Keep provider credentials out of domain state.

## Lifecycle

- [ ] Implement `ReadyForDelivery -> Delivered`.
- [ ] Reject delivery from any non-ready state.
- [ ] Persist successful delivery evidence atomically enough for retry safety.
- [ ] Preserve patch, candidate, semantic diff, and candidate review evidence.

## GitHub adapter

- [ ] Resolve target repository and base branch from adapter configuration/request context.
- [ ] Create/use a non-canonical change branch.
- [ ] Materialize the proposed source change.
- [ ] Create a Pull Request.
- [ ] Return provider-neutral `ChangeDelivery`.
- [ ] Reuse/correlate an existing PR on retry where possible.
- [ ] Do not auto-merge.

## Tests

- [ ] Provider-neutral test double proves the SPI independently of GitHub.
- [ ] Invalid lifecycle state is rejected.
- [ ] Provider failure leaves proposal undelivered.
- [ ] Successful delivery transitions to `Delivered`.
- [ ] Retry does not unintentionally duplicate delivery.
- [ ] Core CML/Scala contracts contain no GitHub-specific types.
- [ ] GitHub integration test is isolated/opt-in.

## Documentation and validation

- [ ] Update README/manual for delivery behavior.
- [ ] Record implementation feedback in journal.
- [ ] Update CNCF extraction ledger.
- [ ] Run `sbt --batch test cozyBuildCAR` through the repository's serialized SBT workflow.
- [ ] Run current CAR lint/validation.
- [ ] Record validation results and deferred issues.
