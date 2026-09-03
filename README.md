# Textus Change Management

`textus-change-management` is a provider-neutral Textus CAR for moving an
observed finding toward a reviewed and delivery-ready change.

The component owns this evidence-preserving loop:

```text
Finding -> Guidance -> Proposed Patch -> Candidate Artifact
        -> Semantic Diff -> Candidate Review -> Ready for Delivery
```

Delivery, merge, and re-verification are represented in the domain model, but
the initial executable slice stops at `ReadyForDelivery`. A candidate is
therefore reviewed before a pull request or another provider-specific delivery
record is created.

## Component contract

- artifact: `textus-change-management`
- component: `org.simplemodeling.textus.ChangeManagement`
- Scala package: `org.simplemodeling.textus.changemanagement`
- version: `0.1.0-SNAPSHOT`
- service: `ChangeManagement`

The initial command path is:

1. `createProposal`
2. `recordGuidance`
3. `attachProposedPatch`
4. `attachCandidateArtifact`
5. `attachSemanticDiff`
6. `recordCandidateReview`
7. `markReadyForDelivery`

`getProposal` and `listProposals` expose retained proposal state. Source
patches, semantic diffs, and candidate-review evidence are distinct values.

## Consumer boundaries

`textus-cbd-support` may supply CML/CDM/design artifacts and semantic review
providers. `textus-bok` may supply Markdown/YAML/information-model artifacts
and knowledge-oriented review providers. Neither format vocabulary belongs in
this component's generic core.

`ChangeDelivery` records an opaque provider, external identifier, URI, and
status. GitHub pull-request creation is intentionally deferred to a provider
adapter and is not implemented in this baseline.

## Development

Use the repository's serialized SBT workflow to run:

```text
/Users/asami/.codex/skills/cncf-sbt-serial-execution/scripts/run-sbt-serial.sh \
  --batch test cozyBuildCAR
```

Generated Scala sources are under `target/scala-3.3.8/src_managed/main`.
