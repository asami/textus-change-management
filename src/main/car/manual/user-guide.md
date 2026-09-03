# Textus Change Management user guide

## Prerequisites

Use a compatible CNCF `0.5.3-SNAPSHOT` runtime and install the
`textus-change-management-0.1.0-SNAPSHOT.car` produced by `cozyBuildCAR`.
Configure the host entity store before relying on durable proposal records.

## First successful proposal

1. Call `createProposal` with a unique `proposalId`, subject, finding summary,
   and a `Source` artifact reference.
2. Call `recordGuidance` with the same `proposalId` and actionable guidance.
3. Call `attachProposedPatch` with a `Patch` artifact.
4. Call `attachCandidateArtifact` with a `Candidate` artifact.
5. Call `attachSemanticDiff` with a nonblank summary and optional evidence
   reference.
6. Call `recordCandidateReview` with decision `Accepted`, a nonblank summary,
   and optional evidence reference.
7. Call `markReadyForDelivery`.
8. Confirm that the returned proposal has state `ReadyForDelivery`, readiness
   `Ready`, and retains all four evidence objects separately.

Use `getProposal` to retrieve one proposal or `listProposals` to inspect the
work queue. Consumers may render or route the returned records without knowing
the underlying artifact media formats.

## Normal workflows

For design-model changes, an adapter may reference CML/CDM sources and generated
models. For BoK changes, another adapter may reference Markdown/YAML sources and
knowledge artifacts. Both follow the same service operations because the core
stores typed roles and opaque references, not format-specific payloads.

After `ReadyForDelivery`, hand the proposal to a provider adapter. This version
does not create a pull request or merge anything by itself.

## Troubleshooting

- `expected <state>`: a command was called out of order; load the proposal and
  resume with the operation appropriate for its current state.
- `must use artifact kind`: supply `Source`, `Patch`, or `Candidate` according
  to the requested operation.
- missing `semanticDiff` or `reviewResult`: attach and accept the semantic
  evidence before requesting readiness.
- duplicate proposal: choose a new public `proposalId` or continue the existing
  proposal.
- runtime persistence errors: verify the CNCF entity-store configuration and
  permissions; they are outside this CAR's provider-neutral model.

The repository README summarizes the component boundary. Runtime-generated
component help is available through CNCF CLI help and the canonical
`/help/<component>` and `/man/<component>` routes.
