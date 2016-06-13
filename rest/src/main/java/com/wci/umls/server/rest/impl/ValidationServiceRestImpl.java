/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import javax.websocket.server.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.wci.umls.server.Project;
import com.wci.umls.server.UserRole;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.KeyValuePairList;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.content.AtomJpa;
import com.wci.umls.server.jpa.content.CodeJpa;
import com.wci.umls.server.jpa.content.ConceptJpa;
import com.wci.umls.server.jpa.content.DescriptorJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.jpa.services.ProjectServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.rest.ProjectServiceRest;
import com.wci.umls.server.jpa.services.rest.ValidationServiceRest;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.services.ContentService;
import com.wci.umls.server.services.ProjectService;
import com.wci.umls.server.services.SecurityService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * REST implementation for {@link ProjectServiceRest}.
 */
@Path("/validation")
@Api(value = "/validation", description = "Operations providing terminology validation")
@Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class ValidationServiceRestImpl extends RootServiceRestImpl implements
    ValidationServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link ProjectServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public ValidationServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /* see superclass */
  @Override
  @GET
  @Path("/validate/concept/merge/{terminology}/{version}/{cui1}/{cui2}")
  @ApiOperation(value = "Validate merge", notes = "Validates the merge of two concepts", response = ValidationResultJpa.class)
  public ValidationResult validateMerge(
    @ApiParam(value = "The project id (optional), e.g. 1", required = false) @QueryParam("projectId") Long projectId,
    @ApiParam(value = "Terminology", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Version", required = true) @PathParam("version") String version,
    @ApiParam(value = "Cui for first concept", required = true) @PathParam("cui1") String cui1,
    @ApiParam(value = "Cui for second concept", required = true) @PathParam("cui2") String cui2,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Validation): /validate/concept/merge/" + terminology
            + "/" + version + "/" + cui1 + "/" + cui2);

    ProjectService projectService = new ProjectServiceJpa();
    ContentService contentService = new ContentServiceJpa();
    try {

      // authorize call
      authorizeProject(projectService, projectId, securityService, authToken,
          "merge concepts", UserRole.USER);
      Project project = projectService.getProject(projectId);

      Concept concept1 =
          contentService.getConcept(cui1, terminology, version, Branch.ROOT);
      Concept concept2 =
          contentService.getConcept(cui2, terminology, version, Branch.ROOT);

      ValidationResult result =
          projectService.validateMerge(project, concept1, concept2);

      return result;

    } catch (Exception e) {

      handleException(e, "trying to validate the concept merge");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  @Override
  @PUT
  @Path("/dui")
  @ApiOperation(value = "Validate Descriptor", notes = "Validates a descriptor", response = ValidationResultJpa.class)
  public ValidationResult validateDescriptor(
    @ApiParam(value = "The project id (optional), e.g. 1", required = false) @QueryParam("projectId") Long projectId,
    @ApiParam(value = "Descriptor", required = true) DescriptorJpa descriptor,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call PUT (Project): /dui " + descriptor);

    ProjectService projectService = new ProjectServiceJpa();
    try {
      Project project = projectService.getProject(projectId);
      authorizeProject(projectService, projectId, securityService, authToken,
          "validate descriptor", UserRole.USER);
      return projectService.validateDescriptor(project, descriptor);
    } catch (Exception e) {
      handleException(e, "trying to validate descriptor");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @PUT
  @Path("/aui")
  @ApiOperation(value = "Validate Atom", notes = "Validates a atom", response = ValidationResultJpa.class)
  public ValidationResult validateAtom(
    @ApiParam(value = "The project id (optional), e.g. 1", required = false) @QueryParam("projectId") Long projectId,
    @ApiParam(value = "Atom", required = true) AtomJpa atom,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call PUT (Project): /aui " + atom);

    ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeProject(projectService, projectId, securityService, authToken,
          "validate atom", UserRole.USER);
      Project project = projectService.getProject(projectId);
      return projectService.validateAtom(project, atom);
    } catch (Exception e) {
      handleException(e, "trying to validate atom");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @PUT
  @Path("/code")
  @ApiOperation(value = "Validate Code", notes = "Validates a code", response = ValidationResultJpa.class)
  public ValidationResult validateCode(
    @ApiParam(value = "The project id (optional), e.g. 1", required = false) @QueryParam("projectId") Long projectId,
    @ApiParam(value = "Code", required = true) CodeJpa code,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call PUT (Project): /code " + code);

    ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeProject(projectService, projectId, securityService, authToken,
          "validate code", UserRole.USER);
      Project project = projectService.getProject(projectId);
      return projectService.validateCode(project, code);
    } catch (Exception e) {
      handleException(e, "trying to validate code");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @PUT
  @Path("/concept")
  @ApiOperation(value = "Validate Concept", notes = "Validates a concept", response = ValidationResultJpa.class)
  public ValidationResult validateConcept(
    @ApiParam(value = "The project id (optional), e.g. 1", required = false) @QueryParam("projectId") Long projectId,
    @ApiParam(value = "Concept", required = true) ConceptJpa concept,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call PUT (Project): /concept " + concept);

    ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeProject(projectService, projectId, securityService, authToken,
          "validate conceptm", UserRole.USER);
      Project project = projectService.getProject(projectId);
      return projectService.validateConcept(project, concept);
    } catch (Exception e) {
      handleException(e, "trying to validate concept");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @GET
  @Path("/checks")
  @ApiOperation(value = "Gets all validation checks for a project", notes = "Gets all validation checks for a project", response = KeyValuePairList.class)
  public KeyValuePairList getValidationChecks(
    @ApiParam(value = "The project id , e.g. 1", required = true) @QueryParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call POST (Validation): /checks ");

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get validation checks",
          UserRole.VIEWER);

      Project project = projectService.getProject(projectId);
      final KeyValuePairList list =
          projectService.getValidationCheckNames(project);
      return list;
    } catch (Exception e) {
      handleException(e, "trying to validate all concept");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }
}
