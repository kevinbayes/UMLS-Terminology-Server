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

import org.apache.log4j.Logger;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;
import com.wci.umls.server.jpa.content.AtomJpa;
import com.wci.umls.server.jpa.content.AtomSubsetJpa;
import com.wci.umls.server.jpa.content.AtomSubsetMemberJpa;
import com.wci.umls.server.jpa.content.AttributeJpa;
import com.wci.umls.server.jpa.content.ConceptSubsetJpa;
import com.wci.umls.server.jpa.content.ConceptSubsetMemberJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomSubset;
import com.wci.umls.server.model.content.AtomSubsetMember;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.ComponentHasAttributes;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptSubset;
import com.wci.umls.server.model.content.ConceptSubsetMember;
import com.wci.umls.server.model.content.Subset;
import com.wci.umls.server.model.content.SubsetMember;
import com.wci.umls.server.model.meta.Terminology;
import com.wci.umls.server.services.RootService;
import com.wci.umls.server.services.handlers.IdentifierAssignmentHandler;

/**
 * Implementation of an algorithm to import subset members.
 */
public class SubsetLoaderAlgorithm extends AbstractInsertMaintReleaseAlgorithm {

  /** The mapping add count. */
  private int subsetMemberAddCount = 0;

  /** The mapping attribute add count. */
  private int subsetMemberAttributeAddCount = 0;

  /** The mapset add count. */
  private int subsetAddCount = 0;

  /** The added subsets. */
  private Map<String, Subset> addedSubsets = new HashMap<>();

  /** The added subset members. */
  private Map<String, Long> addedSubsetMembers = new HashMap<>();

  /**
   * Instantiates an empty {@link SubsetLoaderAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public SubsetLoaderAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("SUBSETLOADER");
    setLastModifiedBy("admin");
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    ValidationResult validationResult = new ValidationResultJpa();

    if (getProject() == null) {
      throw new Exception("Subset Member Loading requires a project to be set");
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

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());
    commitClearBegin();

    // This algorithm can use up a Lot of memory, so start by clearing the
    // caches.
    clearCaches();

    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    // Set up the handler for identifier assignment
    final IdentifierAssignmentHandler handler =
        newIdentifierAssignmentHandler(getProject().getTerminology());
    handler.setTransactionPerOperation(false);
    handler.beginTransaction();

    try {

      logInfo("  Processing attributes.src");
      commitClearBegin();

      //
      // Load the attributes.src file, keeping only subset member lines
      //
      final List<String> lines = loadFileIntoStringList(getSrcDirFile(),
          "attributes.src", "(.*)(SUBSET_MEMBER)(.*)", null, null);

      // Set the number of steps to twice the number of lines to be processed
      // (we'll be looping through everything twice)
      setSteps(2 * lines.size());

      // Scan through and find all Subsets that need to be created
      for (final String line : lines) {
        // Check for a cancelled call once every 100 lines
        if (getStepsCompleted() % 100 == 0) {
          checkCancel();
        }

        createSubsets(line);
        updateProgress();
      }

      // Scan through the lines again, and find all Subset members that need to
      // be created
      for (final String line : lines) {
        // Check for a cancelled call once every 100 lines
        if (getStepsCompleted() % 100 == 0) {
          checkCancel();
        }

        createSubsetMembersAndAttributes(line, handler);
        updateProgress();
        handler.silentIntervalCommit(getStepsCompleted(), RootService.logCt,
            RootService.commitCt);
      }

      commitClearBegin();
      handler.commit();

      // Clear the caches
      clearCaches();

      logInfo("  subset count = " + subsetAddCount);
      logInfo("  subset member count " + subsetMemberAddCount);
      logInfo(
          "  subset member attribute count = " + subsetMemberAttributeAddCount);

      logInfo("Finished " + getName());

    } catch (Exception e) {
      logError("Unexpected problem - " + e.getMessage());
      handler.rollback();
      handler.close();
      throw e;
    }

  }

  /**
   * Creates the subsets.
   *
   * @param line the line
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private void createSubsets(String line) throws Exception {
    String fields[] = new String[14];

    FieldedStringTokenizer.split(line, "|", 14, fields);

    // Fields:
    // 0 source_attribute_id
    // 1 sg_id
    // 2 attribute_level
    // 3 attribute_name
    // 4 attribute_value
    // 5 source
    // 6 status
    // 7 tobereleased
    // 8 released
    // 9 suppressible
    // 10 sg_type_1
    // 11 sg_qualifier_1
    // 12 source_atui
    // 13 hashcode

    // e.g.
    // 2|900000000000530003|S|SUBSET_MEMBER|900000000000456007~ATTRIBUTEDESCRIPTION~900000000000532006|SNOMEDCT_US_2016_09_01|R|Y|N|N|SOURCE_CUI|SNOMEDCT_US_2016_09_01|06522d4b-2512-4c08-9ab6-2a2a0ef2e660|84ea804dcaca003d7071e93acde83391|
    // 3|900000000000530003|S|SUBSET_MEMBER|900000000000456007~ATTRIBUTETYPE~900000000000460005|SNOMEDCT_US_2016_09_01|R|Y|N|N|SOURCE_CUI|SNOMEDCT_US_2016_09_01|06522d4b-2512-4c08-9ab6-2a2a0ef2e660|ba87f8f1dc476846202efb4e86812fda|
    // 4|900000000000530003|S|SUBSET_MEMBER|900000000000456007~ATTRIBUTEORDER~0|SNOMEDCT_US_2016_09_01|R|Y|N|N|SOURCE_CUI|SNOMEDCT_US_2016_09_01|06522d4b-2512-4c08-9ab6-2a2a0ef2e660|62fdb3cf5b09a68a28efcd60d3e2bd00|

    // Split out the micro-syntax as well
    final String atvFields[] = new String[3];
    FieldedStringTokenizer.split(fields[4], "~", 3, atvFields);
    final String subsetId = atvFields[0];
    final String referencedTerminologyAndVersion = fields[5];
    final String subsetType = fields[10];

    // Stop if this subset has already been created
    if (addedSubsets.containsKey(subsetId + referencedTerminologyAndVersion)) {
      return;
    }

    // Load the referenced terminology
    final Terminology referencedTerminology =
        getCachedTerminology(referencedTerminologyAndVersion);
    if (referencedTerminology == null) {
      logWarn("Warning - terminology not found: " + fields[5]
          + ". Could not process the following line:\n\t" + line);
      return;
    }

    // Load the associated SB atom
    // Generate query string
    final String query = "SELECT a " + "FROM AtomJpa a "
        + "WHERE a.terminology=:terminology AND a.version=:version "
        + "AND a.codeId=:codeId " + "AND a.termType=:termType";

    // Generate parameters to pass into query executions
    Map<String, String> params = new HashMap<>();
    params.put("terminology", referencedTerminology.getTerminology());
    params.put("version", referencedTerminology.getVersion());
    params.put("codeId", subsetId);
    params.put("termType", "SB");

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

    // Return the result list (should only return a single atom).
    final List<Object> list = jpaQuery.getResultList();
    if (list.size() != 1) {
      throw new Exception("Unexpected number of atoms returned by: " + query);
    }

    final Atom sbAtom = (AtomJpa) list.get(0);

    if (subsetType.equals("SOURCE_AUI")) {
      // Create a new atom subset
      final AtomSubset atomSubset = new AtomSubsetJpa();
      atomSubset.setBranch(Branch.ROOT);
      atomSubset.setDescription(sbAtom.getName());
      atomSubset.setName(sbAtom.getName());
      atomSubset.setObsolete(false);
      atomSubset.setPublishable(true);
      atomSubset.setPublished(false);
      atomSubset.setSuppressible(false);
      atomSubset.setTerminology(referencedTerminology.getTerminology());
      atomSubset.setTerminologyId(subsetId);
      atomSubset.setVersion(referencedTerminology.getVersion());
      addSubset(atomSubset);
      subsetAddCount++;

      // Add it to the cache, so it can be found by later subset members
      addedSubsets.put(
          atomSubset.getTerminologyId() + referencedTerminologyAndVersion,
          atomSubset);
    } else if (subsetType.equals("SOURCE_CUI")) {
      // Create a new concept subset
      final ConceptSubset conceptSubset = new ConceptSubsetJpa();
      conceptSubset.setBranch(Branch.ROOT);
      conceptSubset.setDescription(sbAtom.getName());
      conceptSubset.setName(sbAtom.getName());
      conceptSubset.setObsolete(false);
      conceptSubset.setPublishable(true);
      conceptSubset.setPublished(false);
      conceptSubset.setSuppressible(false);
      conceptSubset.setTerminology(referencedTerminology.getTerminology());
      conceptSubset.setVersion(referencedTerminology.getVersion());
      conceptSubset.setTerminologyId(subsetId);

      addSubset(conceptSubset);
      subsetAddCount++;

      // Add it to the cache, so it can be found by later subset members
      addedSubsets.put(
          conceptSubset.getTerminologyId() + referencedTerminologyAndVersion,
          conceptSubset);
    } else {
      throw new Exception("Unexpected subset type for member: " + line);
    }
  }

  /**
   * Creates the subset members and attributes.
   *
   * @param line the line
   * @param handler the handler
   * @throws Exception the exception
   */
  private void createSubsetMembersAndAttributes(final String line,
    final IdentifierAssignmentHandler handler) throws Exception {

    final String fields[] = new String[14];

    FieldedStringTokenizer.split(line, "|", 14, fields);

    // Fields:
    // 0 source_attribute_id
    // 1 sg_id
    // 2 attribute_level
    // 3 attribute_name
    // 4 attribute_value
    // 5 source
    // 6 status
    // 7 tobereleased
    // 8 released
    // 9 suppressible
    // 10 sg_type_1
    // 11 sg_qualifier_1
    // 12 source_atui
    // 13 hashcode

    // e.g.
    // 2|900000000000530003|S|SUBSET_MEMBER|900000000000456007~ATTRIBUTEDESCRIPTION~900000000000532006|SNOMEDCT_US_2016_09_01|R|Y|N|N|SOURCE_CUI|SNOMEDCT_US_2016_09_01|06522d4b-2512-4c08-9ab6-2a2a0ef2e660|84ea804dcaca003d7071e93acde83391|
    // 3|900000000000530003|S|SUBSET_MEMBER|900000000000456007~ATTRIBUTETYPE~900000000000460005|SNOMEDCT_US_2016_09_01|R|Y|N|N|SOURCE_CUI|SNOMEDCT_US_2016_09_01|06522d4b-2512-4c08-9ab6-2a2a0ef2e660|ba87f8f1dc476846202efb4e86812fda|
    // 4|900000000000530003|S|SUBSET_MEMBER|900000000000456007~ATTRIBUTEORDER~0|SNOMEDCT_US_2016_09_01|R|Y|N|N|SOURCE_CUI|SNOMEDCT_US_2016_09_01|06522d4b-2512-4c08-9ab6-2a2a0ef2e660|62fdb3cf5b09a68a28efcd60d3e2bd00|

    // Split out the micro-syntax as well
    final String atvFields[] = new String[3];
    FieldedStringTokenizer.split(fields[4], "~", 3, atvFields);
    final String subsetId = atvFields[0];
    final String referencedTerminologyAndVersion = fields[5];
    final String subsetType = fields[10];

    // Load the referenced terminology
    final Terminology referencedTerminology =
        getCachedTerminology(referencedTerminologyAndVersion);
    if (referencedTerminology == null) {
      logWarn("Warning - terminology not found: " + fields[5]
          + ". Could not process the following line:\n\t" + line);
      return;
    }

    final String subsetMemberIdKey =
        fields[12] + referencedTerminology.getTerminology();

    // Load the referenced object
    ComponentHasAttributes referencedComponent =
        (ComponentHasAttributes) getComponent(fields[10], fields[1],
            getCachedTerminologyName(fields[11]), null);
    if (referencedComponent == null) {
      logWarn("Warning - could not find Component for type: " + fields[10]
          + ", terminologyId: " + fields[1] + ", and terminology:" + fields[11]
          + ". Could not process the following line:\n\t" + line);
      return;
    }

    // Check to see if we've already added this subset member
    Long memberId = addedSubsetMembers.get(subsetMemberIdKey);
    SubsetMember<? extends ComponentHasAttributes, ? extends Subset> member =
        null;
    if (memberId != null) {
      if (subsetType.equals("SOURCE_AUI")) {
        member = this.getSubsetMember(memberId, AtomSubsetMemberJpa.class);
      } else if (subsetType.equals("SOURCE_CUI")) {
        member = this.getSubsetMember(memberId, ConceptSubsetMemberJpa.class);
      } else {
        throw new Exception("Unexpected subset type for member: " + line);
      }
    }

    // If not, create it
    if (member == null) {
      if (subsetType.equals("SOURCE_AUI")) {
        // Load the atomSubset
        AtomSubset atomSubset = (AtomSubset) addedSubsets
            .get(subsetId + referencedTerminologyAndVersion);
        if (atomSubset == null) {
          logWarn("Warning - no cached subset found with key: " + subsetId
              + referencedTerminologyAndVersion
              + ". Could not process the following line:\n\t" + line);
          return;
        }

        final AtomSubsetMember atomSubsetMember = new AtomSubsetMemberJpa();
        atomSubsetMember.setMember((Atom) referencedComponent);
        atomSubsetMember.setSubset(atomSubset);

        member = atomSubsetMember;

      } else if (subsetType.equals("SOURCE_CUI")) {

        final ConceptSubset conceptSubset = (ConceptSubset) addedSubsets
            .get(subsetId + referencedTerminologyAndVersion);
        if (conceptSubset == null) {
          logWarn("Warning - no cached subset found with key: " + subsetId
              + referencedTerminologyAndVersion
              + ". Could not process the following line:\n\t" + line);
          return;
        }

        ConceptSubsetMember conceptSubsetMember = new ConceptSubsetMemberJpa();
        conceptSubsetMember.setMember((Concept) referencedComponent);
        conceptSubsetMember.setSubset(conceptSubset);

        member = conceptSubsetMember;

      } else {
        throw new Exception("Unexpected subset type for member: " + line);
      }

      // Populate common member fields
      member.setTerminologyId(fields[12]);
      member.setTerminology(referencedTerminology.getTerminology());
      member.setVersion(referencedTerminology.getVersion());
      member.setSuppressible("OYE".contains(fields[9].toUpperCase()));
      member.setObsolete(fields[9].toUpperCase().equals("O"));
      member.setPublishable(!fields[7].equals("N"));
      member.setPublished(!fields[8].equals("N"));

      // Add member
      addSubsetMember(member);
      subsetMemberAddCount++;

      // Add to the cache.
      addedSubsetMembers.put(subsetMemberIdKey, member.getId());
    }

    // TODO: handle the "update "case - e.g. obsolete, suppressible, version if we're reusing the member
    
    // Always make an attribute, even if it's an entry for JUST a membership
    final Attribute memberAtt = new AttributeJpa();

    // Fake an attribute to calculate the alternate terminology Id
    // Create the fake attribute
    final Attribute newAttribute = new AttributeJpa();
    newAttribute.setName(fields[3]);
    newAttribute.setValue(fields[4]);
    newAttribute.setTerminology(referencedTerminology.getTerminology());
    newAttribute.setTerminologyId("");

    // Compute attribute identity
    final String subsetAtui =
        handler.getTerminologyId(newAttribute, referencedComponent);

    // Assign the ATUI to the member attribute.
    memberAtt.getAlternateTerminologyIds().put(getProject().getTerminology(),
        subsetAtui);

    // TODO: check whether this attribute is already connected to the subset member
    // and if so reuse it (e.g. update suppressible, obsolete, version)
    
    // No terminology id for the member attribute
    memberAtt.setTerminologyId("");

    memberAtt.setTerminology(referencedTerminology.getTerminology());
    memberAtt.setVersion(referencedTerminology.getVersion());
    memberAtt.setSuppressible("OYE".contains(fields[9].toUpperCase()));
    memberAtt.setObsolete(fields[9].toUpperCase().equals("O"));
    memberAtt.setPublishable(!fields[7].equals("N"));
    memberAtt.setPublished(!fields[8].equals("N"));
    if (fields[4].contains("~")) {
      memberAtt.setName(atvFields[1] != null ? atvFields[1] : "");
      memberAtt.setValue(atvFields[2] != null ? atvFields[2] : "");
    } else {
      memberAtt.setName("");
      memberAtt.setValue("Placeholder for ATUI");
    }

    addAttribute(memberAtt, member);
    subsetMemberAttributeAddCount++;

    member.getAttributes().add(memberAtt);
    updateSubsetMember(member);
  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());
    // No molecular actions will be generated by reset
    setMolecularActionFlag(false);

    // Find all atom subsets that were created since the insertion started, and
    // set them to unpublishable.
    String query = "SELECT m.id FROM AtomSubsetJpa m " + "WHERE m.id > "
        + getProcess().getExecutionInfo().get("maxAtomSubsetIdPreInsertion");

    // Execute a query to get Atom Subset Ids
    final List<Long> atomSubsetIds =
        executeSingleComponentIdQuery(query, QueryType.JPQL,
            getDefaultQueryParams(getProject()), AtomSubsetJpa.class, false);

    for (final Long id : atomSubsetIds) {
      final AtomSubset atomSubset =
          (AtomSubset) getSubset(id, AtomSubsetJpa.class);
      atomSubset.setPublishable(false);
      updateSubset(atomSubset);
    }

    // Do the same for the concept subsets.
    query = "SELECT m.id FROM ConceptSubsetJpa m " + "WHERE m.id > "
        + getProcess().getExecutionInfo().get("maxConceptSubsetIdPreInsertion");

    // Execute a query to get Concept Subset Ids
    final List<Long> conceptSubsetIds =
        executeSingleComponentIdQuery(query, QueryType.JPQL,
            getDefaultQueryParams(getProject()), ConceptSubsetJpa.class, false);

    for (final Long id : conceptSubsetIds) {
      final ConceptSubset conceptSubset =
          (ConceptSubset) getSubset(id, ConceptSubsetJpa.class);
      conceptSubset.setPublishable(false);
      updateSubset(conceptSubset);
    }
    logInfo("Finished RESET " + getName());
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();

    return params;
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return "Loads and processes an attributes.src file to load Subset and Subset Member objects.";
  }

}