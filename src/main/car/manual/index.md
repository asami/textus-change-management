# Textus Change Management reference manual

## Purpose and scope

The `org.simplemodeling.textus.ChangeManagement` component retains the evidence
and legal state transitions needed to turn a finding into a reviewed,
delivery-ready change. It is independent of artifact formats and delivery
providers: CML, Markdown, GitHub, and other domain/provider vocabulary belongs
in adapters or consuming components.

The initial executable slice ends at `ReadyForDelivery`. Delivery submission,
merge observation, and post-merge verification are modeled for compatibility
with later slices but are not exposed as commands in this version.

## Component and service

- Component FQN: `org.simplemodeling.textus.ChangeManagement`
- Service: `ChangeManagement`
- Aggregate root: `ChangeProposal`

The service provides the ordered command sequence `createProposal`,
`recordGuidance`, `attachProposedPatch`, `attachCandidateArtifact`,
`attachSemanticDiff`, `recordCandidateReview`, and `markReadyForDelivery`.
Queries `getProposal` and `listProposals` expose retained state.

## Evidence contract

`ChangeArtifact` holds a kind, opaque reference, optional media type, and
optional digest. Source targets, proposed patches, and candidate artifacts are
different artifact roles. `SemanticDiff` records a meaning-level comparison;
`CandidateReview` records its acceptance decision. Neither substitutes for the
patch bytes.

The successful initial lifecycle is:

```text
Detected -> Guided -> Proposed -> CandidateBuilt -> Reviewed -> ReadyForDelivery
```

Commands reject an out-of-order transition. Readiness additionally requires a
patch, candidate artifact, semantic diff, and accepted review.

## Inputs and outputs

Command inputs use `proposalId` to address a proposal. Artifact references are
opaque strings interpreted by their producer or consumer adapter. Each command
returns the current `ChangeProposal`; list returns zero or more proposals.

Example evidence roles:

```text
sourceTarget      Source     source://repository/document
proposedPatch     Patch      patch://proposal-001/1
candidateArtifact Candidate  candidate://proposal-001/1
semanticDiff                 diff://proposal-001/1
reviewResult                 review://proposal-001/1
```

## Configuration and persistence

This initial version has no component-specific configuration keys. Proposal
records use the CNCF entity store selected by the host runtime. Deployments
must configure that runtime persistence according to the CNCF operator guide.

## Failures and limitations

- Duplicate `proposalId` values are rejected.
- Artifact roles that do not match the command are rejected.
- Missing or blank semantic/review summaries are rejected.
- Rejected reviews cannot advance to `Reviewed` or `ReadyForDelivery`.
- Concurrent-write revision enforcement, delivery providers, real pull-request
  creation, merge observation, and re-verification commands are deferred.
- The component does not parse or validate CML, CDM, Markdown, YAML, or RDF.

See [the user guide](user-guide.md) for the first-success workflow. Generated
component help provides platform discovery routes including `/openapi.json`
and `/mcp` when the CAR is running on CNCF.
