/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.rest;

import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.rest.client.ContentClientRest;
import com.wci.umls.server.rest.client.ProjectClientRest;
import com.wci.umls.server.rest.client.SecurityClientRest;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Superclass for "Project Service REST" Test Cases.
 */
@Ignore
public class ProjectServiceRestTest extends IntegrationUnitSupport {

  /** The service. */
  protected static ProjectClientRest projectService;
  
  /** The content service. */
  protected static ContentClientRest contentService;

  /** The security service. */
  protected static SecurityClientRest securityService;

  /** The properties. */
  protected static Properties properties;

  /** The test password. */
  protected static String testUser;

  /** The test password. */
  protected static String testPassword;

  /** The test password. */
  protected static String adminUser;

  /** The test password. */
  protected static String adminPassword;

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {

    // instantiate properties
    properties = ConfigUtility.getConfigProperties();

    // instantiate required services
    projectService = new ProjectClientRest(properties);
    contentService = new ContentClientRest(properties);
    securityService = new SecurityClientRest(properties);

    // test run.config.ts has viewer user
    testUser = properties.getProperty("viewer.user");
    testPassword = properties.getProperty("viewer.password");

    // test run.config.ts has admin user
    adminUser = properties.getProperty("admin.user");
    adminPassword = properties.getProperty("admin.password");

    if (testUser == null || testUser.isEmpty()) {
      throw new Exception("Test prerequisite: viewer.user must be specified");
    }
    if (testPassword == null || testPassword.isEmpty()) {
      throw new Exception(
          "Test prerequisite: viewer.password must be specified");
    }
    if (adminUser == null || adminUser.isEmpty()) {
      throw new Exception("Test prerequisite: admin.user must be specified");
    }
    if (adminPassword == null || adminPassword.isEmpty()) {
      throw new Exception("Test prerequisite: admin.password must be specified");
    }

  }

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {

    /**
     * Prerequisites
     */

  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    // do nothing
  }

  /**
   * Teardown class.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void teardownClass() throws Exception {
    // do nothing
  }

}
