/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.mojo;

import java.io.File;
import java.util.Date;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.wci.umls.server.SourceData;
import com.wci.umls.server.SourceDataFile;
import com.wci.umls.server.jpa.SourceDataFileJpa;
import com.wci.umls.server.jpa.SourceDataJpa;
import com.wci.umls.server.jpa.services.SourceDataServiceJpa;
import com.wci.umls.server.jpa.services.handlers.Rf2SnapshotSourceDataHandler;
import com.wci.umls.server.services.SourceDataService;
import com.wci.umls.server.services.handlers.ExceptionHandler;

/**
 * Used to for sample data load to get SNOMED into a database associated with a
 * source data object.
 * 
 * See admin/pom.xml for a sample execution.
 */
@Mojo(name = "loadsd-rf2-snapshot", defaultPhase = LifecyclePhase.PACKAGE)
public class Rf2SnapshotSourceDataLoaderMojo extends AbstractLoaderMojo {

  /**
   * Name of terminology to be loaded.
   */
  @Parameter
  private String terminology;

  /**
   * The version.
   */
  @Parameter
  private String version;

  /**
   * create or update mode.
   */
  @Parameter
  private String mode;

  /**
   * Input directory.
   */
  @Parameter
  private String inputDir;

  /**
   * Executes the plugin.
   *
   * @throws MojoExecutionException the mojo execution exception
   * @throws MojoFailureException the mojo failure exception
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Starting sample data load");
    getLog().info("  terminology = " + terminology);
    getLog().info("  version = " + version);
    getLog().info("  mode = " + mode);
    getLog().info("  inputDir = " + inputDir);

    SourceDataService service = null;
    try {

      // Create DB
      if (mode != null && mode.equals("create")) {
        createDb(false);
      }

      // setup sample data

      service = new SourceDataServiceJpa();
      // As this is a sample loader and not an integration test,
      // we will use the JPA service layer directly.

      if (inputDir == null) {
        throw new Exception("Input directory not specified");
      }

      final File dir = new File(inputDir);
      if (!dir.exists()) {
        throw new Exception("Input directory does not exist");
      }

      final SourceDataFile sdFile = new SourceDataFileJpa();
      sdFile.setDirectory(true);
      sdFile.setLastModifiedBy("loader");
      sdFile.setName(dir.getName());
      sdFile.setPath(inputDir);
      sdFile.setSize(1000000L);
      sdFile.setTimestamp(new Date());
      service.addSourceDataFile(sdFile);
      getLog().info("    file = " + sdFile);

      // Create loader
      final Rf2SnapshotSourceDataHandler loader =
          new Rf2SnapshotSourceDataHandler();

      // Create and add the source data
      final SourceData sourceData = new SourceDataJpa();
      sourceData.setName(getName(terminology, version));
      sourceData.setDescription("Set of Rf2 files loaded from " + dir);
      sourceData.setLastModifiedBy("loader");
      sourceData.setHandler(loader.getName());
      sourceData.getSourceDataFiles().add(sdFile);
      service.addSourceData(sourceData);
      getLog().info("    source data = " + sourceData);

      service.updateSourceDataFile(sdFile);
      getLog().info("    file (with reference) = " + sdFile);

      // Now, invoke the loader
      loader.setSourceData(sourceData);

      loader.compute();
      loader.close();
      getLog().info("Done ...");

    } catch (Exception e) {
      // Send email if something went wrong
      try {
        ExceptionHandler.handleException(e, "Error loading RF2 source data");
      } catch (Exception e1) {
        e1.printStackTrace();
        throw new MojoFailureException(e.getMessage());
      }

    } finally {
      // Close service(s)
      if (service != null) {
        try {
          service.close();
        } catch (Exception e) {
          // n/a
        }
      }
    }

  }
}