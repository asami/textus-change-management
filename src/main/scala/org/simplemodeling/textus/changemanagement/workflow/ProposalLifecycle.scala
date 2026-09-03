package org.simplemodeling.textus.changemanagement.workflow

import org.goldenport.Consequence
import org.simplemodeling.textus.changemanagement.datatype.GuidanceStatement
import org.simplemodeling.textus.changemanagement.entity.ChangeProposal
import org.simplemodeling.textus.changemanagement.value.{CandidateReview, ChangeArtifact, ChangeArtifactKind, ChangeProposalState, DeliveryReadiness, ReviewDecision, SemanticDiff}

/*
 * @since   Sep.  3, 2026
 * @version Sep.  3, 2026
 * @author  ASAMI, Tomoharu
 */
/** Provider-neutral transition policy for the initial proposal workflow. */
object ProposalLifecycle {
  def recordGuidance(
    proposal: ChangeProposal,
    guidance: GuidanceStatement
  ): Consequence[ChangeProposal] =
    _transition(proposal, ChangeProposalState.Detected, ChangeProposalState.Guided) {
      proposal.copy(guidance = Some(guidance), revision = proposal.revision + 1)
    }

  def attachProposedPatch(
    proposal: ChangeProposal,
    patch: ChangeArtifact
  ): Consequence[ChangeProposal] =
    for {
      _ <- _require_artifact_kind(patch, ChangeArtifactKind.Patch, "proposedPatch")
      result <- _transition(proposal, ChangeProposalState.Guided, ChangeProposalState.Proposed) {
        proposal.copy(proposedPatch = Some(patch), revision = proposal.revision + 1)
      }
    } yield result

  def attachCandidateArtifact(
    proposal: ChangeProposal,
    candidate: ChangeArtifact
  ): Consequence[ChangeProposal] =
    for {
      _ <- _require_artifact_kind(candidate, ChangeArtifactKind.Candidate, "candidateArtifact")
      result <- _transition(proposal, ChangeProposalState.Proposed, ChangeProposalState.CandidateBuilt) {
        proposal.copy(candidateArtifact = Some(candidate), revision = proposal.revision + 1)
      }
    } yield result

  def attachSemanticDiff(
    proposal: ChangeProposal,
    diff: SemanticDiff
  ): Consequence[ChangeProposal] =
    for {
      _ <- _require_state(proposal, ChangeProposalState.CandidateBuilt)
      _ <- _require_non_empty(diff.summary.value, "semanticDiff.summary")
    } yield proposal.copy(semanticDiff = Some(diff), revision = proposal.revision + 1)

  def recordCandidateReview(
    proposal: ChangeProposal,
    review: CandidateReview
  ): Consequence[ChangeProposal] =
    for {
      _ <- _require_state(proposal, ChangeProposalState.CandidateBuilt)
      _ <- _require_present(proposal.semanticDiff, "semanticDiff")
      _ <- _require_accepted(review)
    } yield proposal.copy(
      reviewResult = Some(review),
      state = ChangeProposalState.Reviewed,
      revision = proposal.revision + 1
    )

  def markReadyForDelivery(proposal: ChangeProposal): Consequence[ChangeProposal] =
    for {
      _ <- _require_state(proposal, ChangeProposalState.Reviewed)
      _ <- _require_present(proposal.proposedPatch, "proposedPatch")
      _ <- _require_present(proposal.candidateArtifact, "candidateArtifact")
      _ <- _require_present(proposal.semanticDiff, "semanticDiff")
      review <- _require_present(proposal.reviewResult, "reviewResult")
      _ <- _require_accepted(review)
    } yield proposal.copy(
      readiness = DeliveryReadiness.Ready,
      state = ChangeProposalState.ReadyForDelivery,
      revision = proposal.revision + 1
    )

  def validateNewSource(source: ChangeArtifact): Consequence[Unit] =
    _require_artifact_kind(source, ChangeArtifactKind.Source, "sourceTarget")

  private def _transition(
    proposal: ChangeProposal,
    from: ChangeProposalState,
    to: ChangeProposalState
  )(f: => ChangeProposal): Consequence[ChangeProposal] =
    _require_state(proposal, from).map(_ => f.copy(state = to))

  private def _require_state(
    proposal: ChangeProposal,
    expected: ChangeProposalState
  ): Consequence[Unit] =
    if (proposal.state == expected)
      Consequence.unit
    else
      Consequence.stateConflict(
        s"proposal '${proposal.proposalId.value}' is ${proposal.state.value}; expected ${expected.value}"
      )

  private def _require_artifact_kind(
    artifact: ChangeArtifact,
    expected: ChangeArtifactKind,
    field: String
  ): Consequence[Unit] =
    if (artifact.kind == expected)
      _require_non_empty(artifact.reference.value, s"$field.reference")
    else
      Consequence.operationInvalid(s"$field.kind must be ${expected.value}")

  private def _require_accepted(review: CandidateReview): Consequence[Unit] =
    if (review.decision == ReviewDecision.Accepted)
      _require_non_empty(review.summary.value, "reviewResult.summary")
    else
      Consequence.operationInvalid("reviewResult.decision must be Accepted before delivery readiness")

  private def _require_present[A](value: Option[A], field: String): Consequence[A] =
    value match {
      case Some(v) => Consequence.success(v)
      case None => Consequence.operationInvalid(s"$field is required")
    }

  private def _require_non_empty(value: String, field: String): Consequence[Unit] =
    if (value.trim.nonEmpty) Consequence.unit
    else Consequence.operationInvalid(s"$field must not be empty")
}
