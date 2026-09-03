# Textus Change Management initial design

Date: 2026-09-03

## Purpose

Change-management behavior has appeared in several Textus components as local
workflow code. This project extracts the reusable part into a normal Textus
CAR. It is not a feature of the CNCF framework itself.

The target loop is:

```text
Finding -> Guidance -> Proposed Patch -> Candidate Model/Artifact
        -> Semantic Diff -> Candidate Review -> PR or other delivery
        -> Human Review -> Merge -> Re-verification
```

The first implementation closes at `ReadyForDelivery`. Provider submission,
merge observation, and re-verification remain modeled states so later slices
can extend the workflow without changing the meaning of earlier evidence.

## Responsibility boundary

The CAR owns proposal identity, state, transition rules, generic artifact
references, semantic-diff evidence, candidate-review evidence, delivery
readiness, generic delivery references, and verification results.

It does not parse CML, CDM, Markdown, YAML, RDF, or provider APIs. It does not
generate a patch, decide domain-specific semantic equivalence, create a GitHub
pull request, merge a branch, or replace human review. Those responsibilities
belong to producer, reviewer, and delivery-provider adapters.

## Initial model

`ChangeProposal` is the aggregate root. Its public proposal ID is distinct
from the CNCF entity-store ID. The proposal retains:

- subject and originating finding;
- optional guidance;
- source target, proposed patch, and candidate artifact as typed generic
  `ChangeArtifact` values;
- `SemanticDiff`, separate from patch bytes;
- `CandidateReview`, separate from both patch and semantic diff;
- readiness, delivery, and verification results;
- lifecycle state and optimistic revision number.

The lifecycle vocabulary is:

```text
Detected -> Guided -> Proposed -> CandidateBuilt -> Reviewed
         -> ReadyForDelivery -> Delivered -> Merged -> Verified
```

Terminal or failure states are `Rejected`, `BuildFailed`, `ReviewFailed`,
`DeliveryFailed`, `ClosedWithoutMerge`, and `VerificationFailed`.

The executable baseline follows the successful path through
`ReadyForDelivery`. It requires a candidate artifact, semantic diff, and an
accepted candidate review before readiness can be marked.

The scalar leaves use role-specific nominal names such as `ProposalId`,
`ArtifactReference`, `FindingStatement`, and `ReviewStatement`. Cozy CAR lint
reports unconstrained string wrappers as advisory candidates for a constrained
Value or predefined scalar. They remain nominal in this first slice because
the public contract intentionally distinguishes identifiers, references,
provider names, and evidence statements. Constraints can be tightened without
collapsing these roles after consumer examples establish their valid lexical
forms.

## Provider boundaries

The core exposes a small provider-neutral boundary rather than one abstraction
per lifecycle step:

- artifact producers supply `ChangeArtifact` references;
- semantic reviewers supply `SemanticDiff` and `CandidateReview` values;
- delivery adapters will consume a ready proposal and return `ChangeDelivery`;
- verification adapters will later return `VerificationResult`.

`ChangeDelivery.provider` is deliberately opaque. GitHub, GitLab, a local
patch queue, or a document publication workflow can implement the same
boundary without entering the core vocabulary.

## Consumers

`textus-cbd-support` is the first design/model consumer. It can use CML, CDM,
or design-document producers and semantic reviewers while the core sees only
artifact references and review facts.

`textus-bok` is the knowledge consumer. It can use Markdown, YAML,
information-model, or knowledge-diff providers under the same generic
proposal lifecycle.

## Candidate CNCF primitives

| Mechanism | Why generic | Local implementation | Consumers | Evidence required | Decision |
|---|---|---|---|---|---|
| Entity-bound state-machine transition enforcement | Any aggregate workflow needs legal transition checks at operation execution | Change proposal transition guard | Change Management; later SalesOrder and other workflow components | Multiple component implementations show the same transition guard and observability contract | Deferred; keep local until CNCF state-machine execution is proven reusable |
| Optimistic entity revision/update helper | Stateful operations need stale-write protection independent of domain vocabulary | Proposal revision field and guarded update | Change Management, CBD Support | Repeated identical update/revision patterns across CARs | Deferred pending broader evidence |
| Provider-neutral artifact reference | Review, generation, and delivery workflows exchange typed evidence without embedding payloads | `ChangeArtifact` value | CBD Support, BoK, document workflows | At least two consumers use compatible reference semantics | Candidate for later extraction, not now |
| Semantic-diff and review evidence attachment | Executable workflows need to retain why a change is acceptable | `SemanticDiff` and `CandidateReview` values | CBD Support and BoK | Cross-domain use without importing CML/BoK vocabulary | Candidate for later extraction, not now |

No primitive is extracted into CNCF in this initial slice. The CAR provides a
working proving ground and records the evidence needed for a later decision.

## Implementation feedback

Cozy's metadata lint accepted both short operation attributes and nested
descriptions, but the Scala modeler treated short `- type`, `- input`, and
`- output` attributes placed inside a `DESCRIPTION` section as prose and
returned an empty generation result. The CML now uses the canonical nested
`TYPE`, `INPUT`, and `OUTPUT` headings used by established CARs. This preserves
generated help text and executable operation metadata through the same source.

## Non-goals and deferred work

- Real GitHub or other forge integration.
- Automatic pull-request creation or merge.
- CML/CDM/Markdown/YAML/RDF parsing in the generic core.
- AI provider selection or prompt execution.
- Human-review replacement.
- Delivery and post-merge verification commands in the first slice.
- Immediate extraction of speculative CNCF framework APIs.

The next phase should add a delivery-provider SPI and one consumer adapter only
after this stateful core has packaged and run as a CAR.

## Canonical identity decision

The initial handoff proposed component `TextusChangeManagement` and package
`org.simplemodeling.textus.change.management`. Current Cozy canonical identity
derives the artifact and Scala package from namespace plus component ID. Under
namespace `org.simplemodeling.textus`, ID `ChangeManagement` yields the required
artifact `textus-change-management` and package
`org.simplemodeling.textus.changemanagement`. The project therefore uses the
canonical component FQN `org.simplemodeling.textus.ChangeManagement` and the
derived package rather than keeping a second, conflicting identity spelling.
