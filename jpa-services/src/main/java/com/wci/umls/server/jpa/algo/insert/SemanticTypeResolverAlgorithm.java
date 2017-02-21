/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.jpa.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;
import com.wci.umls.server.jpa.content.ConceptJpa;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.SemanticTypeComponent;

/**
 * Implementation of an algorithm to find all concepts with "new" and "old"
 * semantic type components, and remove either old or new depending on params..
 */
public class SemanticTypeResolverAlgorithm
    extends AbstractInsertMaintReleaseAlgorithm {

  /** Whether new semanticTypes 'win' and replace older ones, or vice versa. */
  private String winLose = null;

  /**
   * Instantiates an empty {@link SemanticTypeResolverAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public SemanticTypeResolverAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("SEMANTICTYPERESOLVER");
    setLastModifiedBy("admin");
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

    ValidationResult validationResult = new ValidationResultJpa();

    if (getProject() == null) {
      throw new Exception(
          "Semantic Type Resolving requires a project to be set");
    }
    if (winLose == null) {
      throw new Exception(
          "Semantic Type Resolving requires winLose to be set.");
    }
    if (!(winLose.equals("win") || winLose.equals("lose"))) {
      throw new Exception("winLose= " + winLose
          + " is invalid: must be either 'win' or 'lose'");
    }

    // Check the input directories
    String srcFullPath =
        ConfigUtility.getConfigProperties().getProperty("source.data.dir")
            + File.separator + getProcess().getInputPath();

    setSrcDirFile(new File(srcFullPath));
    if (!getSrcDirFile().exists()) {
      throw new Exception("Specified input directory does not exist");
    }

    return validationResult;
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());
    logInfo("  winLose = " + winLose);
    commitClearBegin();

    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    // Count number of removed Semantic Types, for logging
    int removedStyCount = 0;

    try {
      logInfo(
          "  Finding all concepts that contain both new and old Semantic Types.");
      commitClearBegin();

      // Find all concepts that contain old and new semantic type components

      Long maxStyPreInsertion = Long.parseLong(
          getProcess().getExecutionInfo().get("maxStyIdPreInsertion"));

      // Generate query string
      String query = "SELECT DISTINCT c.id "
          + "FROM ConceptJpa c JOIN c.semanticTypes s1 JOIN c.semanticTypes s2 "
          + "WHERE c.terminology=:projectTerminology AND c.version=:projectVersion "
          + "AND s1.id <= " + maxStyPreInsertion.toString() + " AND s2.id > "
          + maxStyPreInsertion.toString();

      // Generate parameters to pass into query executions
      Map<String, String> params = new HashMap<>();
      params.put("terminology", getTerminology());
      params.put("version", getVersion());
      params.put("projectTerminology", getProject().getTerminology());
      params.put("projectVersion", getProject().getVersion());

      List<Long> conceptIdArray = executeSingleComponentIdQuery(query,
          QueryType.JQL, params, ConceptJpa.class, false);

      setSteps(conceptIdArray.size());

      for (Long conceptId : conceptIdArray) {
        checkCancel();

        Concept concept = getConcept(conceptId);
        for (SemanticTypeComponent sty : new ArrayList<>(
            concept.getSemanticTypes())) {
          // If new semantic types set to "win", remove all old semantic types
          if (winLose.equals("win") && sty.getId() <= maxStyPreInsertion) {
            concept.getSemanticTypes().remove(sty);
            updateConcept(concept);
            removeSemanticTypeComponent(sty.getId());
            removedStyCount++;
          }
          // If new semantic types set to "lose", remove all new semantic types
          else if (winLose.equals("lose") && sty.getId() > maxStyPreInsertion) {
            concept.getSemanticTypes().remove(sty);
            updateConcept(concept);
            removeSemanticTypeComponent(sty.getId());
            removedStyCount++;
          }
        }

        // Update the progress
        updateProgress();
      }

      commitClearBegin();

      logInfo("  removed" + (winLose.equals("win") ? " old " : " new ")
          + "count = " + removedStyCount);

      // Produce the sty_term_ids file for inverters
      // Get all (project term & version) concepts that contain (process
      // term and version) atoms
      // Collect all atoms' (projectTerm + "-SRC") alternate terminology ids
      // Collect all Semantic Types from the concept
      // Print out atom.alternateTermId|SemanticType.getSemanticType|ClassesFlag
      // Default ClassesFlag to 0
      // Save file as sty_term_ids in process-folder

      logInfo("  Creating the sty_term_ids file.");

      // Generate query string
      query = "SELECT DISTINCT value(aid), s.semanticType "
          + "FROM ConceptJpa c JOIN c.atoms a join a.alternateTerminologyIds aid join c.semanticTypes s "
          + "WHERE c.terminology=:projectTerminology AND c.version=:projectVersion "
          + "AND a.terminology=:terminology AND a.version=:version "
          + "AND key(aid) = :projectTermSrcKey ";

      // Add projectTermSrcKey param
      params.put("projectTermSrcKey", getProject().getTerminology() + "-SRC");

      // Execute the query
      javax.persistence.Query jpaQuery = getEntityManager().createQuery(query);

      // Handle special query key-words
      if (params != null) {
        for (final String key : params.keySet()) {
          if (query.contains(":" + key)) {
            jpaQuery.setParameter(key, params.get(key));
          }
        }
      }
      Logger.getLogger(getClass()).info("  query = " + query);

      // Return the result list as a pair of strings.
      final List<Object[]> list = jpaQuery.getResultList();

      // Create the sty_terms_ids file, and write each result to it
      File outputFile = new File(getSrcDirFile(), "sty_term_ids");
      final PrintWriter out = new PrintWriter(new FileWriter(outputFile));

      for (final Object[] entry : list) {
        String alternateTermId = entry[0].toString();
        String semanticType = entry[1].toString();
        out.println(alternateTermId + "|" + semanticType + "|0");
      }

      out.close();

      logInfo("  sty_term_ids file = " + getSrcDirFile());

      logInfo("Finished " + getName());

    } catch (

    Exception e) {
      logError("Unexpected problem - " + e.getMessage());
      throw e;
    }

  }

  /**
   * Reset.
   *
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());
    // n/a - No reset
    logInfo("Finished RESET " + getName());
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    checkRequiredProperties(new String[] {
        "winLose"
    }, p);
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    winLose = String.valueOf(p.getProperty("winLose"));

  }

  /**
   * Returns the parameters.
   *
   * @return the parameters
   */
  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();
    AlgorithmParameter param = new AlgorithmParameterJpa("WinLose", "winLose",
        "Whether new SemanticTypes created during this insertion 'win' or 'lose' to old semantic types.",
        "e.g. win", 200, AlgorithmParameter.Type.ENUM, "");
    param.setPossibleValues(Arrays.asList("win", "lose"));
    params.add(param);

    return params;
  }

  @Override
  public String getDescription() {
    return "Resolves insertion semantic types against prior state.";
  }

}