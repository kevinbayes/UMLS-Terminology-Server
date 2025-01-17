/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Query;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.helpers.meta.TerminologyList;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;
import com.wci.umls.server.jpa.algo.TreePositionAlgorithm;
import com.wci.umls.server.jpa.content.AtomTreePositionJpa;
import com.wci.umls.server.jpa.content.CodeTreePositionJpa;
import com.wci.umls.server.jpa.content.ConceptTreePositionJpa;
import com.wci.umls.server.jpa.content.DescriptorTreePositionJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomClass;
import com.wci.umls.server.model.content.AtomTreePosition;
import com.wci.umls.server.model.content.Code;
import com.wci.umls.server.model.content.CodeTreePosition;
import com.wci.umls.server.model.content.ComponentHasAttributesAndName;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptTreePosition;
import com.wci.umls.server.model.content.Descriptor;
import com.wci.umls.server.model.content.DescriptorTreePosition;
import com.wci.umls.server.model.content.TreePosition;
import com.wci.umls.server.model.meta.IdType;
import com.wci.umls.server.model.meta.Terminology;
import com.wci.umls.server.services.RootService;

/**
 * Implementation of an algorithm to import contexts.
 */
public class ContextLoaderAlgorithm
    extends AbstractInsertMaintReleaseAlgorithm {

  /** The added tree positions. */
  private int addedTreePositions;

  /** The removed tree pos count. */
  private int removedTreePosCount;

  /**
   * The child and descendant counts. Key = full ptr string (e.g.
   * 31926003.362215152.362207261.362220676.362208073.362250833.362169686)
   * Value.[0] = Child count Value.[1] = Descendant count
   */
  private Map<String, int[]> childAndDescendantCountsMap = new HashMap<>();

  /**
   * The lines to load. This contains ONLY lines that need to have contexts
   * loaded from the file (and not calculated).
   */
  private List<String> linesToLoad = new ArrayList<>();

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

    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    try {

      logInfo("  Process contexts.src");
      commitClearBegin();

      //
      // Load the contexts.src file
      //
      final List<String> lines = loadFileIntoStringList(getSrcDirFile(),
          "contexts.src", null, "(.*)SIB(.*)", null);

      // Scan the contexts.src file and see if HCD (hierarchical code)
      // for a given terminology is populated.
      final Set<String> withHcd = findTermsWithHcd(lines);
      final Set<String> computedTerminologies = new HashSet<>();
      final Set<Terminology> allReferencedTerminologies = new HashSet<>();

      final String fields[] = new String[17];
      for (final String line : lines) {

        FieldedStringTokenizer.split(line, "|", 17, fields);

        final Terminology terminology = getCachedTerminology(fields[4]);

        if (terminology == null) {
          logWarn("WARNING - terminology not found: " + fields[6]
              + ". Could not process the following line:\n\t" + line);
          continue;
        }

        allReferencedTerminologies.add(terminology);

        // // If the specified terminology never has a populated HCD, the
        // // transitive relationships and tree positions can be computed.
        // if (!withHcd.contains(terminology.getTerminology())) {

        // If terminology is hierarchy computable, compute the hierarchy.
        if (terminology.getRootTerminology().isHierarchyComputable()) {
          // Only compute once per terminology
          if (!computedTerminologies.contains(terminology.getTerminology())) {
            computeContexts(terminology);
            computedTerminologies.add(terminology.getTerminology());
          }
        }
        // // If the specified terminology has a populated HCD, we need to load
        // the
        // // Tree Positions from the file contents.

        // Otherwise, load the tree positions from the file contents.
        else {

          // Save this line to process later
          linesToLoad.add(line);

          // Populate childAndDescendant counts based on this line's PTR

          final String parentTreeRel = fields[7];

          // If this particular full PTR has never been seen, add with a child
          // and descendant count of 1 each.
          if (!childAndDescendantCountsMap.containsKey(parentTreeRel)) {
            childAndDescendantCountsMap.put(parentTreeRel, new int[] {
                1, 1
            });
          }
          // If it has been seen before, increment both child and descendent
          // counts by 1
          else {
            final int[] currentChildDescendantCount =
                childAndDescendantCountsMap.get(parentTreeRel);
            childAndDescendantCountsMap.put(parentTreeRel, new int[] {
                ++currentChildDescendantCount[0],
                ++currentChildDescendantCount[1]
            });
          }

          String parentTreeRelSub = parentTreeRel;

          // Loop through the parentTreeRel string, stripping off trailing
          // elements until they're gone.
          while (parentTreeRelSub.contains(".")) {
            parentTreeRelSub = parentTreeRelSub.substring(0,
                parentTreeRelSub.lastIndexOf("."));
            // If this particular sub PTR has never been seen, add with a child
            // count of 0, and a descendant count of 1.
            if (!childAndDescendantCountsMap.containsKey(parentTreeRelSub)) {
              childAndDescendantCountsMap.put(parentTreeRelSub, new int[] {
                  0, 1
              });
            }
            // If it has been seen before, increment descendant count only by 1
            else {
              final int[] currentChildDescendantCount =
                  childAndDescendantCountsMap.get(parentTreeRelSub);
              childAndDescendantCountsMap.put(parentTreeRelSub, new int[] {
                  currentChildDescendantCount[0],
                  ++currentChildDescendantCount[1]
              });
            }
          }
        }
      }

      // Set the number of steps to the number of contexts.src lines that will
      // be actually loaded
      setSteps(linesToLoad.size());

      for (String line : linesToLoad) {
        // Check for a cancelled call once every 100 lines
        if (getStepsCompleted() % 100 == 0) {
          checkCancel();
        }

        loadContexts(line);

        // Update the progress
        updateProgress();
      }

      commitClearBegin();

      // Only show this counter if contexts.src actually had any lines to load.
      // Otherwise, all logging will be handled by the sub-algorithms
      if (getSteps() > 0) {
        logInfo("  added tree position count = " + addedTreePositions);
      }

      // Get all terminology Names referenced in sources.src
      final Set<Terminology> referencedTerminologies =
          getReferencedTerminologies();
      final Set<String> referencedTerminologyNames = new HashSet<>();
      for (final Terminology terminology : referencedTerminologies) {
        referencedTerminologyNames.add(terminology.getTerminology());
      }

      // Get all of the terminologies currently in the database
      final TerminologyList allTerminologies = getTerminologies();
      final List<Terminology> nonCurrentReferencedTerminologies =
          new ArrayList<>();

      // Get all non-current versions of terminologies referenced in
      // sources.src
      for (final Terminology terminology : allTerminologies.getObjects()) {
        if (referencedTerminologyNames.contains(terminology.getTerminology())
            && !terminology.isCurrent()) {
          nonCurrentReferencedTerminologies.add(terminology);
        }
      }

      for (final Terminology terminology : nonCurrentReferencedTerminologies) {
        removedTreePosCount += removeTreePositions(terminology);
        commitClearBegin();
      }

      logInfo("  removed tree position count = " + removedTreePosCount);

      logInfo("Finished " + getName());

    } catch (

    Exception e) {
      logError("Unexpected problem - " + e.getMessage());
      throw e;
    }

  }

  /**
   * Removes the tree positions.
   *
   * @param term the term
   * @return the int
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private int removeTreePositions(Terminology term) throws Exception {
    int removedCount = 0;

    logInfo("  Removing tree positions for terminology: "
        + term.getTerminology() + ", version: " + term.getVersion());

    IdType organizingClassType = term.getOrganizingClassType();
    Class<?> clazz = null;

    if (organizingClassType.equals(IdType.CONCEPT)) {
      clazz = ConceptTreePositionJpa.class;
    } else if (organizingClassType.equals(IdType.DESCRIPTOR)) {
      clazz = DescriptorTreePositionJpa.class;
    } else if (organizingClassType.equals(IdType.CODE)) {
      clazz = CodeTreePositionJpa.class;
    } else if (organizingClassType.equals(IdType.ATOM)) {
      clazz = AtomTreePositionJpa.class;
    }

    // Attempt to remove tree positions for organizing class type (if not Atom
    // Tree Positon)
    if (clazz != AtomTreePositionJpa.class) {
      Query query = manager.createQuery("SELECT a.id FROM "
          + clazz.getSimpleName() + " a WHERE terminology = :terminology "
          + " AND version = :version");
      query.setParameter("terminology", term.getTerminology());
      query.setParameter("version", term.getVersion());
      for (final Long id : (List<Long>) query.getResultList()) {
        removeTreePosition(id,
            (Class<? extends TreePosition<? extends AtomClass>>) clazz);
        logAndCommit(removedCount++, RootService.logCt, RootService.commitCt);
      }
    }

    // Attempt again for AtomTreePositionJpa.class
    Query query = manager.createQuery(
        "SELECT a.id FROM AtomTreePositionJpa a WHERE terminology = :terminology "
            + " AND version = :version");
    query.setParameter("terminology", term.getTerminology());
    query.setParameter("version", term.getVersion());
    for (final Long id : (List<Long>) query.getResultList()) {
      removeTreePosition(id, AtomTreePositionJpa.class);
      logAndCommit(removedCount++, RootService.logCt, RootService.commitCt);
    }

    return removedCount;
  }

  /**
   * Calculate contexts.
   *
   * @param terminology the term
   * @throws Exception the exception
   */
  private void computeContexts(Terminology terminology) throws Exception {
    logInfo("  Compute contexts for " + terminology.getTerminology());

    // Check for a cancelled call before starting
    checkCancel();

    // Don't handle transitiveRelationships
    // //
    // // Compute transitive closures
    // //
    // TransitiveClosureAlgorithm algo = null;
    // // Only compute for organizing class types
    // if (term.getOrganizingClassType() != null) {
    // algo = new TransitiveClosureAlgorithm();
    // algo.setLastModifiedBy(getLastModifiedBy());
    // algo.setTerminology(term.getTerminology());
    // algo.setVersion(term.getVersion());
    // algo.setIdType(term.getOrganizingClassType());
    // // some terminologies may have cycles, allow these for now.
    // algo.setCycleTolerant(true);
    // algo.compute();
    // algo.close();
    //
    // }

    //
    // Compute tree positions
    //

    // Compute for organizing class types and atoms (no way to know for sure
    // which one needs doing. One algo will create tree positions, and
    // the other won't, so it covers our bases).
    TreePositionAlgorithm algo2 = new TreePositionAlgorithm();
    algo2.setLastModifiedBy(getLastModifiedBy());
    algo2.setTerminology(terminology.getTerminology());
    algo2.setVersion(terminology.getVersion());
    algo2.setIdType(terminology.getOrganizingClassType());
    algo2.setWorkId(getWorkId());
    algo2.setActivityId(getActivityId());
    algo2.setCycleTolerant(false);
    algo2.setComputeSemanticType(false);
    algo2.setProject(getProject());
    algo2.compute();
    algo2.close();

    algo2 = new TreePositionAlgorithm();
    algo2.setLastModifiedBy(getLastModifiedBy());
    algo2.setTerminology(terminology.getTerminology());
    algo2.setVersion(terminology.getVersion());
    algo2.setIdType(IdType.ATOM);
    algo2.setWorkId(getWorkId());
    algo2.setActivityId(getActivityId());
    algo2.setCycleTolerant(false);
    algo2.setComputeSemanticType(false);
    algo2.setProject(getProject());
    algo2.compute();
    algo2.close();
  }

  /**
   * Load contexts.
   *
   * @param line the line
   * @throws Exception the exception
   */
  private void loadContexts(String line) throws Exception {

    final String fields[] = new String[17];
    FieldedStringTokenizer.split(line, "|", 17, fields);

    // If sg_type_1 and sg_type_2 don't match, fire a warning and skip the
    // line.
    if (!fields[12].equals(fields[15])) {
      logWarn("WARNING - type 1: " + fields[12] + " does not equals type 2: "
          + fields[15] + ". Could not process the following line:\n\t" + line);
      return;
    }

    // Extract the "ptr" field 12345.12346.12347 and the sg_type_1
    // field -> this will tell you the type of object (e.g.
    // ConceptTransitiveRelationship
    final String parentTreeRel = fields[7];

    final List<Atom> ptrAtoms = new ArrayList<>();
    final List<String> ptrAltIds = new ArrayList<>();
    ptrAltIds.addAll(Arrays.asList(parentTreeRel.split("\\.")));

    // Add the atom alternate Id in the first column to the end of the list
    // (needed in that position for Transitive Relationships, and Tree Positions
    // will use the loaded atom as well)
    ptrAltIds.add(fields[0]);

    for (final String element : ptrAltIds) {

      final Atom atom = (Atom) getComponent("SRC_ATOM_ID", element, null, null);

      // If Atom can't be found, fire a warning and move onto the
      // next line of the file.
      if (atom == null) {
        // EXCEPTION: if this is the first id in the list, the reason it
        // couldn't be found is likely because it is a SRC atom. In this case,
        // don't error out - just skip the element and continue.
        if (ptrAltIds.indexOf(element) == 0) {
          continue;
        }
        logWarn("Warning - atom not found for alternate Terminology Id: "
            + element + ". Could not process the following line:\n\t" + line);
        return;
      }

      ptrAtoms.add(atom);
    }

    // Check the first atom to make sure it isn't a SRC atom. If it is, drop it
    // from the list.
    if (ptrAtoms.get(0).getTerminology().equals("SRC")) {
      ptrAtoms.remove(0);
    }

    // Don't handle transitiveRelationships
    // createTransitiveRelationships(clazz, ptrAtoms);

    // Tree Positions use the last atom in the list (which was loaded from the
    // first column of the line) for determining the node.
    final Atom nodeAtom = ptrAtoms.get(ptrAtoms.size() - 1);
    createTreePositions(fields[12], nodeAtom, parentTreeRel, fields[6]);
  }

  // /**
  // * Creates the transitive relationships.
  // *
  // * @param clazz the clazz
  // * @param ptrAtoms the ptr atoms
  // * @throws Exception the exception
  // */
  // private void createTransitiveRelationships(Class<?> clazz,
  // List<Atom> ptrAtoms) throws Exception {
  // // For transitive relationships, create one from each "higher" level
  // // object to each "lower" level one (e.g. in the example above
  // // 12345->12346, 12345->123467, and 12346->123467). Save these
  // // pairwise connections so you don't recreate objects for the same
  // // pairs (e.g. have a Set<String> that stores superTypeId+subTypeId)
  //
  // // Can't create relationships if there aren't any atoms in the
  // // list...
  // if (ptrAtoms.isEmpty()) {
  // return;
  // }
  //
  // final Atom superAtom = ptrAtoms.get(0);
  // final AbstractAtomClass superAtomContainer =
  // getCachedAtomContainer(clazz, superAtom);
  // int depthCounter = 0;
  // for (Atom subAtom : ptrAtoms) {
  // final AbstractAtomClass subAtomContainer =
  // getCachedAtomContainer(clazz, subAtom);
  //
  // // Skip if this pair of containers have already created a Transitive
  // // Relationship created for them.
  // if (createdTransRels.contains(superAtomContainer.getId().toString() + "_"
  // + subAtomContainer.getId().toString())) {
  // continue;
  // }
  //
  // TransitiveRelationship<? extends ComponentHasAttributes> newTransRel =
  // null;
  //
  // if (Concept.class.isAssignableFrom(clazz)) {
  // final ConceptTransitiveRelationshipJpa ctr =
  // new ConceptTransitiveRelationshipJpa();
  // ctr.setSubType((Concept) subAtomContainer);
  // ctr.setSuperType((Concept) superAtomContainer);
  // newTransRel = ctr;
  // } else if (Descriptor.class.isAssignableFrom(clazz)) {
  // final DescriptorTransitiveRelationshipJpa dtr =
  // new DescriptorTransitiveRelationshipJpa();
  // dtr.setSubType((Descriptor) subAtomContainer);
  // dtr.setSuperType((Descriptor) superAtomContainer);
  // newTransRel = dtr;
  // } else if (Code.class.isAssignableFrom(clazz)) {
  // final CodeTransitiveRelationshipJpa cdtr =
  // new CodeTransitiveRelationshipJpa();
  // cdtr.setSubType((Code) subAtomContainer);
  // cdtr.setSuperType((Code) superAtomContainer);
  // newTransRel = cdtr;
  // } else if (Atom.class.isAssignableFrom(clazz)) {
  // final AtomTransitiveRelationshipJpa atr =
  // new AtomTransitiveRelationshipJpa();
  // atr.setSubType(subAtom);
  // atr.setSuperType(superAtom);
  // newTransRel = atr;
  // }
  //
  // newTransRel.setDepth(depthCounter++);
  // newTransRel.setObsolete(false);
  // newTransRel.setPublishable(true);
  // newTransRel.setPublished(false);
  // newTransRel.setSuppressible(false);
  // newTransRel.setTerminology(newTransRel.getSuperType().getTerminology());
  // newTransRel.setTerminologyId("");
  // newTransRel.setVersion(newTransRel.getSuperType().getVersion());
  //
  // // persist the Transitive Relationship
  // addTransitiveRelationship(newTransRel);
  // createdTransRels.add(superAtomContainer.getId().toString() + "_"
  // + subAtomContainer.getId().toString());
  // }
  //
  // // Once all of the relationships have been made with this super atom,
  // remove
  // // it from the list, and run the remaining ones through again.
  // List<Atom> shortenedAtomList = new ArrayList<>(ptrAtoms);
  // shortenedAtomList.remove(superAtom);
  // createTransitiveRelationships(clazz, shortenedAtomList);
  //
  // }

  /**
   * Creates the tree positions.
   *
   * @param idType the id type
   * @param nodeAtom the node atom
   * @param parentTreeRel the parent tree rel
   * @param hcd the hcd
   * @throws Exception the exception
   */
  private void createTreePositions(String idType, Atom nodeAtom,
    String parentTreeRel, String hcd) throws Exception {
    // For tree positions, create one each line. The
    // "node" will always be based on the first field of the
    // contexts.src file.

    final String ancestorPath = parentTreeRel.replace('.', '~');

    // Instantiate the tree position
    TreePosition<? extends ComponentHasAttributesAndName> newTreePos = null;
    if (idType.equals("SOURCE_CUI")) {
      final ConceptTreePosition ctp = new ConceptTreePositionJpa();
      final Concept concept = (Concept) getComponent(idType,
          nodeAtom.getConceptId(), nodeAtom.getTerminology(), null);
      ctp.setNode(concept);
      newTreePos = ctp;
    } else if (idType.equals("SOURCE_DUI")) {
      final DescriptorTreePosition dtp = new DescriptorTreePositionJpa();
      final Descriptor descriptor = (Descriptor) getComponent(idType,
          nodeAtom.getConceptId(), nodeAtom.getTerminology(), null);
      dtp.setNode(descriptor);
      newTreePos = dtp;
    } else if (idType.equals("CODE_SOURCE")) {
      final CodeTreePosition ctp = new CodeTreePositionJpa();
      final Code code = (Code) getComponent(idType, nodeAtom.getConceptId(),
          nodeAtom.getTerminology(), null);
      ctp.setNode(code);
      newTreePos = ctp;
    } else if (idType.equals("SRC_ATOM_ID")) {
      final AtomTreePosition atp = new AtomTreePositionJpa();
      final Atom atom = nodeAtom;
      atp.setNode(atom);
      newTreePos = atp;
    } else {
      throw new Exception("Unsupported id type: " + idType);
    }
    newTreePos.setObsolete(false);
    newTreePos.setSuppressible(false);
    newTreePos.setPublishable(true);
    newTreePos.setPublished(false);
    newTreePos.setAncestorPath(ancestorPath);
    newTreePos.setTerminology(newTreePos.getNode().getTerminology());
    newTreePos.setVersion(newTreePos.getNode().getVersion());
    newTreePos.setChildCt(childAndDescendantCountsMap.get(parentTreeRel)[0]);
    newTreePos
        .setDescendantCt(childAndDescendantCountsMap.get(parentTreeRel)[1]);
    newTreePos.setTerminologyId(hcd);

    // persist the tree position
    addTreePosition(newTreePos);
    addedTreePositions++;

  }

  /**
   * Scan terms and hcds.
   *
   * @param lines the lines
   * @return the map
   * @throws Exception the exception
   */
  private Set<String> findTermsWithHcd(List<String> lines) throws Exception {
    Set<String> termsWithHcds = new HashSet<>();

    final String fields[] = new String[17];
    for (final String line : lines) {
      FieldedStringTokenizer.split(line, "|", 17, fields);

      final String termNameAndVersion = fields[4];
      final Boolean termHasHcd = !ConfigUtility.isEmpty(fields[6]);
      final Terminology terminology = getCachedTerminology(termNameAndVersion);

      if (terminology == null) {
        // No need to fire a warning here - will be done in compute
        continue;
      }

      // Add all terms with populated hcd's to the set
      if (termHasHcd) {
        termsWithHcds.add(terminology.getTerminology());
      }
    }

    return termsWithHcds;
  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());

    // Delete any TreePositions and TransitiveRelationships for all terminology
    // and versions referenced in the contexts.src file.

    // No molecular actions will be generated by this algorithm reset
    setMolecularActionFlag(false);

    // Make sure the process' preconditions are still valid, and that the
    // srcDirFile is set.
    checkPreconditions();

    logInfo("Reset " + getName()
        + ": removing all Tree Positions added by previous run");

    //
    // Load the contexts.src file
    //
    final List<String> lines =
        loadFileIntoStringList(getSrcDirFile(), "contexts.src", null, null, null);

    // Scan through contexts.src, and collect all terminology/versions
    // referenced.
    final Set<String> terminologyAndVersions = new HashSet<>();

    final String fields[] = new String[17];
    for (final String line : lines) {
      FieldedStringTokenizer.split(line, "|", 17, fields);

      final String termNameAndVersion = fields[4];
      terminologyAndVersions.add(termNameAndVersion);
    }

    int removedTreePosCount = 0;

    for (final String terminologyVersion : terminologyAndVersions) {
      final Terminology terminology = getCachedTerminology(terminologyVersion);
      removedTreePosCount += removeTreePositions(terminology);

      commitClearBegin();
    }

    logInfo("  removed tree position count = " + removedTreePosCount);
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
    return "Loads and processes contexts.src and computes tree positions where possible from PAR/CHD relationships.";
  }

}