/*
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.wci.umls.server.ReleaseInfo;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.CancelException;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.jpa.services.HistoryServiceJpa;
import com.wci.umls.server.jpa.services.MetadataServiceJpa;
import com.wci.umls.server.model.content.ConceptSubset;
import com.wci.umls.server.model.content.Subset;
import com.wci.umls.server.model.meta.IdType;
import com.wci.umls.server.services.HistoryService;
import com.wci.umls.server.services.helpers.ProgressEvent;
import com.wci.umls.server.services.helpers.ProgressListener;

/**
 * Implementation of an algorithm to import RF2 snapshot data.
 */
public class Rf2FullLoaderAlgorithm extends AbstractTerminologyLoaderAlgorithm {

  /** Listeners. */
  private List<ProgressListener> listeners = new ArrayList<>();

  /** The loader. */
  final String loader = "loader";

  /** The tree pos algorithm. */
  final TreePositionAlgorithm treePosAlgorithm = new TreePositionAlgorithm();

  /** The trans closure algorithm. */
  final TransitiveClosureAlgorithm transClosureAlgorithm =
      new TransitiveClosureAlgorithm();

  /** The label set algorithm. */
  final LabelSetMarkedParentAlgorithm labelSetAlgorithm =
      new LabelSetMarkedParentAlgorithm();

  /** The RF2 File sorting algorithm. */
  final Rf2FileSorter sorter = new Rf2FileSorter();

  /**
   * Instantiates an empty {@link Rf2FullLoaderAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public Rf2FullLoaderAlgorithm() throws Exception {
    super();
  }

  /* see superclass */
  @Override
  public String getFileVersion() throws Exception {
    Rf2FileSorter sorter = new Rf2FileSorter();
    sorter.setInputDir(inputPath);
    return sorter.getFileVersion();
  }

  /* see superclass */
  @SuppressWarnings("resource")
  @Override
  public void compute() throws Exception {

    // check prerequisites
    if (terminology == null) {
      throw new Exception("Terminology name must be specified");
    }
    if (version == null) {
      throw new Exception("Terminology version must be specified");
    }
    if (inputPath == null) {
      throw new Exception("Input directory must be specified");
    }

    final HistoryService historyService = new HistoryServiceJpa();
    try {

      // Check the input directory
      File inputDirFile = new File(inputPath);
      if (!inputDirFile.exists()) {
        throw new Exception("Specified input directory does not exist");
      }

      // Get the release versions (need to look in complex map too for October
      // releases)
      Logger.getLogger(getClass()).info("  Get release versions");
      Rf2FileSorter sorter = new Rf2FileSorter();
      final File conceptsFile =
          sorter.findFile(new File(inputPath, "Terminology"), "sct2_Concept");
      final Set<String> releaseSet = new HashSet<>();
      BufferedReader reader = new BufferedReader(new FileReader(conceptsFile));
      String line;
      while ((line = reader.readLine()) != null) {
        final String fields[] = FieldedStringTokenizer.split(line, "\t");
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
      final File complexMapFile =
          sorter.findFile(new File(inputPath, "Refset/Map"),
              "der2_iissscRefset_ComplexMap");
      reader = new BufferedReader(new FileReader(complexMapFile));
      while ((line = reader.readLine()) != null) {
        final String fields[] = FieldedStringTokenizer.split(line, "\t");
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
          sorter.findFile(new File(inputPath, "Refset/Map"),
              "der2_iisssccRefset_ExtendedMap");
      reader = new BufferedReader(new FileReader(extendedMapFile));
      while ((line = reader.readLine()) != null) {
        final String fields[] = FieldedStringTokenizer.split(line, "\t");
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
      final List<String> releases = new ArrayList<>(releaseSet);
      Collections.sort(releases);

      // check that release info does not already exist
      Logger.getLogger(getClass()).info("  Releases to process");
      for (final String release : releases) {
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
      sorter.setInputDir(inputPath);
      sorter.setOutputDir("/RF2-sorted-temp/");
      sorter.compute();

      // Open readers
      File outputDir = new File(inputDirFile, "/RF2-sorted-temp/");
      final Rf2Readers readers = new Rf2Readers(outputDir);
      readers.openReaders();

      // Load initial snapshot - first release version
      final Rf2SnapshotLoaderAlgorithm algorithm =
          new Rf2SnapshotLoaderAlgorithm();
      algorithm.setTerminology(terminology);
      algorithm.setVersion(version);
      algorithm.compute();
      algorithm.close();

      // Load deltas
      for (final String release : releases) {
        // Refresh caches for metadata handlers
        new MetadataServiceJpa().refreshCaches();

        if (release.equals(releases.get(0))) {
          continue;
        }

        Rf2DeltaLoaderAlgorithm algorithm2 = new Rf2DeltaLoaderAlgorithm();
        algorithm2.setTerminology(terminology);
        algorithm2.setVersion(version);
        algorithm2.setReleaseVersion(release);
        algorithm2.setReaders(readers);
        algorithm2.compute();
        algorithm2.close();
        algorithm2.closeFactory();
        algorithm2 = null;

      }

      // Refresh caches for metadata handlers
      new MetadataServiceJpa().refreshCaches();

    } catch (Exception e) {
      throw e;
    } finally {
      historyService.close();
    }

  }

  /* see superclass */
  @Override
  public void computeTreePositions() throws Exception {

    try {
      Logger.getLogger(getClass()).info("Computing tree positions");
      treePosAlgorithm.setCycleTolerant(false);
      treePosAlgorithm.setIdType(IdType.CONCEPT);
      // some terminologies may have cycles, allow these for now.
      treePosAlgorithm.setCycleTolerant(true);
      treePosAlgorithm.setComputeSemanticType(true);
      treePosAlgorithm.setTerminology(terminology);
      treePosAlgorithm.setVersion(version);
      treePosAlgorithm.reset();
      treePosAlgorithm.compute();
      treePosAlgorithm.close();
    } catch (CancelException e) {
      Logger.getLogger(getClass()).info("Cancel request detected");
      throw new CancelException("Tree position computation cancelled");
    }

  }

  /* see superclass */
  @Override
  public void computeTransitiveClosures() throws Exception {
    Logger.getLogger(getClass()).info(
        "  Compute transitive closure from  " + terminology + "/" + version);
    try {
      transClosureAlgorithm.setCycleTolerant(false);
      transClosureAlgorithm.setIdType(IdType.CONCEPT);
      transClosureAlgorithm.setTerminology(terminology);
      transClosureAlgorithm.setVersion(version);
      transClosureAlgorithm.reset();
      transClosureAlgorithm.compute();
      transClosureAlgorithm.close();

      // Compute label sets - after transitive closure
      // for each subset, compute the label set
      for (final Subset subset : getConceptSubsets(terminology, version,
          Branch.ROOT).getObjects()) {
        final ConceptSubset conceptSubset = (ConceptSubset) subset;
        if (conceptSubset.isLabelSubset()) {
          Logger.getLogger(getClass()).info(
              "  Create label set for subset = " + subset);

          labelSetAlgorithm.setSubset(conceptSubset);
          labelSetAlgorithm.compute();
          labelSetAlgorithm.close();
        }
      }
    } catch (CancelException e) {
      Logger.getLogger(getClass()).info("Cancel request detected");
      throw new CancelException("Tree position computation cancelled");
    }
  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    // do nothing
  }

  /**
   * Fires a {@link ProgressEvent}.
   *
   * @param pct percent done
   * @param note progress note
   * @throws Exception the exception
   */
  public void fireProgressEvent(int pct, String note) throws Exception {
    ProgressEvent pe = new ProgressEvent(this, pct, pct, note);
    for (int i = 0; i < listeners.size(); i++) {
      listeners.get(i).updateProgress(pe);
    }
    logInfo("    " + pct + "% " + note);
  }

  /* see superclass */
  @Override
  public void addProgressListener(ProgressListener l) {
    listeners.add(l);
  }

  /* see superclass */
  @Override
  public void removeProgressListener(ProgressListener l) {
    listeners.remove(l);
  }

  /* see superclass */
  @Override
  public void cancel() throws Exception {
    // cancel any currently running local algorithms
    treePosAlgorithm.cancel();
    transClosureAlgorithm.cancel();
    labelSetAlgorithm.cancel();

    // invoke superclass cancel
    super.cancel();
  }

  /* see superclass */
  @Override
  public void close() throws Exception {
    super.close();
  }

}
