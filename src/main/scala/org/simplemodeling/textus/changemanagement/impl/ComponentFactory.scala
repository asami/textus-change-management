package org.simplemodeling.textus.changemanagement.impl

import cats.implicits.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.{ActionCall, FunctionalActionCall}
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId}
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.{EntityQuery, EntitySearchScope}
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.model.directive.Update
import org.simplemodeling.textus.changemanagement.ChangeManagementComponent
import org.simplemodeling.textus.changemanagement.entity.{ChangeProposal as ChangeProposalEntity}
import org.simplemodeling.textus.changemanagement.entity.create.{ChangeProposal as ChangeProposalCreate}
import org.simplemodeling.textus.changemanagement.entity.query.{ChangeProposal as ChangeProposalQuery}
import org.simplemodeling.textus.changemanagement.entity.update.{ChangeProposal as ChangeProposalUpdate}
import org.simplemodeling.textus.changemanagement.value.{
  AttachCandidateArtifact as AttachCandidateArtifactValue,
  AttachProposedPatch as AttachProposedPatchValue,
  AttachSemanticDiff as AttachSemanticDiffValue,
  CreateProposal as CreateProposalValue,
  RecordCandidateReview as RecordCandidateReviewValue,
  RecordGuidance as RecordGuidanceValue,
  ChangeProposalState,
  DeliveryReadiness
}
import org.simplemodeling.textus.changemanagement.workflow.ProposalLifecycle

/*
 * @since   Sep.  3, 2026
 * @version Sep.  3, 2026
 * @author  ASAMI, Tomoharu
 */
final class ComponentFactory extends Component.BundleFactory {
  def primaryFactory: Component.PrimaryComponentFactory =
    ChangeManagementPrimaryFactory

  override def componentletFactories: Vector[Component.ComponentletFactory] =
    Vector.empty
}

abstract class ChangeManagementParticipantFactoryBase extends ChangeManagementComponent.Factory {
  protected final val shared_services =
    Vector(ChangeManagementComponent.ChangeManagementService)

  protected final def component_core(
    name: String,
    componentid: ComponentId
  ): Component.Core =
    spec_create(name, componentid, shared_services)

  override val ChangeManagement: ChangeManagementComponent.ChangeManagementServiceFactory =
    DefaultChangeManagementServiceFactory()
  override val aggregate: ChangeManagementComponent.AggregateServiceFactory =
    AggregateServiceFactoryImpl()
  override val view: ChangeManagementComponent.ViewServiceFactory =
    ViewServiceFactoryImpl()
  override val entity: ChangeManagementComponent.EntityServiceFactory =
    DefaultEntityServiceFactory()
}

final class ChangeManagementPrimaryComponent extends ChangeManagementComponent {
  override def mcpReadyServices: Set[String] = Set.empty
}

object ChangeManagementPrimaryFactory
    extends ChangeManagementParticipantFactoryBase
    with Component.PrimaryComponentFactory {
  override protected def create_Component(params: ComponentCreate): Component =
    new ChangeManagementPrimaryComponent()

  override protected def create_Core(
    params: ComponentCreate,
    comp: Component
  ): Component.Core =
    component_core(ChangeManagementComponent.name, ChangeManagementComponent.componentId)
}

final class DefaultChangeManagementServiceFactory
    extends ChangeManagementComponent.ChangeManagementServiceFactory {
  import ChangeManagementComponent.ChangeManagementService.*

  override def createCreateProposalActionCall(
    core: ActionCall.Core,
    action: CreateProposal
  ): CreateProposalActionCall = CreateProposalActionCallImpl(core, action)

  override def createRecordGuidanceActionCall(
    core: ActionCall.Core,
    action: RecordGuidance
  ): RecordGuidanceActionCall = RecordGuidanceActionCallImpl(core, action)

  override def createAttachProposedPatchActionCall(
    core: ActionCall.Core,
    action: AttachProposedPatch
  ): AttachProposedPatchActionCall = AttachProposedPatchActionCallImpl(core, action)

  override def createAttachCandidateArtifactActionCall(
    core: ActionCall.Core,
    action: AttachCandidateArtifact
  ): AttachCandidateArtifactActionCall = AttachCandidateArtifactActionCallImpl(core, action)

  override def createAttachSemanticDiffActionCall(
    core: ActionCall.Core,
    action: AttachSemanticDiff
  ): AttachSemanticDiffActionCall = AttachSemanticDiffActionCallImpl(core, action)

  override def createRecordCandidateReviewActionCall(
    core: ActionCall.Core,
    action: RecordCandidateReview
  ): RecordCandidateReviewActionCall = RecordCandidateReviewActionCallImpl(core, action)

  override def createMarkReadyForDeliveryActionCall(
    core: ActionCall.Core,
    action: MarkReadyForDelivery
  ): MarkReadyForDeliveryActionCall = MarkReadyForDeliveryActionCallImpl(core, action)

  override def createGetProposalActionCall(
    core: ActionCall.Core,
    action: GetProposal
  ): GetProposalActionCall = GetProposalActionCallImpl(core, action)

  override def createListProposalsActionCall(
    core: ActionCall.Core,
    action: ListProposals
  ): ListProposalsActionCall = ListProposalsActionCallImpl(core, action)
}

object DefaultChangeManagementServiceFactory {
  def apply(): DefaultChangeManagementServiceFactory =
    new DefaultChangeManagementServiceFactory()
}

private trait ChangeManagementActionSupport {
  self: FunctionalActionCall =>

  protected final def all_proposals: ExecUowM[Vector[ChangeProposalEntity]] =
    entity_search_internal[ChangeProposalEntity](EntityQuery(
      ChangeProposalQuery.collectionId,
      Query.fromRecord(Record.empty),
      EntitySearchScope.Store
    )).map(_.data.toVector)

  protected final def find_proposal(proposalid: String): ExecUowM[ChangeProposalEntity] =
    all_proposals.flatMap { proposals =>
      exec_from(
        proposals.find(_.proposalId.value == proposalid) match {
          case Some(proposal) => Consequence.success(proposal)
          case None => Consequence.resourceNotFound(proposalid)
        }
      )
    }

  protected final def persist(
    before: ChangeProposalEntity,
    after: ChangeProposalEntity,
    patch: Consequence[ChangeProposalUpdate]
  ): ExecUowM[OperationResponse] =
    for {
      update <- exec_from(patch)
      _ <- entity_update(before.id, update)
    } yield proposal_response(after)

  protected final def proposal_response(proposal: ChangeProposalEntity): OperationResponse =
    OperationResponse(Record.dataAuto(
      "item" -> proposal.toRecord().upsertSingle("id", proposal.id.value)
    ))
}

private final case class CreateProposalActionCallImpl(
  core: ActionCall.Core,
  override val action: ChangeManagementComponent.ChangeManagementService.CreateProposal
) extends ChangeManagementComponent.ChangeManagementService.CreateProposalActionCall
    with ChangeManagementActionSupport {
  protected def build_Program: ExecUowM[OperationResponse] =
    for {
      input <- exec_from(CreateProposalValue.createC(action.record))
      _ <- exec_from(ProposalLifecycle.validateNewSource(input.sourceTarget))
      proposals <- all_proposals
      _ <- exec_from(
        if (proposals.exists(_.proposalId == input.proposalId))
          Consequence.stateConflict(s"proposal '${input.proposalId.value}' already exists")
        else
          Consequence.unit
      )
      created <- entity_create(ChangeProposalCreate(
        None,
        input.proposalId,
        input.subject,
        input.originatingFinding,
        None,
        input.sourceTarget,
        None,
        None,
        None,
        None,
        DeliveryReadiness.NotReady,
        None,
        None,
        ChangeProposalState.Detected,
        0
      ))
      entity = ChangeProposalEntity(
        created.id,
        input.proposalId,
        input.subject,
        input.originatingFinding,
        None,
        input.sourceTarget,
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
    } yield proposal_response(entity)
}

private final case class RecordGuidanceActionCallImpl(
  core: ActionCall.Core,
  override val action: ChangeManagementComponent.ChangeManagementService.RecordGuidance
) extends ChangeManagementComponent.ChangeManagementService.RecordGuidanceActionCall
    with ChangeManagementActionSupport {
  protected def build_Program: ExecUowM[OperationResponse] =
    for {
      input <- exec_from(RecordGuidanceValue.createC(action.record))
      current <- find_proposal(input.proposalId.value)
      updated <- exec_from(ProposalLifecycle.recordGuidance(current, input.guidance))
      result <- persist(current, updated, new ChangeProposalUpdate.Builder()
        .withGuidance(Update.set(input.guidance))
        .withState(Update.set(updated.state))
        .withRevision(Update.set(updated.revision))
        .buildC())
    } yield result
}

private final case class AttachProposedPatchActionCallImpl(
  core: ActionCall.Core,
  override val action: ChangeManagementComponent.ChangeManagementService.AttachProposedPatch
) extends ChangeManagementComponent.ChangeManagementService.AttachProposedPatchActionCall
    with ChangeManagementActionSupport {
  protected def build_Program: ExecUowM[OperationResponse] =
    for {
      input <- exec_from(AttachProposedPatchValue.createC(action.record))
      current <- find_proposal(input.proposalId.value)
      updated <- exec_from(ProposalLifecycle.attachProposedPatch(current, input.proposedPatch))
      result <- persist(current, updated, new ChangeProposalUpdate.Builder()
        .withProposedPatch(Update.set(input.proposedPatch))
        .withState(Update.set(updated.state))
        .withRevision(Update.set(updated.revision))
        .buildC())
    } yield result
}

private final case class AttachCandidateArtifactActionCallImpl(
  core: ActionCall.Core,
  override val action: ChangeManagementComponent.ChangeManagementService.AttachCandidateArtifact
) extends ChangeManagementComponent.ChangeManagementService.AttachCandidateArtifactActionCall
    with ChangeManagementActionSupport {
  protected def build_Program: ExecUowM[OperationResponse] =
    for {
      input <- exec_from(AttachCandidateArtifactValue.createC(action.record))
      current <- find_proposal(input.proposalId.value)
      updated <- exec_from(ProposalLifecycle.attachCandidateArtifact(current, input.candidateArtifact))
      result <- persist(current, updated, new ChangeProposalUpdate.Builder()
        .withCandidateArtifact(Update.set(input.candidateArtifact))
        .withState(Update.set(updated.state))
        .withRevision(Update.set(updated.revision))
        .buildC())
    } yield result
}

private final case class AttachSemanticDiffActionCallImpl(
  core: ActionCall.Core,
  override val action: ChangeManagementComponent.ChangeManagementService.AttachSemanticDiff
) extends ChangeManagementComponent.ChangeManagementService.AttachSemanticDiffActionCall
    with ChangeManagementActionSupport {
  protected def build_Program: ExecUowM[OperationResponse] =
    for {
      input <- exec_from(AttachSemanticDiffValue.createC(action.record))
      current <- find_proposal(input.proposalId.value)
      updated <- exec_from(ProposalLifecycle.attachSemanticDiff(current, input.semanticDiff))
      result <- persist(current, updated, new ChangeProposalUpdate.Builder()
        .withSemanticDiff(Update.set(input.semanticDiff))
        .withRevision(Update.set(updated.revision))
        .buildC())
    } yield result
}

private final case class RecordCandidateReviewActionCallImpl(
  core: ActionCall.Core,
  override val action: ChangeManagementComponent.ChangeManagementService.RecordCandidateReview
) extends ChangeManagementComponent.ChangeManagementService.RecordCandidateReviewActionCall
    with ChangeManagementActionSupport {
  protected def build_Program: ExecUowM[OperationResponse] =
    for {
      input <- exec_from(RecordCandidateReviewValue.createC(action.record))
      current <- find_proposal(input.proposalId.value)
      updated <- exec_from(ProposalLifecycle.recordCandidateReview(current, input.reviewResult))
      result <- persist(current, updated, new ChangeProposalUpdate.Builder()
        .withReviewResult(Update.set(input.reviewResult))
        .withState(Update.set(updated.state))
        .withRevision(Update.set(updated.revision))
        .buildC())
    } yield result
}

private final case class MarkReadyForDeliveryActionCallImpl(
  core: ActionCall.Core,
  override val action: ChangeManagementComponent.ChangeManagementService.MarkReadyForDelivery
) extends ChangeManagementComponent.ChangeManagementService.MarkReadyForDeliveryActionCall
    with ChangeManagementActionSupport {
  protected def build_Program: ExecUowM[OperationResponse] =
    for {
      proposalId <- exec_from(action.record.getAs[org.simplemodeling.textus.changemanagement.datatype.ProposalId]("proposalId")
        .map(Consequence.success)
        .getOrElse(Consequence.recordNotFound("proposalId", action.record)))
      current <- find_proposal(proposalId.value)
      updated <- exec_from(ProposalLifecycle.markReadyForDelivery(current))
      result <- persist(current, updated, new ChangeProposalUpdate.Builder()
        .withReadiness(Update.set(updated.readiness))
        .withState(Update.set(updated.state))
        .withRevision(Update.set(updated.revision))
        .buildC())
    } yield result
}

private final case class GetProposalActionCallImpl(
  core: ActionCall.Core,
  override val action: ChangeManagementComponent.ChangeManagementService.GetProposal
) extends ChangeManagementComponent.ChangeManagementService.GetProposalActionCall
    with ChangeManagementActionSupport {
  protected def build_Program: ExecUowM[OperationResponse] =
    for {
      proposalId <- exec_from(action.record.getAs[org.simplemodeling.textus.changemanagement.datatype.ProposalId]("proposalId")
        .map(Consequence.success)
        .getOrElse(Consequence.recordNotFound("proposalId", action.record)))
      proposal <- find_proposal(proposalId.value)
    } yield proposal_response(proposal)
}

private final case class ListProposalsActionCallImpl(
  core: ActionCall.Core,
  override val action: ChangeManagementComponent.ChangeManagementService.ListProposals
) extends ChangeManagementComponent.ChangeManagementService.ListProposalsActionCall
    with ChangeManagementActionSupport {
  protected def build_Program: ExecUowM[OperationResponse] =
    for {
      proposals <- all_proposals
      state = action.record.getAs[ChangeProposalState]("state")
      offset = action.record.getInt("offset").getOrElse(0).max(0)
      limit = action.record.getInt("limit").getOrElse(100).max(0)
      selected = proposals
        .filter(proposal => state.forall(_ == proposal.state))
        .sortBy(_.proposalId.value)
        .drop(offset)
        .take(limit)
    } yield OperationResponse(Record.dataAuto(
      "items" -> selected.map(_.toRecord())
    ))
}

final class DefaultEntityServiceFactory extends ChangeManagementComponent.EntityServiceFactory
object DefaultEntityServiceFactory {
  def apply(): DefaultEntityServiceFactory = new DefaultEntityServiceFactory()
}

final class AggregateServiceFactoryImpl extends ChangeManagementComponent.AggregateServiceFactory
object AggregateServiceFactoryImpl {
  def apply(): AggregateServiceFactoryImpl = new AggregateServiceFactoryImpl()
}

final class ViewServiceFactoryImpl extends ChangeManagementComponent.ViewServiceFactory
object ViewServiceFactoryImpl {
  def apply(): ViewServiceFactoryImpl = new ViewServiceFactoryImpl()
}
