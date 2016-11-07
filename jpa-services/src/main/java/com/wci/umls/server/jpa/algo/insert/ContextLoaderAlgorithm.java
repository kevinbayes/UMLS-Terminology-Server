/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.helpers.meta.TerminologyList;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractAlgorithm;
import com.wci.umls.server.jpa.algo.TransitiveClosureAlgorithm;
import com.wci.umls.server.jpa.algo.TreePositionAlgorithm;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.model.meta.IdType;
import com.wci.umls.server.model.meta.Terminology;
import com.wci.umls.server.services.RootService;
import com.wci.umls.server.services.handlers.IdentifierAssignmentHandler;

/**
 * Implementation of an algorithm to import contexts.
 */
public class ContextLoaderAlgorithm extends AbstractAlgorithm {

  /** The full directory where the src files are. */
  private File srcDirFile = null;

  /** The previous progress. */
  private int previousProgress;

  /** The steps. */
  private int steps;

  /** The steps completed. */
  private int stepsCompleted;

  /**
   * The loaded terminologies. Key = Terminology_Version (or just Terminology,
   * if Version = "latest") Value = Terminology object
   */
  private Map<String, Terminology> loadedTerminologies = new HashMap<>();

  /**
   * Instantiates an empty {@link ContextLoaderAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public ContextLoaderAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("CONTEXTLOADER");
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
      throw new Exception("Context Loading requires a project to be set");
    }

    // Check the input directories

    String srcFullPath =
        ConfigUtility.getConfigProperties().getProperty("source.data.dir")
            + File.separator + getProcess().getInputPath();

    srcDirFile = new File(srcFullPath);
    if (!srcDirFile.exists()) {
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
    logInfo("Starting CONTEXTLOADING");

    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    // Set up the handler for identifier assignment
    final IdentifierAssignmentHandler handler =
        newIdentifierAssignmentHandler(getProject().getTerminology());
    handler.setTransactionPerOperation(false);
    handler.beginTransaction();

    // Count number of added and updated Contexts, for logging
    int addCount = 0;
    int updateCount = 0;

    try {

      previousProgress = 0;
      stepsCompleted = 0;

      logInfo("[ContextLoader] Checking for new/updated Contexts");

      // TODO - scan the contexts.src file and see if HCD (hierarchical code)
      // for a given terminology is populated. If so we are loading transitive
      // closure and tree positions for that terminology, otherwise we are
      // computing it.
      
//      for (Terminology t : referencedTerminologies){
//        if(!hcdTerminologies.contains(t)){
//          
//          TransitiveClosureAlgorithm algo2 = null;
//          TreePositionAlgorithm algo3 = null;
//          
//            // Only compute for organizing class types
//            if (t.getOrganizingClassType() != null) {
//              algo2 = new TransitiveClosureAlgorithm();
//              algo2.setTerminology(t.getTerminology());
//              algo2.setVersion(t.getVersion());
//              algo2.setIdType(t.getOrganizingClassType());
//              // some terminologies may have cycles, allow these for now.
//              algo2.setCycleTolerant(true);
//              algo2.compute();
//              algo2.close();
//            }
//
//          //
//          // Compute tree positions
//          // Refresh caches after metadata has changed in loader
//            if (t.getOrganizingClassType() != null) {
//              algo3 = new TreePositionAlgorithm();
//              algo3.setTerminology(t.getTerminology());
//              algo3.setVersion(t.getVersion());
//              algo3.setIdType(t.getOrganizingClassType());
//              // some terminologies may have cycles, allow these for now.
//              algo3.setCycleTolerant(true);
//              // compute "semantic types" for concept hierarchies
//              if (t.getOrganizingClassType() == IdType.CONCEPT) {
//                algo3.setComputeSemanticType(true);
//              }
//              algo3.compute();
//              algo3.close();
//            }
//          }
//        
//        else{
//          //TODO - load the transitive relationships and tree positions from the file
//          
//        }
//        }

      // Update the progress
      updateProgress();

      logAndCommit("[Context Loader] Contexts processed ", stepsCompleted,
          RootService.logCt, RootService.commitCt);

      logInfo("[ContextLoader] Added " + addCount + " new Contexts.");
      logInfo("[ContextLoader] Updated " + updateCount + " existing Contexts.");

      logInfo("  project = " + getProject().getId());
      logInfo("  workId = " + getWorkId());
      logInfo("  activityId = " + getActivityId());
      logInfo("  user  = " + getLastModifiedBy());
      logInfo("Finished CONTEXTLOADING");

    } catch (

    Exception e) {
      logError("Unexpected problem - " + e.getMessage());
      throw e;
    }

  }

  /**
   * Cache existing terminologies. Key = Terminology_Version, or just
   * Terminology if version = "latest"
   *
   * @throws Exception the exception
   */
  private void cacheExistingTerminologies() throws Exception {

    for (final Terminology term : getTerminologies().getObjects()) {
      // lazy init
      term.getSynonymousNames().size();
      term.getRootTerminology().getTerminology();
      if (term.getVersion().equals("latest")) {
        loadedTerminologies.put(term.getTerminology(), term);
      } else {
        loadedTerminologies.put(term.getTerminology() + "_" + term.getVersion(),
            term);
      }
    }
  }  
  

//  /**
//   * Identify all terminologies from insertion.
//   *
//   * @param lines the lines
//   * @throws Exception the exception
//   */
//  private void identifyAllTerminologiesFromInsertion(List<String> lines)
//    throws Exception {
//
//    String fields[] = new String[18];
//    steps = lines.size();
//    stepsCompleted = 0;
//
//    for (String line : lines) {
//
//      FieldedStringTokenizer.split(line, "|", 18, fields);
//
//      // For the purpose of this method, all we care about:
//      // fields[6]: source
//      // fields[13]: id_qualifier_1
//      // fields[15]: id_qualifier_2
//
//      String terminology = fields[6].contains("_")
//          ? fields[6].substring(0, fields[6].indexOf('_')) : fields[6];
//      allTerminologiesFromInsertion.add(terminology);
//
//      terminology = fields[13].contains("_")
//          ? fields[13].substring(0, fields[13].indexOf('_')) : fields[13];
//      allTerminologiesFromInsertion.add(terminology);
//
//      terminology = fields[15].contains("_")
//          ? fields[15].substring(0, fields[15].indexOf('_')) : fields[15];
//
//      allTerminologiesFromInsertion.add(terminology);
//    }
//  }  
  
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

  /**
   * Update progress.
   *
   * @throws Exception the exception
   */
  public void updateProgress() throws Exception {
    stepsCompleted++;
    int currentProgress = (int) ((100 * stepsCompleted / steps));
    if (currentProgress > previousProgress) {
      fireProgressEvent(currentProgress,
          "CONTEXTLOADING progress: " + currentProgress + "%");
      previousProgress = currentProgress;
    }
  }

  /**
   * Sets the properties.
   *
   * @param p the properties
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    checkRequiredProperties(new String[] {
        // TODO - handle problem with config.properties needing properties
    }, p);
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

    return params;
  }

}