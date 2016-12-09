/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.CancelException;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.helpers.KeyValuePair;
import com.wci.umls.server.jpa.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractMergeAlgorithm;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomClass;
import com.wci.umls.server.model.content.Component;
import com.wci.umls.server.services.handlers.ComputePreferredNameHandler;

/**
 * Implementation of an algorithm to import attributes.
 */
public class PrecomputedMergeAlgorithm extends AbstractMergeAlgorithm {

  /** The merge set. */
  private String mergeSet;

  /** The check names. */
  private List<String> checkNames;

  /**
   * Instantiates an empty {@link PrecomputedMergeAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public PrecomputedMergeAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("PRECOMPUTEDMERGE");
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
      throw new Exception("Precomputed Merge requires a project to be set");
    }

    // Check the input directories

    final String srcFullPath =
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
    logInfo("Starting PRECOMPUTEDMERGE");

    // Molecular actions WILL be generated by this algorithm
    setMolecularActionFlag(true);

    // Set up the search handler
    final ComputePreferredNameHandler prefNameHandler =
        getComputePreferredNameHandler(getProject().getTerminology());

    // Set up a stats map to be passed into the merge function later
    final Map<String, Integer> statsMap = new HashMap<>();
    statsMap.put("successfulMerges", 0);
    statsMap.put("unsuccessfulMerges", 0);
    statsMap.put("successfulDemotions", 0);
    statsMap.put("unsuccessfulDemotions", 0);

    try {

      logInfo("[PrecomputedMerge] Performing precomputed merges");

      //
      // Load the mergefacts.src file
      //
      List<String> lines =
          loadFileIntoStringList(getSrcDirFile(), "mergefacts.src", null, null);

      // Set the number of steps to the number of lines to be processed
      setSteps(lines.size());

      String fields[] = new String[12];

      for (String line : lines) {

        // Check for a cancelled call once every 100 lines
        if (getStepsCompleted() % 100 == 0 && isCancelled()) {
          throw new CancelException("Cancelled");
        }

        FieldedStringTokenizer.split(line, "|", 12, fields);

        // Fields:
        // 0 id_1
        // 1 merge_level
        // 2 id_2
        // 3 source
        // 4 integrity_vector
        // 5 make_demotion
        // 6 change_status
        // 7 merge_set
        // 8 id_type_1
        // 9 id_qualifier_1
        // 10 id_type_2
        // 11 id_qualifier_2

        // e.g.
        // 362249700|SY|362281363|NCI_2016_05E||Y|N|NCI-SY|SRC_ATOM_ID||SRC_ATOM_ID||

        // If this lines mergeSet doesn't match the specified mergeSet, skip.
        if (!fields[7].equals(mergeSet)) {
          updateProgress();
          continue;
        }

        // Load the two atoms specified by the mergefacts line, or the preferred
        // name atoms if they are containing component
        Component component = getComponent(fields[8], fields[0],
            getCachedTerminologyName(fields[9]), null);
        if (component == null) {
          logWarnAndUpdate(line, "Warning - could not find Component for type: "
              + fields[8] + ", terminologyId: " + fields[0]);
          continue;
        }
        Atom atom = null;
        if (component instanceof Atom) {
          atom = (Atom) component;
        } else if (component instanceof AtomClass) {
          AtomClass atomClass = (AtomClass) component;
          List<Atom> atoms =
              prefNameHandler.sortAtoms(atomClass.getAtoms(), getPrecedenceList(
                  getProject().getTerminology(), getProject().getVersion()));
          atom = atoms.get(0);
        } else {
          logWarnAndUpdate(line, "Warning - " + component.getClass().getName()
              + " is an unhandled type.");
          continue;
        }

        Component component2 = getComponent(fields[10], fields[2],
            getCachedTerminologyName(fields[11]), null);
        if (component2 == null) {
          logWarnAndUpdate(line, "Warning - could not find Component for type: "
              + fields[10] + ", terminologyId: " + fields[2]);
          continue;
        }
        Atom atom2 = null;
        if (component2 instanceof Atom) {
          atom2 = (Atom) component2;
        } else if (component2 instanceof AtomClass) {
          AtomClass atomClass = (AtomClass) component2;
          List<Atom> atoms =
              prefNameHandler.sortAtoms(atomClass.getAtoms(), getPrecedenceList(
                  getProject().getTerminology(), getProject().getVersion()));
          atom2 = atoms.get(0);
        } else {
          logWarnAndUpdate(line, "Warning - " + component2.getClass().getName()
              + " is an unhandled type.");
          continue;
        }

        // Attempt to perform the merge
        merge(atom.getId(), atom2.getId(), checkNames,
            fields[5].toUpperCase().equals("Y"),
            fields[6].toUpperCase().equals("Y"), getProject(), statsMap);

        // Update the progress
        updateProgress();

      }

      commitClearBegin();

      logInfo("[PrecomputedMerge] " + getSteps()
          + " mergefacts.src lines processed.");
      logInfo("[PrecomputedMerge] " + statsMap.get("successfulMerges")
          + " merges successfully performed.");
      logInfo("[PrecomputedMerge] " + statsMap.get("unsuccessfulMerges")
          + " attempted merges were unsuccessful.");
      logInfo("[GeneratedMerge] " + statsMap.get("successfulDemotions")
          + " demotions successfully created.");
      logInfo("[GeneratedMerge] " + statsMap.get("unsuccessfulDemotions")
          + " attempted demotion creations were unsuccessful.");

      logInfo("  project = " + getProject().getId());
      logInfo("  workId = " + getWorkId());
      logInfo("  activityId = " + getActivityId());
      logInfo("  user  = " + getLastModifiedBy());
      logInfo("Finished PRECOMPUTEDMERGE");

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
    // n/a - No reset
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    checkRequiredProperties(new String[] {
        "mergeSet"
    }, p);
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {

    if (p.getProperty("mergeSet") != null) {
      mergeSet = String.valueOf(p.getProperty("mergeSet"));
    }
    if (p.getProperty("checkNames") != null) {
      checkNames =
          Arrays.asList(String.valueOf(p.getProperty("checkNames")).split(";"));
    }

  }

  /**
   * Returns the parameters.
   *
   * @return the parameters
   */
  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() {
    final List<AlgorithmParameter> params = super.getParameters();

    // Run checkPreconditions to set the SrcDirFile, since it will be used by
    // the merge
    // set parameter
    try {
      checkPreconditions();
    } catch (Exception e) {
      // Do nothing
    }

    AlgorithmParameter param = new AlgorithmParameterJpa("Merge Set",
        "mergeSet", "The merge set to perform the merges on", "e.g. NCI-SY", 10,
        AlgorithmParameter.Type.MULTI, "");
    // Look for the mergefacts.src file and populate the enum based on the
    // merge_set column.
    List<String> mergeSets = getMergeSets(getSrcDirFile());

    // If the file isn't found, or the file contains no mergeSets, set the
    // parameter to a free-entry string
    if (mergeSets == null || mergeSets.size() == 0) {
      param.setType(AlgorithmParameter.Type.STRING);
    } else {
      param.setPossibleValues(mergeSets);
    }
    params.add(param);

    param = new AlgorithmParameterJpa("Integrity Checks", "checkNames",
        "The names of the integrity checks to run", "e.g. MGV_B", 10,
        AlgorithmParameter.Type.ENUM, "");

    List<String> validationChecks = new ArrayList<>();
    for (final KeyValuePair validationCheck : getValidationCheckNames()
        .getKeyValuePairs()) {
      // Add handler Name to ENUM list
      validationChecks.add(validationCheck.getKey());
    }

    param.setPossibleValues(validationChecks);
    params.add(param);

    return params;
  }

  @Override
  public String getDescription() {
    return "Loads and performs merges based on mergefacts.src.";
  }
}