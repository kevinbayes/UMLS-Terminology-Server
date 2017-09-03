/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.jpa.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractMergeAlgorithm;
import com.wci.umls.server.jpa.content.AtomJpa;
import com.wci.umls.server.jpa.content.AtomRelationshipJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomRelationship;
import com.wci.umls.server.model.meta.TermType;
import com.wci.umls.server.model.meta.Terminology;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/**
 * Implementation of an algorithm to remove demotions from atoms matching
 * criteria.
 */
public class SafeReplaceAlgorithm extends AbstractMergeAlgorithm {

  /** The string class id flag. */
  private Boolean stringClassId = null;

  /** The lexical class id flag. */
  private Boolean lexicalClassId = null;

  /** The code id flag. */
  private Boolean codeId = null;

  /** The concept id flag. */
  private Boolean conceptId = null;

  /** The descriptor id flag. */
  private Boolean descriptorId = null;

  /** The term type flag. */
  private Boolean termType = null;

  /** The terminology. */
  private String terminology = null;

  /**
   * Instantiates an empty {@link SafeReplaceAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public SafeReplaceAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("SAFEREPLACE");
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
      throw new Exception("Safe Replace requires a project to be set");
    }

    if (!(codeId || conceptId || descriptorId || lexicalClassId || stringClassId
        || termType)) {
      throw new Exception(
          "No match-criteria are selected (e.g. code Id, concept Id, etc.).");
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
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());
    logInfo("  code id = " + codeId);
    logInfo("  concept id = " + conceptId);
    logInfo("  descriptor id = " + descriptorId);
    logInfo("  lexical class id = " + lexicalClassId);
    logInfo("  string class id = " + stringClassId);
    logInfo("  terminology = " + terminology);
    logInfo("  term type = " + termType);

    // Not Molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    // Find all atoms pairs that satisfy the specified matching criteria and are
    // in the same project Terminology concept, where one atom is old and the
    // other is new

    // Generate query string
    String query = "SELECT DISTINCT a1.id, a2.id "
        + "FROM ConceptJpa c JOIN c.atoms a1 JOIN c.atoms a2 "
        + "WHERE NOT a1.id = a2.id "
        + "AND c.terminology=:projectTerminology AND c.version=:projectVersion "
        + "AND a1.terminology=:terminology AND NOT a1.version=:version "
        + "AND a1.publishable=true "
        + "AND a2.terminology=:terminology AND a2.version=:version "
        + "AND a2.publishable=true "
        + (stringClassId ? "AND a1.stringClassId = a2.stringClassId " : "")
        + (lexicalClassId ? "AND a1.lexicalClassId = a2.lexicalClassId " : "")
        + (conceptId ? "AND a1.conceptId = a2.conceptId " : "")
        + (codeId ? "AND a1.codeId = a2.codeId " : "")
        + (descriptorId ? "AND a1.descriptorId = a2.descriptorId " : "")
        + (termType ? "AND a1.termType = a2.termType " : "");

    // If terminology is not set, run the query for ALL terminologies referenced
    // in sources.src, and add all results to atomIdPairArray
    // If terminology is set, run the query once for that terminology.

    List<Long[]> atomIdPairArray = new ArrayList<>();

    if (ConfigUtility.isEmpty(terminology)) {

      // Get all terminologies referenced in the sources.src file
      // terminologies.left = Terminology
      // terminolgoies.right = Version
      Set<Terminology> terminologies = new HashSet<>();
      terminologies = getReferencedTerminologies();

      for (final Terminology terminology : terminologies) {
        // Generate parameters to pass into query executions
        Map<String, String> params = new HashMap<>();
        params.put("terminology", terminology.getTerminology());
        params.put("version", terminology.getVersion());
        params.put("projectTerminology", getProject().getTerminology());
        params.put("projectVersion", getProject().getVersion());

        atomIdPairArray.addAll(executeComponentIdPairQuery(query,
            QueryType.JPQL, params, AtomJpa.class, false));
      }
    } else {

      Terminology currentTerminology = getCurrentTerminology(terminology);
      // Generate parameters to pass into query executions
      Map<String, String> params = new HashMap<>();
      params.put("terminology", currentTerminology.getTerminology());
      params.put("version", currentTerminology.getVersion());
      params.put("projectTerminology", getProject().getTerminology());
      params.put("projectVersion", getProject().getVersion());

      atomIdPairArray.addAll(executeComponentIdPairQuery(query, QueryType.JPQL,
          params, AtomJpa.class, false));
    }

    setSteps(atomIdPairArray.size());

    // Recast the array as a Pair, for easier comparison
    final List<Pair<Long, Long>> atomIdPairs = new ArrayList<>();
    for (Long[] atomidPair : atomIdPairArray) {
      atomIdPairs
          .add(new ImmutablePair<Long, Long>(atomidPair[0], atomidPair[1]));
    }

    // Sort pairs by MergeLevel, AtomId1, and AtomId2
    sortPairsByMergeLevelAndId(atomIdPairs);

    logInfo("  Performing safe replacements");
    commitClearBegin();

    // Track which new atoms have been safe-replaced:
    // Each new atom can only be safe-replaced by a single old-atom.
    Set<Long> safeReplacedAtomIds = new HashSet<>();

    for (Pair<Long, Long> atomIdPair : atomIdPairs) {
      checkCancel();

      final Long oldAtomId = atomIdPair.getLeft();
      final Long newAtomId = atomIdPair.getRight();

      // If this newAtom has already had a safe-replacement performed on it,
      // skip
      if (safeReplacedAtomIds.contains(newAtomId)) {
        continue;
      } else {
        safeReplacedAtomIds.add(newAtomId);
      }

      // Borrow information from the old atom and assign to the new atom
      final Atom oldAtom = getAtom(oldAtomId);
      final Atom newAtom = getAtom(newAtomId);

      // Update newAtom's alternateTerminologyIds
      for (final Map.Entry<String, String> oldAltTermId : oldAtom
          .getAlternateTerminologyIds().entrySet()) {
        newAtom.getAlternateTerminologyIds().put(oldAltTermId.getKey(),
            oldAltTermId.getValue());
      }

      // Update obsolete and suppresible.
      // If old atom was suppresed by an editor and new atom is unsuppressed,
      // set new atom to suppresible
      TermType oldAtomTty = getCachedTermType(oldAtom.getTermType());
      TermType newAtomTty = getCachedTermType(newAtom.getTermType());
      if (oldAtom.isSuppressible() && !newAtom.isSuppressible()
          && !oldAtomTty.isSuppressible() && !newAtomTty.isSuppressible()) {
        newAtom.setSuppressible(true);
      }

      if (!oldAtom.getWorkflowStatus().equals(newAtom.getWorkflowStatus())
          && !newAtom.getWorkflowStatus().equals(WorkflowStatus.DEMOTION)) {
        newAtom.setWorkflowStatus(oldAtom.getWorkflowStatus());
      }

      newAtom.setLastPublishedRank(oldAtom.getLastPublishedRank());

      // Remove all demotions from the new Atom, and the inverses
      final Set<Long> removeRelationshipIds = new HashSet<>();

      for (AtomRelationship rel : new ArrayList<>(newAtom.getRelationships())) {
        if (rel.getWorkflowStatus().equals(WorkflowStatus.DEMOTION)) {
          // Remove the demotion from the atom
          newAtom.getRelationships().remove(rel);

          // Remove the inverse relationship from the toAtom
          Atom relatedAtom = getAtom(rel.getTo().getId());
          AtomRelationship inverseDemotion =
              (AtomRelationship) getInverseRelationship(
                  getProject().getTerminology(), getProject().getVersion(),
                  rel);
          relatedAtom.getRelationships().remove(inverseDemotion);

          // Update the related atom
          updateAtom(relatedAtom);

          // Save the demotion and inverseDemotion Ids, to delete later
          removeRelationshipIds.add(rel.getId());
          removeRelationshipIds.add(inverseDemotion.getId());
        }
      }

      // Once all demotions are removed, update the newAtom
      updateAtom(newAtom);

      // Delete any demotions that were removed from atoms
      for (Long relId : removeRelationshipIds) {
        removeRelationship(relId, AtomRelationshipJpa.class);
      }

      // Log it
      addLogEntry(getLastModifiedBy(), getProject().getId(), newAtomId,
          getActivityId(), getWorkId(), "Performed safe replace on new Atom: "
              + newAtomId + " using old Atom: " + oldAtomId);

      // Update the progress
      updateProgress();
    }

    commitClearBegin();

    logInfo("  new atoms safe-replaced = " + safeReplacedAtomIds.size());
    logInfo("Finished " + getName());

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
    if (p.getProperty("stringClassId") == null
        && p.getProperty("lexicalClassId") == null
        && p.getProperty("codeId") == null && p.getProperty("conceptId") == null
        && p.getProperty("descriptorId") == null
        && p.getProperty("termType") == null) {
      throw new LocalException(
          "No match-criteria are selected (e.g. code Id, concept Id, etc.).");
    }
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    if (p.getProperty("stringClassId") != null) {
      stringClassId = Boolean.parseBoolean(p.getProperty("stringClassId"));
    }
    if (p.getProperty("lexicalClassId") != null) {
      lexicalClassId = Boolean.parseBoolean(p.getProperty("lexicalClassId"));
    }
    if (p.getProperty("codeId") != null) {
      codeId = Boolean.parseBoolean(p.getProperty("codeId"));
    }
    if (p.getProperty("conceptId") != null) {
      conceptId = Boolean.parseBoolean(p.getProperty("conceptId"));
    }
    if (p.getProperty("descriptorId") != null) {
      descriptorId = Boolean.parseBoolean(p.getProperty("descriptorId"));
    }
    if (p.getProperty("termType") != null) {
      termType = Boolean.parseBoolean(p.getProperty("termType"));
    }
    if (p.getProperty("terminology") != null) {
      terminology = String.valueOf(p.getProperty("terminology"));
    }

  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();

    AlgorithmParameter param = new AlgorithmParameterJpa("String Class Id",
        "stringClassId", "Match atoms by String Class Id?", "e.g. true", 5,
        AlgorithmParameter.Type.BOOLEAN, "false");
    params.add(param);

    param = new AlgorithmParameterJpa("Lexical Class Id", "lexicalClassId",
        "Match atoms by Lexical Class Id?", "e.g. true", 5,
        AlgorithmParameter.Type.BOOLEAN, "false");
    params.add(param);

    param = new AlgorithmParameterJpa("Code Id", "codeId",
        "Match atoms by Code Id?", "e.g. true", 5,
        AlgorithmParameter.Type.BOOLEAN, "false");
    params.add(param);

    param = new AlgorithmParameterJpa("Concept Id", "conceptId",
        "Match atoms by Concept Id?", "e.g. true", 5,
        AlgorithmParameter.Type.BOOLEAN, "false");
    params.add(param);

    param = new AlgorithmParameterJpa("Descriptor Id", "descriptorId",
        "Match atoms by Descriptor Id?", "e.g. true", 5,
        AlgorithmParameter.Type.BOOLEAN, "false");
    params.add(param);

    param = new AlgorithmParameterJpa("Term Type", "termType",
        "Match atoms by Term Type?", "e.g. true", 5,
        AlgorithmParameter.Type.BOOLEAN, "false");
    params.add(param);

    param = new AlgorithmParameterJpa("Terminology", "terminology",
        "Terminology to run safe replacement on (if left blank, will run on all terminologies referenced in sources.src",
        "e.g. NCI", 5, AlgorithmParameter.Type.STRING, "");
    params.add(param);

    return params;
  }

  @Override
  public String getDescription() {
    return "Performs criteria-based safe replacement algorithm.";
  }
}