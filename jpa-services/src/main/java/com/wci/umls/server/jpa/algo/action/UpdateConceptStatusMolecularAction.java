/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.action;

import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.content.ConceptJpa;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/**
 * A molecular action for updating a concept workflow status.
 */
public class UpdateConceptStatusMolecularAction
    extends AbstractMolecularAction {

  /** The concept pre updates. */
  private Concept conceptPreUpdates;

  /** The concept post updates. */
  private Concept conceptPostUpdates;

  /** The workflow status. */
  private WorkflowStatus workflowStatus;

  /**
   * Instantiates an empty {@link UpdateConceptStatusMolecularAction}.
   *
   * @throws Exception the exception
   */
  public UpdateConceptStatusMolecularAction() throws Exception {
    super();
    // n/a
  }

  /**
   * Returns the concept pre updates.
   *
   * @return the concept pre updates
   */
  public Concept getConceptPreUpdates() {
    return conceptPreUpdates;
  }

  /**
   * Returns the concept post updates.
   *
   * @return the concept post updates
   */
  public Concept getConceptPostUpdates() {
    return conceptPostUpdates;
  }

  /**
   * Sets the workflow status.
   *
   * @param workflowStatus the workflow status
   */
  public void setWorkflowStatus(WorkflowStatus workflowStatus) {
    this.workflowStatus = workflowStatus;
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    ValidationResult validationResult = new ValidationResultJpa();
    // Perform action specific validation - n/a

    // Metadata referential integrity checking

    // Check preconditions
    validationResult.merge(super.checkPreconditions());
    validationResult.merge(validateConcept(this.getProject(), this.getConcept()));
    return validationResult;
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public void compute() throws Exception {
    //
    // Perform the action (contentService will create atomic actions for CRUD
    // operations)
    //

    // Make copy of the Concept before changes, to pass into
    // change event
    conceptPreUpdates = new ConceptJpa(getConcept(), false);

    // Make a copy of the concept
    Concept updateConcept = new ConceptJpa(getConcept(),true);
    
    //
    // Change status of the concept
    //
    updateConcept.setWorkflowStatus(this.workflowStatus);

    //
    // update the Concept
    //
    updateConcept(updateConcept);

    // Make copy of the Concept after changes, to pass into
    // change event
    conceptPostUpdates = new ConceptJpa(getConcept(), false);

    // log the REST calls
    addLogEntry(getUserName(), getProject().getId(), getConcept().getId(),
        getActivityId(), getWorkId(), getName() + " " + getConcept());

  }

}
