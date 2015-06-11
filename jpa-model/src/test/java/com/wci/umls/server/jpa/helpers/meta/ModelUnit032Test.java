/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers.meta;

import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.CopyConstructorTester;
import com.wci.umls.server.helpers.EqualsHashcodeTester;
import com.wci.umls.server.helpers.GetterSetterTester;
import com.wci.umls.server.helpers.XmlSerializationTester;
import com.wci.umls.server.jpa.helpers.NullableFieldTester;
import com.wci.umls.server.jpa.meta.RelationshipTypeJpa;
import com.wci.umls.server.model.meta.RelationshipType;

/**
 * Unit testing for {@link RelationshipTypeJpa}.
 */
public class ModelUnit032Test {

  /** The model object to test. */
  private RelationshipTypeJpa object;

  /** The rel. */
  private RelationshipTypeJpa rel;

  /** The rel2. */
  private RelationshipTypeJpa rel2;

  /**
   * Setup class.
   */
  @BeforeClass
  public static void setupClass() {
    // do nothing
  }

  /**
   * Setup.
   */
  @Before
  public void setup() {
    object = new RelationshipTypeJpa();
    rel = new RelationshipTypeJpa();
    rel2 = new RelationshipTypeJpa();
    rel.setId(1L);
    rel.setAbbreviation("1");
    rel.setExpandedForm("1");
    rel2.setId(2L);
    rel2.setAbbreviation("2");
    rel2.setExpandedForm("2");
    rel.setInverse(rel2);
    rel2.setInverse(rel);
  }

  /**
   * Test getter and setter methods of model object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelGetSet032() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelGetSet032");
    GetterSetterTester tester = new GetterSetterTester(object);
    tester.exclude("inverseAbbreviation");
    tester.exclude("inverseId");
    tester.test();
  }

  /**
   * Test equals and hascode methods.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelEqualsHashcode032() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelEqualsHashcode032");
    EqualsHashcodeTester tester = new EqualsHashcodeTester(object);
    tester.include("abbreviation");
    tester.include("expandedForm");
    tester.include("terminology");
    tester.include("version");
    tester.include("publishable");
    tester.include("published");

    assertTrue(tester.testIdentitiyFieldEquals());
    assertTrue(tester.testNonIdentitiyFieldEquals());
    assertTrue(tester.testIdentityFieldNotEquals());
    assertTrue(tester.testIdentitiyFieldHashcode());
    assertTrue(tester.testNonIdentitiyFieldHashcode());
    assertTrue(tester.testIdentityFieldDifferentHashcode());
  }

  /**
   * Test copy constructor.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelCopy032() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelCopy032");
    CopyConstructorTester tester = new CopyConstructorTester(object);
    tester.proxy(RelationshipType.class, 1, rel);
    tester.proxy(RelationshipType.class, 2, rel2);
    assertTrue(tester.testCopyConstructor(RelationshipType.class));
  }

  /**
   * Test XML serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelXmlSerialization032() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelXmlSerialization032");
    XmlSerializationTester tester = new XmlSerializationTester(object);
    tester.proxy(RelationshipType.class, 1, rel);
    tester.proxy(RelationshipType.class, 2, rel2);
    assertTrue(tester.testXmlSerialization());
  }

  /**
   * Test not null fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelNotNullField032() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelNotNullField032");
    NullableFieldTester tester = new NullableFieldTester(object);
    tester.include("abbreviation");
    tester.include("expandedForm");
    tester.include("terminology");
    tester.include("version");
    tester.include("publishable");
    tester.include("published");
    tester.include("timestamp");
    tester.include("lastModified");
    tester.include("lastModifiedBy");

    assertTrue(tester.testNotNullFields());
  }

  /**
   * Test concept reference in XML serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelXmlTransient032() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelXmlTransient032");
    String xml = ConfigUtility.getStringForGraph(rel);
    assertTrue(xml.contains("<inverseId>"));
    assertTrue(xml.contains("<inverseAbbreviation>"));
    Assert.assertFalse(xml.contains("<inverse>"));

  }

  /**
   * Teardown.
   */
  @After
  public void teardown() {
    // do nothing
  }

  /**
   * Teardown class.
   */
  @AfterClass
  public static void teardownClass() {
    // do nothing
  }

}
