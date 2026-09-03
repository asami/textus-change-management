package org.simplemodeling.textus.changemanagement

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.goldenport.Consequence
import org.goldenport.cncf.component.{ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.{DataStoreContext, EntityStoreContext, ExecutionContext, ScopeContext, ScopeKind}
import org.goldenport.cncf.datastore.{DataStore, DataStoreSpace}
import org.goldenport.cncf.entity.EntityStoreSpace
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.cncf.testutil.RuntimeBindingAdmissionFixture
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ResolvedConfiguration}
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.model.datatype.EntityId
import org.simplemodeling.textus.changemanagement.datatype.*
import org.simplemodeling.textus.changemanagement.entity.ChangeProposal
import org.simplemodeling.textus.changemanagement.impl.{ChangeManagementPrimaryComponent, ComponentFactory}
import org.simplemodeling.textus.changemanagement.value.*
import org.simplemodeling.textus.changemanagement.workflow.ProposalLifecycle
import org.simplemodeling.textus.changemanagement.value.{ChangeArtifactKind, ChangeProposalState, DeliveryReadiness, ReviewDecision}

/*
 * @since   Sep.  3, 2026
 * @version Sep.  3, 2026
 * @author  ASAMI, Tomoharu
 */
final class ComponentFactorySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Textus Change Management" should {
    "publish only the semantic workflow service" in {
      Given("the generated component bundle factory")
      val factory = new ComponentFactory()

      When("the primary factory is requested")
      val primary = factory.primaryFactory

      Then("the semantic component boundary is available")
      primary should not be null
    }

    "advance a reviewed candidate to delivery readiness" in {
      Given("a newly detected proposal and separately typed evidence")
      val detected = _proposal()
      val patch = _artifact(ChangeArtifactKind.Patch, "patch://proposal-001/1")
      val candidate = _artifact(ChangeArtifactKind.Candidate, "candidate://proposal-001/1")
      val diff = SemanticDiff(
        SemanticDiffStatement("adds a provider-neutral proposal lifecycle"),
        Some(ArtifactReference("diff://proposal-001/1"))
      )
      val review = CandidateReview(
        ReviewDecision.Accepted,
        ReviewStatement("candidate preserves the requested domain boundary"),
        Some(ArtifactReference("review://proposal-001/1"))
      )

      When("the representative initial workflow is executed")
      val ready = for {
        guided <- ProposalLifecycle.recordGuidance(detected, GuidanceStatement("prepare a generic CAR"))
        proposed <- ProposalLifecycle.attachProposedPatch(guided, patch)
        built <- ProposalLifecycle.attachCandidateArtifact(proposed, candidate)
        compared <- ProposalLifecycle.attachSemanticDiff(built, diff)
        reviewed <- ProposalLifecycle.recordCandidateReview(compared, review)
        result <- ProposalLifecycle.markReadyForDelivery(reviewed)
      } yield result

      Then("all evidence remains distinct and the candidate is ready")
      val result = ready.toOption.getOrElse(fail(ready.toString))
      result.state shouldBe ChangeProposalState.ReadyForDelivery
      result.readiness shouldBe DeliveryReadiness.Ready
      result.proposedPatch shouldBe Some(patch)
      result.candidateArtifact shouldBe Some(candidate)
      result.semanticDiff shouldBe Some(diff)
      result.reviewResult shouldBe Some(review)
      result.revision shouldBe 6
    }

    "persist service operations through the generated component boundary" in {
      import ChangeManagementComponent.ChangeManagementService.{
        CreateProposal as CreateProposalAction,
        GetProposal as GetProposalAction,
        RecordGuidance as RecordGuidanceAction
      }

      Given("an assembled component with an isolated in-memory entity store")
      val component = _component()
      given ExecutionContext = component.logic.executionContext()
      val request = Request.of("ChangeManagement", "ChangeManagement", "createProposal")
      val create = CreateProposalAction.unsafeForTest(request, Record.dataAuto(
        "proposalId" -> ProposalId("proposal-service-001"),
        "subject" -> ChangeSubject("Exercise the generated service boundary"),
        "originatingFinding" -> FindingStatement("persistence needs executable evidence"),
        "sourceTarget" -> Record.dataAuto(
          "kind" -> "Source",
          "reference" -> "source://service-boundary"
        )
      ))

      When("the proposal is created, guided, and loaded through generated operations")
      val created = _item(_execute(component, create))
      val guidance = RecordGuidanceAction.unsafeForTest(
        Request.of("ChangeManagement", "ChangeManagement", "recordGuidance"),
        Record.dataAuto(
          "proposalId" -> ProposalId("proposal-service-001"),
          "guidance" -> GuidanceStatement("retain typed evidence at the service boundary")
        )
      )
      val guided = _item(_execute(component, guidance))
      val get = GetProposalAction.unsafeForTest(
        Request.of("ChangeManagement", "ChangeManagement", "getProposal"),
        Record.dataAuto("proposalId" -> ProposalId("proposal-service-001"))
      )
      val retained = _item(_execute(component, get))

      Then("the persisted aggregate is observable through the query operation")
      val createdproposal = ChangeProposal.createC(created).toOption.getOrElse(fail("created proposal is invalid"))
      val guidedproposal = ChangeProposal.createC(guided).toOption.getOrElse(fail("guided proposal is invalid"))
      val retainedproposal = ChangeProposal.createC(retained).toOption.getOrElse(fail("retained proposal is invalid"))
      createdproposal.state shouldBe ChangeProposalState.Detected
      guidedproposal.state shouldBe ChangeProposalState.Guided
      retainedproposal.proposalId shouldBe ProposalId("proposal-service-001")
      retainedproposal.state shouldBe ChangeProposalState.Guided
      retainedproposal.revision shouldBe 1
      retainedproposal.guidance shouldBe
        Some(GuidanceStatement("retain typed evidence at the service boundary"))
    }

    "reject out-of-order or unreviewed readiness transitions" in {
      Given("a detected proposal without a candidate, semantic diff, or review")
      val detected = _proposal()

      When("delivery readiness is requested too early")
      val result = ProposalLifecycle.markReadyForDelivery(detected)

      Then("the state contract rejects the transition")
      result.isSuccess shouldBe false
      result.show should include ("expected Reviewed")
    }
  }

  private def _proposal(): ChangeProposal =
    ChangeProposal(
      EntityId("ChangeManagement", "proposal_001", ChangeProposal.collectionId),
      ProposalId("proposal-001"),
      ChangeSubject("Introduce reusable change management"),
      FindingStatement("change workflows are duplicated across consumers"),
      None,
      _artifact(ChangeArtifactKind.Source, "source://textus/consumer"),
      None,
      None,
      None,
      None,
      DeliveryReadiness.NotReady,
      None,
      None,
      ChangeProposalState.Detected,
      0
    )

  private def _artifact(kind: ChangeArtifactKind, reference: String): ChangeArtifact =
    ChangeArtifact(kind, ArtifactReference(reference), None, None)

  private def _component(): ChangeManagementPrimaryComponent = {
    val base = ExecutionContext.create()
    val datastorespace = new DataStoreSpace().addDataStore(DataStore.inMemorySearchable())
    val entitystorespace = EntityStoreSpace.create(
      ResolvedConfiguration(Configuration.empty, ConfigurationTrace.empty)
    )
    val scope = ScopeContext.Instance(ScopeContext.Core(
      kind = ScopeKind.Subsystem,
      name = "textus-change-management-spec",
      parent = None,
      observabilityContext = base.observability,
      httpDriverOption = None,
      datastore = Some(DataStoreContext(datastorespace)),
      entitystore = Some(EntityStoreContext(entitystorespace))
    ))
    val subsystem = RuntimeBindingAdmissionFixture.admit(new Subsystem(
      name = "textus-change-management-spec",
      scopecontext = Some(scope),
      configuration = ResolvedConfiguration(Configuration.empty, ConfigurationTrace.empty)
    ))
    val bundle = new ComponentFactory().create(ComponentCreate(subsystem, ComponentOrigin.Main))
    subsystem.add(bundle.participants)
    bundle.primary.asInstanceOf[ChangeManagementPrimaryComponent]
  }

  private def _execute(
    component: ChangeManagementPrimaryComponent,
    action: org.goldenport.cncf.action.Action
  )(using context: ExecutionContext): Consequence[OperationResponse] =
    component.logic.executeAction(action, context)

  private def _item(result: Consequence[OperationResponse]): Record =
    result match {
      case Consequence.Success(OperationResponse.RecordResponse(record)) =>
        record.getRecord("item").getOrElse(fail("proposal item missing"))
      case Consequence.Success(other) => fail(s"expected record response but got $other")
      case Consequence.Failure(conclusion) => fail(s"operation failed: ${conclusion.show}")
    }
}
