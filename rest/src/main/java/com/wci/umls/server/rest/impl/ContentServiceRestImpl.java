/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.wci.umls.server.ReleaseInfo;
import com.wci.umls.server.UserRole;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.SearchResultList;
import com.wci.umls.server.helpers.content.CodeList;
import com.wci.umls.server.helpers.content.ConceptList;
import com.wci.umls.server.helpers.content.DescriptorList;
import com.wci.umls.server.helpers.content.SubsetMemberList;
import com.wci.umls.server.jpa.algo.ClamlLoaderAlgorithm;
import com.wci.umls.server.jpa.algo.LuceneReindexAlgorithm;
import com.wci.umls.server.jpa.algo.Rf2DeltaLoaderAlgorithm;
import com.wci.umls.server.jpa.algo.Rf2FileSorter;
import com.wci.umls.server.jpa.algo.Rf2Readers;
import com.wci.umls.server.jpa.algo.Rf2SnapshotLoaderAlgorithm;
import com.wci.umls.server.jpa.algo.RrfFileSorter;
import com.wci.umls.server.jpa.algo.RrfLoaderAlgorithm;
import com.wci.umls.server.jpa.algo.RrfReaders;
import com.wci.umls.server.jpa.algo.TransitiveClosureAlgorithm;
import com.wci.umls.server.jpa.algo.TreePositionAlgorithm;
import com.wci.umls.server.jpa.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.jpa.services.HistoryServiceJpa;
import com.wci.umls.server.jpa.services.MetadataServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.helper.TerminologyUtility;
import com.wci.umls.server.jpa.services.rest.ContentServiceRest;
import com.wci.umls.server.model.content.Code;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.Descriptor;
import com.wci.umls.server.model.content.LexicalClass;
import com.wci.umls.server.model.content.StringClass;
import com.wci.umls.server.model.meta.Terminology;
import com.wci.umls.server.services.ContentService;
import com.wci.umls.server.services.HistoryService;
import com.wci.umls.server.services.MetadataService;
import com.wci.umls.server.services.SecurityService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * REST implementation for {@link ContentServiceRest}..
 */
@Path("/content")
@Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@Api(value = "/content", description = "Operations to retrieve RF2 content for a terminology.")
public class ContentServiceRestImpl extends RootServiceRestImpl implements
    ContentServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link ContentServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public ContentServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.services.rest.ContentServiceRest#luceneReindex(
   * java.lang.String, java.lang.String)
   */
  @Override
  @POST
  @Path("/reindex")
  @ApiOperation(value = "Reindexes specified objects", notes = "Recomputes lucene indexes for the specified comma-separated objects")
  public void luceneReindex(
    @ApiParam(value = "Comma-separated list of objects to reindex, e.g. ConceptJpa (optional)", required = false) String indexedObjects,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)

  throws Exception {
    Logger.getLogger(getClass()).info("test");
    Logger.getLogger(getClass()).info(
        "RESTful POST call (ContentChange): /reindex "
            + (indexedObjects == null ? "with no objects specified"
                : "with specified objects " + indexedObjects));

    // Track system level information
    long startTimeOrig = System.nanoTime();
    LuceneReindexAlgorithm algo = new LuceneReindexAlgorithm();
    try {
      authenticate(securityService, authToken, "reindex",
          UserRole.ADMINISTRATOR);
      algo.setIndexedObjects(indexedObjects);
      algo.compute();
      algo.close();
      // Final logging messages
      Logger.getLogger(getClass()).info(
          "      elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));
      Logger.getLogger(getClass()).info("done ...");

    } catch (Exception e) {
      handleException(e, "trying to reindex");
    } finally {
      algo.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#
   * computeTransitiveClosure(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/terminology/closure/compute/{terminology}/{version}")
  @ApiOperation(value = "Computes terminology transitive closure", notes = "Computes transitive closure for the latest version of the specified terminology")
  public void computeTransitiveClosure(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology, e.g. SNOMEDCT", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)

  throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful POST call (ContentChange): /terminology/closure/compute/"
            + terminology + "/" + version);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    TransitiveClosureAlgorithm algo = new TransitiveClosureAlgorithm();
    MetadataService service = new MetadataServiceJpa();
    try {
      authenticate(securityService, authToken, "compute transitive closure",
          UserRole.ADMINISTRATOR);

      // Compute transitive closure
      Logger.getLogger(getClass()).info(
          "  Compute transitive closure for  " + terminology + "/" + version);
      algo.setTerminology(terminology);
      algo.setTerminologyVersion(version);
      algo.setIdType(service.getTerminology(terminology, version)
          .getOrganizingClassType());
      algo.reset();
      algo.compute();

      // Final logging messages
      Logger.getLogger(getClass()).info(
          "      elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));
      Logger.getLogger(getClass()).info("done ...");

    } catch (Exception e) {
      handleException(e, "trying to compute transitive closure");
    } finally {
      algo.close();
      service.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.services.rest.ContentServiceRest#computeTreePositions
   * (java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @POST
  @Path("/terminology/treepos/compute/{terminology}/{version}")
  @ApiOperation(value = "Computes terminology tree positions", notes = "Computes tree positions for the latest version of the specified terminology")
  public void computeTreePositions(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology, e.g. SNOMEDCT", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)

  throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful POST call (ContentChange): /terminology/treepos/compute/"
            + terminology + "/" + version);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    TreePositionAlgorithm algo = new TreePositionAlgorithm();
    MetadataService service = new MetadataServiceJpa();
    try {
      authenticate(securityService, authToken, "compute tree positions ",
          UserRole.ADMINISTRATOR);

      // Compute tree positions
      Logger.getLogger(getClass()).info(
          "  Compute tree positions for " + terminology + "/" + version);
      algo.setTerminology(terminology);
      algo.setTerminologyVersion(version);
      algo.setIdType(service.getTerminology(terminology, version)
          .getOrganizingClassType());
      algo.reset();
      algo.compute();

      // Final logging messages
      Logger.getLogger(getClass()).info(
          "      elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));
      Logger.getLogger(getClass()).info("done ...");

    } catch (Exception e) {
      handleException(e, "trying to compute tree positions");
    } finally {
      algo.close();
      service.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.services.rest.ContentServiceRest#loadTerminologyRrf
   * (java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @PUT
  @Path("/terminology/load/rrf/{singleMode}/{terminology}/{version}")
  @Consumes({
    MediaType.TEXT_PLAIN
  })
  @ApiOperation(value = "Load all terminologies from an RRF directory", notes = "Loads terminologies from an RRF directory for specified terminology and version")
  public void loadTerminologyRrf(
    @ApiParam(value = "Terminology, e.g. UMLS", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 2014AB", required = true) @PathParam("version") String version,
    @ApiParam(value = "Single mode, e.g. false", required = true) @PathParam("singleMode") boolean singleMode,
    @ApiParam(value = "RRF input directory", required = true) String inputDir,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info(
            "RESTful POST call (ContentChange): /terminology/load/rrf/umls/"
                + terminology + "/" + version + " from input directory "
                + inputDir);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    try {
      authenticate(securityService, authToken, "load RRF",
          UserRole.ADMINISTRATOR);

      // Check the input directory
      File inputDirFile = new File(inputDir);
      if (!inputDirFile.exists()) {
        throw new Exception("Specified input directory does not exist");
      }

      // Sort files - not really needed because files are already sorted
      Logger.getLogger(getClass()).info("  Sort RRF Files");
      RrfFileSorter sorter = new RrfFileSorter();
      sorter.setRequireAllFiles(true);
      // File outputDir = new File(inputDirFile, "/RRF-sorted-temp/");
      // sorter.sortFiles(inputDirFile, outputDir);
      String releaseVersion = sorter.getFileVersion(inputDirFile);
      Logger.getLogger(getClass()).info("  releaseVersion = " + releaseVersion);

      // Open readers - just open original RRF
      RrfReaders readers = new RrfReaders(inputDirFile);
      readers.openOriginalReaders();

      // Load snapshot
      RrfLoaderAlgorithm algorithm = new RrfLoaderAlgorithm();
      algorithm.setTerminology(terminology);
      algorithm.setTerminologyVersion(version);
      algorithm.setSingleMode(singleMode);
      algorithm.setReleaseVersion(releaseVersion);
      algorithm.setReaders(readers);
      algorithm.compute();
      algorithm.close();
      algorithm = null;

      // Compute transitive closure
      // Obtain each terminology and run transitive closure on it with the
      // correct id type
      MetadataService metadataService = new MetadataServiceJpa();
      // Refresh caches after metadata has changed in loader
      metadataService.refreshCaches();
      for (Terminology t : metadataService.getTerminologyLatestVersions()
          .getObjects()) {
        // Only compute for organizing class types
        if (t.getOrganizingClassType() != null) {
          TransitiveClosureAlgorithm algo = new TransitiveClosureAlgorithm();
          algo.setTerminology(t.getTerminology());
          algo.setTerminologyVersion(t.getTerminologyVersion());
          algo.setIdType(t.getOrganizingClassType());
          // some terminologies may have cycles, allow these for now.
          algo.setCycleTolerant(true);
          algo.compute();
          algo.close();
        }
      }

      // Compute tree positions
      // Refresh caches after metadata has changed in loader
      for (Terminology t : metadataService.getTerminologyLatestVersions()
          .getObjects()) {
        // Only compute for organizing class types
        if (t.getOrganizingClassType() != null) {
          TreePositionAlgorithm algo = new TreePositionAlgorithm();
          algo.setTerminology(t.getTerminology());
          algo.setTerminologyVersion(t.getTerminologyVersion());
          algo.setIdType(t.getOrganizingClassType());
          // some terminologies may have cycles, allow these for now.
          algo.setCycleTolerant(true);
          algo.compute();
          algo.close();
        }
      }

      // Clean-up
      // readers.closeReaders();
      metadataService.close();
      ConfigUtility
          .deleteDirectory(new File(inputDirFile, "/RRF-sorted-temp/"));

      // Final logging messages
      Logger.getLogger(getClass()).info(
          "      elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));
      Logger.getLogger(getClass()).info("done ...");

    } catch (Exception e) {
      handleException(e, "trying to load terminology from RRF directory");
    } finally {
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.services.rest.ContentServiceRest#removeTerminology
   * (java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/terminology/remove/{terminology}/{version}")
  @ApiOperation(value = "Removes a terminology", notes = "Removes all elements for a specified terminology and version")
  public void removeTerminology(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 20140731", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful POST call (ContentChange): /terminology/remove/" + terminology
            + "/" + version);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    MetadataService metadataService = new MetadataServiceJpa();
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "start editing cycle",
          UserRole.ADMINISTRATOR);

      metadataService.clearMetadata(terminology, version);
      contentService.clearConcepts(terminology, version);

      // Final logging messages
      Logger.getLogger(getClass()).info(
          "      elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));
      Logger.getLogger(getClass()).info("done ...");

    } catch (Exception e) {
      handleException(e, "trying to load terminology from ClaML file");
    } finally {
      metadataService.close();
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.services.rest.ContentServiceRest#getConcept(java
   * .lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/cui/{terminology}/{version}/{terminologyId}")
  @ApiOperation(value = "Get concept by id, terminology, and version", notes = "Get the root branch concept matching the specified parameters.", response = Concept.class)
  public Concept getConcept(
    @ApiParam(value = "Concept terminology id, e.g. 102751005", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. latest", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /cui/" + terminology + "/" + version + "/"
            + terminologyId);
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "retrieve the concept",
          UserRole.VIEWER);

      Concept concept =
          contentService.getConcept(terminologyId, terminology, version,
              Branch.ROOT);

      if (concept != null) {
        contentService.getGraphResolutionHandler(terminology).resolve(
            concept,
            TerminologyUtility.getHierarchicalIsaRels(concept.getTerminology(),
                concept.getTerminologyVersion()));
        concept.setAtoms(contentService.getComputePreferredNameHandler(
            concept.getTerminology()).sortByPreference(concept.getAtoms()));
      }
      return concept;
    } catch (Exception e) {
      handleException(e, "trying to retrieve a concept");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.services.rest.ContentServiceRest#findConceptsForQuery
   * (java.lang.String, java.lang.String, java.lang.String,
   * com.wci.umls.server.jpa.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/cui/{terminology}/{version}/query/{query}")
  @ApiOperation(value = "Find concepts matching a search query.", notes = "Gets a list of search results that match the lucene query for the root branch.", response = SearchResultList.class)
  public SearchResultList findConceptsForQuery(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "Query, e.g. 'sulfur'", required = true) @PathParam("query") String query,
    @ApiParam(value = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'", required = false) PfsParameterJpa pfs,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /cui/" + terminology + "/" + version
            + "/query/" + query + " with PFS parameter "
            + (pfs == null ? "empty" : pfs.toString()));
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "find concepts by query",
          UserRole.VIEWER);

      SearchResultList sr =
          contentService.findConceptsForQuery(terminology, version,
              Branch.ROOT, query, pfs);
      return sr;

    } catch (Exception e) {
      handleException(e, "trying to find the concepts by query");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.services.rest.ContentServiceRest#getDescriptor(
   * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/dui/{terminology}/{version}/{terminologyId}")
  @ApiOperation(value = "Get descriptor by id, terminology, and version", notes = "Get the root branch descriptor matching the specified parameters.", response = Descriptor.class)
  public Descriptor getDescriptor(
    @ApiParam(value = "Descriptor terminology id, e.g. D003933", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Descriptor terminology name, e.g. MSH", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Descriptor terminology version, e.g. 2015_2014_09_08", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /dui/" + terminology + "/" + version + "/"
            + terminologyId);
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "retrieve the descriptor",
          UserRole.VIEWER);

      Descriptor descriptor =
          contentService.getDescriptor(terminologyId, terminology, version,
              Branch.ROOT);

      if (descriptor != null) {
        contentService.getGraphResolutionHandler(terminology)
            .resolve(
                descriptor,
                TerminologyUtility.getHierarchicalIsaRels(
                    descriptor.getTerminology(),
                    descriptor.getTerminologyVersion()));
        descriptor.setAtoms(contentService.getComputePreferredNameHandler(
            descriptor.getTerminology())
            .sortByPreference(descriptor.getAtoms()));

      }
      return descriptor;
    } catch (Exception e) {
      handleException(e, "trying to retrieve a descriptor");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#
   * findDescriptorsForQuery(java.lang.String, java.lang.String,
   * java.lang.String, com.wci.umls.server.jpa.helpers.PfsParameterJpa,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/dui/{terminology}/{version}/query/{query}")
  @ApiOperation(value = "Find descriptors matching a search query.", notes = "Gets a list of search results that match the lucene query for the root branch.", response = SearchResultList.class)
  public SearchResultList findDescriptorsForQuery(
    @ApiParam(value = "Descriptor terminology name, e.g. MSH", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Descriptor terminology version, e.g. 2015_2014_09_08", required = true) @PathParam("version") String version,
    @ApiParam(value = "Query, e.g. 'sulfur'", required = true) @PathParam("query") String query,
    @ApiParam(value = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'", required = false) PfsParameterJpa pfs,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /dui/" + terminology + "/" + version
            + "/query/" + query + " with PFS parameter "
            + (pfs == null ? "empty" : pfs.toString()));
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "find descriptors by query",
          UserRole.VIEWER);

      SearchResultList sr =
          contentService.findDescriptorsForQuery(terminology, version,
              Branch.ROOT, query, pfs);
      return sr;

    } catch (Exception e) {
      handleException(e, "trying to find the descriptors by query");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.services.rest.ContentServiceRest#getCode(java.lang
   * .String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/code/{terminology}/{version}/{terminologyId}")
  @ApiOperation(value = "Get code by id, terminology, and version", notes = "Get the root branch code matching the specified parameters.", response = Code.class)
  public Code getCode(
    @ApiParam(value = "Code terminology id, e.g. U002135", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Code terminology name, e.g. MTH", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Code terminology version, e.g. 2014AB", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /code/" + terminology + "/" + version + "/"
            + terminologyId);
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "retrieve the code",
          UserRole.VIEWER);

      Code code =
          contentService.getCode(terminologyId, terminology, version,
              Branch.ROOT);

      if (code != null) {
        contentService.getGraphResolutionHandler(terminology).resolve(
            code,
            TerminologyUtility.getHierarchicalIsaRels(code.getTerminology(),
                code.getTerminologyVersion()));
        code.setAtoms(contentService.getComputePreferredNameHandler(
            code.getTerminology()).sortByPreference(code.getAtoms()));

      }
      return code;
    } catch (Exception e) {
      handleException(e, "trying to retrieve a code");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.services.rest.ContentServiceRest#findCodesForQuery
   * (java.lang.String, java.lang.String, java.lang.String,
   * com.wci.umls.server.jpa.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/code/{terminology}/{version}/query/{query}")
  @ApiOperation(value = "Find codes matching a search query.", notes = "Gets a list of search results that match the lucene query for the root branch.", response = SearchResultList.class)
  public SearchResultList findCodesForQuery(
    @ApiParam(value = "Code terminology name, e.g. MTH", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Code terminology version, e.g. 2014AB", required = true) @PathParam("version") String version,
    @ApiParam(value = "Query, e.g. 'sulfur'", required = true) @PathParam("query") String query,
    @ApiParam(value = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'", required = false) PfsParameterJpa pfs,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /code/" + terminology + "/" + version
            + "/query/" + query + " with PFS parameter "
            + (pfs == null ? "empty" : pfs.toString()));
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "find codes by query",
          UserRole.VIEWER);

      SearchResultList sr =
          contentService.findCodesForQuery(terminology, version, Branch.ROOT,
              query, pfs);
      return sr;

    } catch (Exception e) {
      handleException(e, "trying to find the codes by query");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.services.rest.ContentServiceRest#getLexicalClass
   * (java.lang .String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/lui/{terminology}/{version}/{terminologyId}")
  @ApiOperation(value = "Get lexical class by id, terminology, and version", notes = "Get the root branch lexical class matching the specified parameters.", response = LexicalClass.class)
  public LexicalClass getLexicalClass(
    @ApiParam(value = "Lexical class terminology id, e.g. L0356926", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Lexical class terminology name, e.g. UMLS", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Lexical class terminology version, e.g. latest", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /lui/" + terminology + "/" + version + "/"
            + terminologyId);
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "retrieve the lexical class",
          UserRole.VIEWER);

      LexicalClass lexicalClass =
          contentService.getLexicalClass(terminologyId, terminology, version,
              Branch.ROOT);

      if (lexicalClass != null) {
        contentService.getGraphResolutionHandler(terminology).resolve(
            lexicalClass);
        lexicalClass.setAtoms(contentService.getComputePreferredNameHandler(
            lexicalClass.getTerminology()).sortByPreference(
            lexicalClass.getAtoms()));

      }
      return lexicalClass;
    } catch (Exception e) {
      handleException(e, "trying to retrieve a lexicalClass");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#
   * findLexicalClasssForQuery (java.lang.String, java.lang.String,
   * java.lang.String, com.wci.umls.server.jpa.helpers.PfsParameterJpa,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/lui/{terminology}/{version}/query/{query}")
  @ApiOperation(value = "Find lexical class matching a search query.", notes = "Gets a list of search results that match the lucene query for the root branch.", response = SearchResultList.class)
  public SearchResultList findLexicalClassesForQuery(
    @ApiParam(value = "Lexical class terminology name, e.g. UMLS", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Lexical class terminology version, e.g. latest", required = true) @PathParam("version") String version,
    @ApiParam(value = "Query, e.g. 'sulfur'", required = true) @PathParam("query") String query,
    @ApiParam(value = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'", required = false) PfsParameterJpa pfs,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /lui/" + terminology + "/" + version
            + "/query/" + query + " with PFS parameter "
            + (pfs == null ? "empty" : pfs.toString()));
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "find lexical class by query",
          UserRole.VIEWER);

      SearchResultList sr =
          contentService.findLexicalClassesForQuery(terminology, version,
              Branch.ROOT, query, pfs);
      return sr;

    } catch (Exception e) {
      handleException(e, "trying to find the lexicalClasses by query");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.services.rest.ContentServiceRest#getStringClass
   * (java.lang .String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/sui/{terminology}/{version}/{terminologyId}")
  @ApiOperation(value = "Get string class by id, terminology, and version", notes = "Get the root branch string class matching the specified parameters.", response = StringClass.class)
  public StringClass getStringClass(
    @ApiParam(value = "String class terminology id, e.g. L0356926", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "String class terminology name, e.g. UMLS", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "String class terminology version, e.g. latest", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /sui/" + terminology + "/" + version + "/"
            + terminologyId);
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "retrieve the string class",
          UserRole.VIEWER);

      StringClass stringClass =
          contentService.getStringClass(terminologyId, terminology, version,
              Branch.ROOT);

      if (stringClass != null) {
        contentService.getGraphResolutionHandler(terminology).resolve(
            stringClass);
        stringClass.setAtoms(contentService.getComputePreferredNameHandler(
            stringClass.getTerminology()).sortByPreference(
            stringClass.getAtoms()));
      }
      return stringClass;
    } catch (Exception e) {
      handleException(e, "trying to retrieve a stringClass");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#
   * findStringClasssForQuery (java.lang.String, java.lang.String,
   * java.lang.String, com.wci.umls.server.jpa.helpers.PfsParameterJpa,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/sui/{terminology}/{version}/query/{query}")
  @ApiOperation(value = "Find string class matching a search query.", notes = "Gets a list of search results that match the lucene query for the root branch.", response = SearchResultList.class)
  public SearchResultList findStringClassesForQuery(
    @ApiParam(value = "String class terminology name, e.g. UMLS", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "String class terminology version, e.g. latest", required = true) @PathParam("version") String version,
    @ApiParam(value = "Query, e.g. 'sulfur'", required = true) @PathParam("query") String query,
    @ApiParam(value = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'", required = false) PfsParameterJpa pfs,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /sui/" + terminology + "/" + version
            + "/query/" + query + " with PFS parameter "
            + (pfs == null ? "empty" : pfs.toString()));
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "find string class by query",
          UserRole.VIEWER);

      SearchResultList sr =
          contentService.findStringClassesForQuery(terminology, version,
              Branch.ROOT, query, pfs);
      return sr;

    } catch (Exception e) {
      handleException(e, "trying to find the stringClasses by query");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.services.rest.ContentServiceRest#findAncestorConcepts
   * (java.lang.String, java.lang.String, java.lang.String, boolean,
   * com.wci.umls.server.jpa.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/cui/{terminology}/{version}/{terminologyId}/ancestors")
  @ApiOperation(value = "Find ancestor concepts.", notes = "Gets a list of ancestor concepts.", response = ConceptList.class)
  public ConceptList findAncestorConcepts(
    @ApiParam(value = "Concept terminology id, e.g. 102751005", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "Children only flag, e.g. true", required = true) @PathParam("childrenOnly") boolean childrenOnly,
    @ApiParam(value = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'", required = false) PfsParameterJpa pfs,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /cui/" + terminology + "/" + version
            + terminologyId + " with PFS parameter "
            + (pfs == null ? "empty" : pfs.toString()));
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "find ancestor concepts",
          UserRole.VIEWER);

      Concept concept =
          contentService.getConcept(terminologyId, terminology, version,
              Branch.ROOT);
      return contentService.findAncestorConcepts(concept, childrenOnly, pfs,
          Branch.ROOT);

    } catch (Exception e) {
      handleException(e, "trying to find the ancestor concepts");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#findDescendantConcepts(java.lang.String, java.lang.String, java.lang.String, boolean, com.wci.umls.server.jpa.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/cui/{terminology}/{version}/{terminologyId}/descendants")
  @ApiOperation(value = "Find descendant concepts.", notes = "Gets a list of descendant concepts.", response = ConceptList.class)
  public ConceptList findDescendantConcepts(
    @ApiParam(value = "Concept terminology id, e.g. 102751005", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "Children only flag, e.g. true", required = true) @PathParam("childrenOnly") boolean childrenOnly,
    @ApiParam(value = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'", required = false) PfsParameterJpa pfs,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /cui/" + terminology + "/" + version
            + terminologyId + " with PFS parameter "
            + (pfs == null ? "empty" : pfs.toString()));
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "find descendant concepts",
          UserRole.VIEWER);

      Concept concept =
          contentService.getConcept(terminologyId, terminology, version,
              Branch.ROOT);
      return contentService.findDescendantConcepts(concept, childrenOnly, pfs,
          Branch.ROOT);

    } catch (Exception e) {
      handleException(e, "trying to find the descendant concepts");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#findAncestorDescriptors(java.lang.String, java.lang.String, java.lang.String, boolean, com.wci.umls.server.jpa.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/dui/{terminology}/{version}/{terminologyId}/ancestors")
  @ApiOperation(value = "Find ancestor descriptors.", notes = "Gets a list of ancestor descriptors.", response = DescriptorList.class)
  public DescriptorList findAncestorDescriptors(
    @ApiParam(value = "Descriptor terminology id, e.g. 102751005", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "Children only flag, e.g. true", required = true) @PathParam("childrenOnly") boolean childrenOnly,
    @ApiParam(value = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'", required = false) PfsParameterJpa pfs,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /dui/" + terminology + "/" + version
            + terminologyId + " with PFS parameter "
            + (pfs == null ? "empty" : pfs.toString()));
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "find ancestor descriptors",
          UserRole.VIEWER);

      Descriptor descriptor =
          contentService.getDescriptor(terminologyId, terminology, version,
              Branch.ROOT);
      return contentService.findAncestorDescriptors(descriptor, childrenOnly,
          pfs, Branch.ROOT);

    } catch (Exception e) {
      handleException(e, "trying to find the ancestor descriptors");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#findDescendantDescriptors(java.lang.String, java.lang.String, java.lang.String, boolean, com.wci.umls.server.jpa.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/dui/{terminology}/{version}/{terminologyId}/descendants")
  @ApiOperation(value = "Find descendant descriptors.", notes = "Gets a list of descendant descriptors.", response = DescriptorList.class)
  public DescriptorList findDescendantDescriptors(
    @ApiParam(value = "Descriptor terminology id, e.g. 102751005", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "Children only flag, e.g. true", required = true) @PathParam("childrenOnly") boolean childrenOnly,
    @ApiParam(value = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'", required = false) PfsParameterJpa pfs,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /dui/" + terminology + "/" + version
            + terminologyId + " with PFS parameter "
            + (pfs == null ? "empty" : pfs.toString()));
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "find descendant descriptors",
          UserRole.VIEWER);

      Descriptor descriptor =
          contentService.getDescriptor(terminologyId, terminology, version,
              Branch.ROOT);
      return contentService.findDescendantDescriptors(descriptor, childrenOnly,
          pfs, Branch.ROOT);

    } catch (Exception e) {
      handleException(e, "trying to find the descendant descriptors");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#findAncestorCodes(java.lang.String, java.lang.String, java.lang.String, boolean, com.wci.umls.server.jpa.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/code/{terminology}/{version}/{terminologyId}/ancestors")
  @ApiOperation(value = "Find ancestor codes.", notes = "Gets a list of ancestor codes.", response = CodeList.class)
  public CodeList findAncestorCodes(
    @ApiParam(value = "Code terminology id, e.g. 102751005", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "Children only flag, e.g. true", required = true) @PathParam("childrenOnly") boolean childrenOnly,
    @ApiParam(value = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'", required = false) PfsParameterJpa pfs,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /code/" + terminology + "/" + version
            + terminologyId + " with PFS parameter "
            + (pfs == null ? "empty" : pfs.toString()));
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "find ancestor codes",
          UserRole.VIEWER);

      Code code =
          contentService.getCode(terminologyId, terminology, version,
              Branch.ROOT);
      return contentService.findAncestorCodes(code, childrenOnly, pfs,
          Branch.ROOT);

    } catch (Exception e) {
      handleException(e, "trying to find the ancestor codes");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#findDescendantCodes(java.lang.String, java.lang.String, java.lang.String, boolean, com.wci.umls.server.jpa.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/code/{terminology}/{version}/{terminologyId}/descendants")
  @ApiOperation(value = "Find descendant codes.", notes = "Gets a list of descendant codes.", response = CodeList.class)
  public CodeList findDescendantCodes(
    @ApiParam(value = "Code terminology id, e.g. 102751005", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "Children only flag, e.g. true", required = true) @PathParam("childrenOnly") boolean childrenOnly,
    @ApiParam(value = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'", required = false) PfsParameterJpa pfs,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /code/" + terminology + "/" + version
            + terminologyId + " with PFS parameter "
            + (pfs == null ? "empty" : pfs.toString()));
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken, "find descendant codes",
          UserRole.VIEWER);

      Code code =
          contentService.getCode(terminologyId, terminology, version,
              Branch.ROOT);
      return contentService.findDescendantCodes(code, childrenOnly, pfs,
          Branch.ROOT);

    } catch (Exception e) {
      handleException(e, "trying to find the descendant codes");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#
   * getSubsetMembersForConcept(java.lang.String, java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/csm/{terminology}/{version}/{conceptId}")
  @ApiOperation(value = "Get subset members with this conceptId", notes = "Get the subset members with the given concept id.", response = SubsetMemberList.class)
  public SubsetMemberList getSubsetMembersForConcept(
    @ApiParam(value = "Concept terminology id, e.g. 102751005", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. latest", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /csm/" + terminology + "/" + version + "/"
            + conceptId);
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken,
          "retrieve subset members for the concept", UserRole.VIEWER);

      return contentService.getSubsetMembersForConcept(conceptId, terminology,
          version, Branch.ROOT);

    } catch (Exception e) {
      handleException(e, "trying to retrieve subset members for a concept");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#
   * getSubsetMembersForAtom(java.lang.String, java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/asm/{terminology}/{version}/{atomId}")
  @ApiOperation(value = "Get subset members with this atomId", notes = "Get the subset members with the given atom id.", response = SubsetMemberList.class)
  public SubsetMemberList getSubsetMembersForAtom(
    @ApiParam(value = "Atom terminology id, e.g. 102751005", required = true) @PathParam("atomId") String atomId,
    @ApiParam(value = "Atom terminology name, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Atom terminology version, e.g. latest", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /asm/" + terminology + "/" + version + "/"
            + atomId);
    ContentService contentService = new ContentServiceJpa();
    try {
      authenticate(securityService, authToken,
          "retrieve subset members for the atom", UserRole.VIEWER);

      return contentService.getSubsetMembersForAtom(atomId, terminology,
          version, Branch.ROOT);

    } catch (Exception e) {
      handleException(e, "trying to retrieve subset members for a atom");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }
  

  /* (non-Javadoc)
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#loadTerminologyRf2Delta(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @PUT
  @Path("/terminology/load/rf2/delta/{terminology}")
  @Consumes({
    MediaType.TEXT_PLAIN
  })
  @ApiOperation(value = "Loads terminology RF2 delta from directory", notes = "Loads terminology RF2 delta from directory for specified terminology and version")
  public void loadTerminologyRf2Delta(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "RF2 input directory", required = true) String inputDir,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful POST call (ContentChange): /terminology/load/rf2/delta/"
            + terminology + " from input directory " + inputDir);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    try {
      authenticate(securityService, authToken, "start editing cycle",
          UserRole.ADMINISTRATOR);

      Logger.getLogger(getClass()).info("Starting RF2 delta loader");
      Logger.getLogger(getClass()).info("  terminology = " + terminology);
      Logger.getLogger(getClass()).info("  inputDir = " + inputDir);

      // Check the input directory
      File inputDirFile = new File(inputDir);
      if (!inputDirFile.exists()) {
        throw new Exception("Specified input directory does not exist");
      }

      // Previous computation of terminology version is based on file name
      // but for delta/daily build files, this is not the current version
      // look up the current version instead
      MetadataService metadataService = new MetadataServiceJpa();
      final String version = metadataService.getLatestVersion(terminology);
      metadataService.close();
      if (version == null) {
        throw new Exception("Unable to determine terminology version.");
      }

      // Sort files
      Logger.getLogger(getClass()).info("  Sort RF2 Files");
      Rf2FileSorter sorter = new Rf2FileSorter();
      sorter.setSortByEffectiveTime(false);
      sorter.setRequireAllFiles(false);
      File outputDir = new File(inputDirFile, "/RF2-sorted-temp/");
      sorter.sortFiles(inputDirFile, outputDir);

      // Open readers
      Rf2Readers readers = new Rf2Readers(outputDir);
      readers.openReaders();

      // Load delta
      Rf2DeltaLoaderAlgorithm algorithm = new Rf2DeltaLoaderAlgorithm();
      algorithm.setTerminology(terminology);
      algorithm.setTerminologyVersion(version);
      algorithm.setReleaseVersion(sorter.getFileVersion());
      algorithm.setReaders(readers);
      algorithm.compute();
      algorithm.close();

      // Compute transitive closure
      Logger.getLogger(getClass()).info(
          "  Compute transitive closure from  " + terminology + "/" + version);
      TransitiveClosureAlgorithm algo = new TransitiveClosureAlgorithm();
      algo.setTerminology(terminology);
      algo.setTerminologyVersion(version);
      algo.reset();
      algo.compute();

      // Clean-up
      readers.closeReaders();
      Logger.getLogger(getClass()).info("...done");

      // Final logging messages
      Logger.getLogger(getClass()).info(
          "      elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));
      Logger.getLogger(getClass()).info("done ...");

    } catch (Exception e) {
      handleException(e, "trying to load terminology delta from RF2 directory");
    } finally {
      securityService.close();
    }
  }

  
  /* (non-Javadoc)
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#loadTerminologyRf2Full(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @SuppressWarnings("resource")
  @Override
  @PUT
  @Path("/terminology/load/rf2/full/{terminology}/{version}")
  @Consumes({
    MediaType.TEXT_PLAIN
  })
  @ApiOperation(value = "Loads terminology RF2 full from directory", notes = "Loads terminology RF2 full from directory for specified terminology and version")
  public void loadTerminologyRf2Full(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 20140731", required = true) @PathParam("version") String version,
    @ApiParam(value = "RF2 input directory", required = true) String inputDir,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful POST call (ContentChange): /terminology/load/rf2/full/"
            + terminology + "/" + version + " from input file " + inputDir);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    try {
      authenticate(securityService, authToken, "start editing cycle",
          UserRole.ADMINISTRATOR);

      // Check the input directory
      File inputDirFile = new File(inputDir);
      if (!inputDirFile.exists()) {
        throw new Exception("Specified input directory does not exist");
      }

      // Get the release versions (need to look in complex map too for October
      // releases)
      Logger.getLogger(getClass()).info("  Get release versions");
      Rf2FileSorter sorter = new Rf2FileSorter();
      File conceptsFile =
          sorter.findFile(new File(inputDir, "Terminology"), "sct2_Concept");
      Set<String> releaseSet = new HashSet<>();
      BufferedReader reader = new BufferedReader(new FileReader(conceptsFile));
      String line;
      while ((line = reader.readLine()) != null) {
        final String fields[] = line.split("\t");
        if (!fields[1].equals("effectiveTime")) {
          try {
            ConfigUtility.DATE_FORMAT.parse(fields[1]);
          } catch (Exception e) {
            throw new Exception("Improperly formatted date found: " + fields[1]);
          }
          releaseSet.add(fields[1]);
        }
      }
      reader.close();
      File complexMapFile =
          sorter.findFile(new File(inputDir, "Refset/Map"),
              "der2_iissscRefset_ComplexMap");
      reader = new BufferedReader(new FileReader(complexMapFile));
      while ((line = reader.readLine()) != null) {
        final String fields[] = line.split("\t");
        if (!fields[1].equals("effectiveTime")) {
          try {
            ConfigUtility.DATE_FORMAT.parse(fields[1]);
          } catch (Exception e) {
            throw new Exception("Improperly formatted date found: " + fields[1]);
          }
          releaseSet.add(fields[1]);
        }
      }
      File extendedMapFile =
          sorter.findFile(new File(inputDir, "Refset/Map"),
              "der2_iisssccRefset_ExtendedMap");
      reader = new BufferedReader(new FileReader(extendedMapFile));
      while ((line = reader.readLine()) != null) {
        final String fields[] = line.split("\t");
        if (!fields[1].equals("effectiveTime")) {
          try {
            ConfigUtility.DATE_FORMAT.parse(fields[1]);
          } catch (Exception e) {
            throw new Exception("Improperly formatted date found: " + fields[1]);
          }
          releaseSet.add(fields[1]);
        }
      }

      reader.close();
      List<String> releases = new ArrayList<>(releaseSet);
      Collections.sort(releases);

      // check that release info does not already exist
      HistoryService historyService = new HistoryServiceJpa();
      Logger.getLogger(getClass()).info("  Releases to process");
      for (String release : releases) {
        Logger.getLogger(getClass()).info("    release = " + release);

        ReleaseInfo releaseInfo =
            historyService.getReleaseInfo(terminology, release);
        if (releaseInfo != null) {
          throw new Exception("A release info already exists for " + release);
        }
      }
      historyService.close();

      // Sort files
      Logger.getLogger(getClass()).info("  Sort RF2 Files");
      sorter = new Rf2FileSorter();
      sorter.setSortByEffectiveTime(true);
      sorter.setRequireAllFiles(true);
      File outputDir = new File(inputDirFile, "/RF2-sorted-temp/");
      sorter.sortFiles(inputDirFile, outputDir);

      // Open readers
      Rf2Readers readers = new Rf2Readers(outputDir);
      readers.openReaders();

      // Load initial snapshot - first release version
      Rf2SnapshotLoaderAlgorithm algorithm = new Rf2SnapshotLoaderAlgorithm();
      algorithm.setTerminology(terminology);
      algorithm.setTerminologyVersion(version);
      algorithm.setReleaseVersion(releases.get(0));
      algorithm.setReaders(readers);
      algorithm.compute();
      algorithm.close();
      algorithm = null;

      // Load deltas
      for (String release : releases) {
        if (release.equals(releases.get(0))) {
          continue;
        }

        Rf2DeltaLoaderAlgorithm algorithm2 = new Rf2DeltaLoaderAlgorithm();
        algorithm2.setTerminology(terminology);
        algorithm2.setTerminologyVersion(version);
        algorithm2.setReleaseVersion(release);
        algorithm2.setReaders(readers);
        algorithm2.compute();
        algorithm2.close();
        algorithm2 = null;

      }

      // Compute transitive closure
      Logger.getLogger(getClass()).info(
          "  Compute transitive closure from  " + terminology + "/" + version);
      TransitiveClosureAlgorithm algo = new TransitiveClosureAlgorithm();
      algo.setTerminology(terminology);
      algo.setTerminologyVersion(version);
      algo.reset();
      algo.compute();

      //
      // Individual release infos will already be created by
      // snapshot and delta processes, so it is not needed here
      //

      // Clean-up
      readers.closeReaders();
      ConfigUtility
          .deleteDirectory(new File(inputDirFile, "/RF2-sorted-temp/"));

      // Final logging messages
      Logger.getLogger(getClass()).info(
          "      elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));
      Logger.getLogger(getClass()).info("done ...");

    } catch (Exception e) {
      handleException(e, "trying to load full terminology from RF2 directory");
    } finally {
      securityService.close();
    }
  }

 
  /* (non-Javadoc)
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#loadTerminologyRf2Snapshot(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @PUT
  @Path("/terminology/load/rf2/snapshot/{terminology}/{version}")
  @Consumes({
    MediaType.TEXT_PLAIN
  })
  @ApiOperation(value = "Loads terminology RF2 snapshot from directory", notes = "Loads terminology RF2 snapshot from directory for specified terminology and version")
  public void loadTerminologyRf2Snapshot(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 20140731", required = true) @PathParam("version") String version,
    @ApiParam(value = "RF2 input directory", required = true) String inputDir,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info(
            "RESTful POST call (ContentChange): /terminology/load/rf2/snapshot/"
                + terminology + "/" + version + " from input directory "
                + inputDir);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    try {
      authenticate(securityService, authToken, "start editing cycle",
          UserRole.ADMINISTRATOR);

      // Check the input directory
      File inputDirFile = new File(inputDir);
      if (!inputDirFile.exists()) {
        throw new Exception("Specified input directory does not exist");
      }

      // Sort files
      Logger.getLogger(getClass()).info("  Sort RF2 Files");
      Rf2FileSorter sorter = new Rf2FileSorter();
      sorter.setSortByEffectiveTime(false);
      sorter.setRequireAllFiles(true);
      File outputDir = new File(inputDirFile, "/RF2-sorted-temp/");
      sorter.sortFiles(inputDirFile, outputDir);
      String releaseVersion = sorter.getFileVersion();
      Logger.getLogger(getClass()).info("  releaseVersion = " + releaseVersion);

      // Open readers
      Rf2Readers readers = new Rf2Readers(outputDir);
      readers.openReaders();

      // Load snapshot
      Rf2SnapshotLoaderAlgorithm algorithm = new Rf2SnapshotLoaderAlgorithm();
      algorithm.setTerminology(terminology);
      algorithm.setTerminologyVersion(version);
      algorithm.setReleaseVersion(releaseVersion);
      algorithm.setReaders(readers);
      algorithm.compute();
      algorithm.close();
      algorithm = null;

      // Compute transitive closure
      Logger.getLogger(getClass()).info(
          "  Compute transitive closure from  " + terminology + "/" + version);
      TransitiveClosureAlgorithm algo = new TransitiveClosureAlgorithm();
      algo.setTerminology(terminology);
      algo.setTerminologyVersion(version);
      algo.reset();
      algo.compute();

      // Clean-up
      readers.closeReaders();
      ConfigUtility
          .deleteDirectory(new File(inputDirFile, "/RF2-sorted-temp/"));

      // Final logging messages
      Logger.getLogger(getClass()).info(
          "      elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));
      Logger.getLogger(getClass()).info("done ...");

    } catch (Exception e) {
      handleException(e,
          "trying to load terminology snapshot from RF2 directory");
    } finally {
      securityService.close();
    }

  }

  /* (non-Javadoc)
   * @see com.wci.umls.server.jpa.services.rest.ContentServiceRest#loadTerminologyClaml(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @PUT
  @Path("/terminology/load/claml/{terminology}/{version}")
  @Consumes({
    MediaType.TEXT_PLAIN
  })
  @ApiOperation(value = "Loads ClaML terminology from file", notes = "Loads terminology from ClaML file, assigning specified version")
  public void loadTerminologyClaml(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 20140731", required = true) @PathParam("version") String version,
    @ApiParam(value = "ClaML input file", required = true) String inputFile,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful POST call (ContentChange): /terminology/load/claml/"
            + terminology + "/" + version + " from input file " + inputFile);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    ClamlLoaderAlgorithm clamlAlgorithm = new ClamlLoaderAlgorithm();
    TransitiveClosureAlgorithm transitiveClosureAlgorithm =
        new TransitiveClosureAlgorithm();
    try {
      authenticate(securityService, authToken, "start editing cycle",
          UserRole.ADMINISTRATOR);

      // Load snapshot
      Logger.getLogger(getClass()).info("Load ClaML data from " + inputFile);
      clamlAlgorithm.setTerminology(terminology);
      clamlAlgorithm.setTerminologyVersion(version);
      clamlAlgorithm.setInputFile(inputFile);
      clamlAlgorithm.compute();

      // Let service begin its own transaction
      Logger.getLogger(getClass()).info("Start computing transtive closure");
      transitiveClosureAlgorithm.setTerminology(terminology);
      transitiveClosureAlgorithm.setTerminologyVersion(version);
      transitiveClosureAlgorithm.reset();
      transitiveClosureAlgorithm.compute();

      // Final logging messages
      Logger.getLogger(getClass()).info(
          "      elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));
      Logger.getLogger(getClass()).info("done ...");

    } catch (Exception e) {
      handleException(e, "trying to load terminology from ClaML file");
    } finally {
      clamlAlgorithm.close();
      transitiveClosureAlgorithm.close();
      securityService.close();
    }
  }
}
