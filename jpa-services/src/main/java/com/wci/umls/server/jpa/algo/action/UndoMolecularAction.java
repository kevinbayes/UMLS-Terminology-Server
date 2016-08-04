/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.action;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.HasLastModified;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.model.actions.AtomicAction;
import com.wci.umls.server.model.actions.MolecularAction;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * A molecular action for undoing a previously performed action.
 */
public class UndoMolecularAction extends AbstractMolecularAction {

  /** The molecular action id. */
  private Long molecularActionId;

  /**
   * Instantiates an empty {@link UndoMolecularAction}.
   *
   * @throws Exception the exception
   */
  public UndoMolecularAction() throws Exception {
    super();
    // n/a
  }

  /**
   * Returns the molecular action id.
   *
   * @return the molecular action id
   */
  public Long getMolecularActionId() {
    return molecularActionId;
  }

  /**
   * Sets the molecular action id.
   *
   * @param molecularActionId the molecular action id
   */
  public void setMolecularActionId(Long molecularActionId) {
    this.molecularActionId = molecularActionId;
  }

  /**
   * Check preconditions.
   *
   * @return the validation result
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    final ValidationResult validationResult = new ValidationResultJpa();
    // Perform action specific validation - n/a

    // Metadata referential integrity checking

    // Check if action has already been undone
    if (getMolecularAction(molecularActionId) == null) {
      rollback();
      throw new LocalException(
          "Molecular action does not exist " + molecularActionId);
    }

    // Check if action has already been undone
    if (getMolecularAction(molecularActionId).isUndoneFlag()) {
      rollback();
      throw new LocalException("Cannot undo Molecular action "
          + molecularActionId + " - it has already been undone.");
    }

    // Check preconditions
    validationResult.merge(super.checkPreconditions());

    return validationResult;
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  /* see superclass */
  @Override
  public void compute() throws Exception {

    // Call up the molecular Action we're undoing
    final MolecularAction undoMolecularAction =
        getMolecularAction(molecularActionId);

    // Perform the opposite action for each of the molecular action's atomic
    // actions
    final List<AtomicAction> atomicActions =
        undoMolecularAction.getAtomicActions();

    // REVERSE Sort actions by id (order inserted into DB)
    Collections.sort(atomicActions, (a1, a2) -> a2.getId().compareTo(a1.getId()));
    
    // Iterate through atomic actions IN REVERSE ORDER
    for (final AtomicAction a : atomicActions) {

      //
      // Undo add (null old value with "id" field)
      //
      if (isAddAction(a)) {

        // Get the object that was added, and make sure it still exists
        final Object referencedObject = getReferencedObject(a);
        removeObject(referencedObject);

      }

      //
      // Undo remove
      //
      else if (isRemoveAction(a)) {

        // Get the class of the object we're looking for, so we can pass it into
        // the Hibernate query

        final AuditReader reader = AuditReaderFactory.get(manager);
        final AuditQuery query = reader.createQuery()
            // last updated revision
            .forRevisionsOfEntity(Class.forName(a.getClassName()), true, true)
            .addProjection(AuditEntity.revisionNumber().max())
            // add id and owner as constraints
            .add(AuditEntity.property("id").eq(a.getObjectId()));
        final Number revision = (Number) query.getSingleResult();
        final HasLastModified returnedObject =
            (HasLastModified) reader.find(Class.forName(a.getClassName()),
                a.getClassName(), a.getObjectId(), revision, true);

        // Recover the object here (id is set already so this works better than
        // "add")
        updateHasLastModified(returnedObject);

      }

      //
      // Undo a collections action
      //
      else if (isCollectionsAction(a)) {

        // Obtain the object with a collection (based on the class name)
        final HasLastModified containerObject = getReferencedObject(a);
        // Obtain the referenced object (based on collection class name and the
        // old/new value)
        final HasLastModified referencedObject =
            getReferencedCollectionObject(a);
        // Based on invoking the collection method based on the field name
        final Collection collection = getCollection(a, containerObject);

        // If the action was to add to the collection, remove it
        if (a.getOldValue() == null && a.getNewValue() != null) {
          collection.remove(referencedObject);
        }

        // If the action was to remove from the collection, remove it
        else if (a.getNewValue() == null && a.getOldValue() != null) {
          collection.add(referencedObject);
        }

        // otherwise fail
        else {
          throw new Exception("Unexpected combination of old/new values - "
              + a.getOldValue() + ", " + a.getNewValue());
        }

        // Update the container object
        updateHasLastModified(containerObject);

      }

      //
      // Undo a field change
      //
      else if (isChangeAction(a)) {

        // Get the object that was modified, and make sure it still exists
        final HasLastModified referencedObject = getReferencedObject(a);

        // Get the get/set methods for the field
        final Method getMethod = getColumnGetMethod(a);
        final Method setMethod = getColumnSetMethod(a);

        if (getMethod == null) {
          throw new Exception(
              "Unable to find get method for field " + a.getField());
        }
        if (setMethod == null) {
          throw new Exception(
              "Unable to find set method for field " + a.getField());
        }

        // Check to make sure the field is still in the state it was set to in
        // the action
        // TODO: need a "force" mode to override these kinds of checks (for
        // other action types too)
        final Object origValue = getMethod.invoke(referencedObject);
        if (!origValue.toString().equals(a.getNewValue().toString())) {
          throw new Exception("Error: field " + a.getField() + " in "
              + referencedObject + " no longer has value: " + a.getNewValue());
        }

        // If all is well, set the field back to the previous value
        final Object setObject =
            getObjectForValue(getMethod.getReturnType(), a.getOldValue());
        setMethod.invoke(referencedObject, setObject);
        updateHasLastModified(referencedObject);

      }

    }

    // Set the molecular action undone flag
    undoMolecularAction.setUndoneFlag(true);
    this.updateMolecularAction(undoMolecularAction);

    // log the REST call
    addLogEntry(getUserName(), getProject().getId(),
        undoMolecularAction.getComponentId(),
        getMolecularAction().getActivityId(), getMolecularAction().getWorkId(),
        "UNDO " + undoMolecularAction.getName() + ", " + molecularActionId);

  }

}