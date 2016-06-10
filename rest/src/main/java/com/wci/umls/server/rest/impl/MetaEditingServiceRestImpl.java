/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.wci.umls.server.Project;
import com.wci.umls.server.UserRole;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.content.SemanticTypeComponentJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.jpa.services.ProjectServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.rest.ContentServiceRest;
import com.wci.umls.server.jpa.services.rest.MetaEditingServiceRest;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.meta.SemanticType;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.services.ContentService;
import com.wci.umls.server.services.ProjectService;
import com.wci.umls.server.services.SecurityService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * REST implementation for {@link ContentServiceRest}..
 */
@Path("/meta")
@Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@Api(value = "/meta", description = "Operations for metathesaurus editing")
public class MetaEditingServiceRestImpl extends RootServiceRestImpl
    implements MetaEditingServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link MetaEditingServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public MetaEditingServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /* see superclass */
  @Override
  @POST
  @Path("/sty/add")
  @ApiOperation(value = "Add semantic type to concept", notes = "Add semantic type to concept on a project branch")
  public ValidationResult addSemanticType(
    @ApiParam(value = "Project id, e.g. 1", required = true) @QueryParam("projectId") Long projectId,
    @ApiParam(value = "Concept id, e.g. 2", required = true) @QueryParam("conceptId") Long conceptId,
    @ApiParam(value = "Concept timestamp, as date", required = true) @QueryParam("timestamp") Long timestamp,
    @ApiParam(value = "Semantic type to add", required = true) SemanticTypeComponentJpa semanticTypeComponent,
    @ApiParam(value = "Override warnings", required = false) @QueryParam("overrideWarnings") boolean overrideWarnings,
    @ApiParam(value = "Authorization token, e.g. 'author'", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {
    {

      Logger.getLogger(getClass())
          .info("RESTful POST call (MetaEditing): /sty/" + projectId + "/"
              + conceptId + "/add for user " + authToken + " with sty value "
              + semanticTypeComponent.getSemanticType());

      String action = "trying to add semantic type to concept";

      ValidationResult validationResult = new ValidationResultJpa();

      ContentService contentService = new ContentServiceJpa();
      ProjectService projectService = new ProjectServiceJpa();

      try {

        // authorize and get user name from the token
        String userName = authorizeProject(projectService, projectId,
            securityService, authToken, action, UserRole.AUTHOR);

        Concept concept;

        // prepare the transaction
        contentService.setTransactionPerOperation(false);
        contentService.beginTransaction();

        //
        // Synchronized retrieval and locking based on conceptId
        // Intended for use to prevent access by other MetaEditing calls
        //
        // TODO Make sure to test different concept ids (should not block)
        synchronized (conceptId.toString().intern()) {

          // retrieve the concept
          concept = contentService.getConcept(conceptId);

          // lock the concept via Hibernate, secondary protection
          if (contentService.isObjectLocked(concept)) {
            throw new Exception(
                "Fatal error: Attempted to access locked object in synchronization block");
          }
          contentService.lockObject(concept);
        }

        // retrieve the project
        Project project = projectService.getProject(projectId);

        //
        // Check prerequisites
        //

        // perform action-specific validation
        // NOTE: No validation required for addSemanticType

        // check project and concept compatibility
        checkPrerequisitesForProjectAndConcept(project, concept,
            validationResult);

        // check for stale-state
        if (concept.getTimestamp().getTime() != timestamp) {
          validationResult.getErrors()
              .add("Stale state detected: concept modified after retrieval");
        }

        // check that semantic type is valid
        // TODO This will be replaced by metadata handler methods (i.e. getSemanticType(terminology, version, expandedForm)
        boolean semanticTypeValid = false;
        for (SemanticType s : contentService
            .getSemanticTypes(concept.getTerminology(), concept.getVersion())
            .getObjects()) {
          if (s.getExpandedForm().equals(semanticTypeComponent.getSemanticType())) {
            semanticTypeValid = true;
            break;
          }
        }
        if (!semanticTypeValid) {
          validationResult.getErrors()
              .add("Invalid semantic type: semantic type does not exist");
        }

        // check if semantic type already exists
        for (SemanticTypeComponent s : concept.getSemanticTypes()) {
          if (s.getSemanticType()
              .equals(semanticTypeComponent.getSemanticType())) {
            validationResult.getErrors()
                .add("Concept already contains semantic type");
          }
        }

        // if prerequisites fail, return validation result
        if (!validationResult.getErrors().isEmpty()
            || (!validationResult.getWarnings().isEmpty()
                && !overrideWarnings)) {
          // rollback -- unlocks the concept and closes transaction
          contentService.rollback();
          return validationResult;
        }

        //
        // Perform the action
        //

        // add the semantic type component itself and set the last modified
        semanticTypeComponent.setLastModifiedBy(userName);
        semanticTypeComponent.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
        semanticTypeComponent = (SemanticTypeComponentJpa) contentService
            .addSemanticTypeComponent(semanticTypeComponent, concept);

        // add the semantic type and set the last modified by
        concept.getSemanticTypes().add(semanticTypeComponent);
        concept.setLastModifiedBy(userName);
        concept.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);

        // update the concept
        contentService.updateConcept(concept);

        // commit (also removes the lock)
        contentService.commit();

        // TODO Add Action and LogEntry objects here once action CRUD servics
        // available

        return validationResult;

        // end synchronization block

      } catch (Exception e) {
        handleException(e, action);
        return null;
      } finally {
        contentService.close();
        projectService.close();
        securityService.close();
      }
    }

  }

  @Override
  @POST
  @Path("/sty/remove/{id}")
  @ApiOperation(value = "Remove semantic type from concept", notes = "Remove semantic type from concept on a project branch")
  public ValidationResult removeSemanticType(
    @ApiParam(value = "Project id, e.g. 1", required = true) @QueryParam("projectId") Long projectId,
    @ApiParam(value = "Concept id, e.g. 2", required = true) @QueryParam("conceptId") Long conceptId,
    @ApiParam(value = "Concept timestamp, in ms ", required = true) @QueryParam("timestamp") Long timestamp,
    @ApiParam(value = "Semantic type id, e.g. 3", required = true) @PathParam("id") Long semanticTypeComponentId,
    @ApiParam(value = "Override warnings", required = false) @QueryParam("overrideWarnings") boolean overrideWarnings,
    @ApiParam(value = "Authorization token, e.g. 'author'", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful POST call (MetaEditing): /sty/" + projectId + "/"
            + conceptId + "/remove for user " + authToken + " with id "
            + semanticTypeComponentId);

    String action = "trying to add semantic type to concept";

    ValidationResult validationResult = new ValidationResultJpa();

    ContentService contentService = new ContentServiceJpa();
    ProjectService projectService = new ProjectServiceJpa();

    try {

      // authorize and get user name from the token
      String userName = authorizeProject(projectService, projectId,
          securityService, authToken, action, UserRole.AUTHOR);

      Concept concept;

      // prepare the transaction
      contentService.setTransactionPerOperation(false);
      contentService.beginTransaction();

      //
      // Synchronized retrieval and locking based on conceptId
      // Intended for use to prevent access by other MetaEditing calls
      //
      synchronized (conceptId.toString().intern()) {

        // retrieve the concept
        concept = contentService.getConcept(conceptId);

        // lock the concept via Hibernate, secondary protection
        if (contentService.isObjectLocked(concept)) {
          throw new Exception("Fatal error: concept is locked");
        }
        contentService.lockObject(concept);

      }

      // retrieve the project
      Project project = projectService.getProject(projectId);

      //
      // Check prerequisites
      //

      // perform action-specific validation
      // NOTE: No validation required for addSemanticType

      // check project and concept compatibility
      checkPrerequisitesForProjectAndConcept(project, concept,
          validationResult);

      // check for stale-state
      if (concept.getTimestamp().getTime() < timestamp) {
        validationResult.getErrors()
            .add("Stale state detected: concept modified after retrieval");
      }

      // check that semantic type component exists on concept
      SemanticTypeComponent semanticTypeComponent = null;
      for (final SemanticTypeComponent sty : concept.getSemanticTypes()) {
        if (sty.getId().equals(semanticTypeComponentId)) {
          semanticTypeComponent = sty;
        }
      }
      if (semanticTypeComponent == null) {
        validationResult.getErrors().add(
            "Semantic type could not be removed from concept, not present");
      }
      // if prerequisites fail, return validation result
      if (!validationResult.getErrors().isEmpty()) {
        contentService.rollback();
        return validationResult;
      }

      //
      // Perform the action
      //

      // remove the semantic type component from the concept and update
      concept.getSemanticTypes().remove(semanticTypeComponent);
      concept.setLastModifiedBy(userName);
      contentService.updateConcept(concept);

      // remove the semantic type component
      contentService.removeSemanticTypeComponent(semanticTypeComponent.getId());

      // commit (also removes the lock)
      contentService.commit();

      // TODO Add Action and LogEntry objects here once action CRUD services
      // available

      return validationResult;

    } catch (Exception e) {
      handleException(e, action);
      return null;
    } finally {
      contentService.close();
      projectService.close();
      securityService.close();
    }
  }

  /**
   * Validate project and concept.
   *
   * @param project the project
   * @param concept the concept
   * @param validationResult the validation result
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private void checkPrerequisitesForProjectAndConcept(Project project,
    Concept concept, ValidationResult validationResult) throws Exception {

    // throw exception on terminology mismatch
    if (!concept.getTerminology().equals(project.getTerminology())) {
      validationResult.getErrors()
          .add("Project and concept terminologies do not match");
    }

    // throw exception on branch mismatch
    if (!concept.getBranch().equals(project.getBranch())) {
      validationResult.getErrors()
          .add("Project and concept branches do not match");
    }
  }
}
