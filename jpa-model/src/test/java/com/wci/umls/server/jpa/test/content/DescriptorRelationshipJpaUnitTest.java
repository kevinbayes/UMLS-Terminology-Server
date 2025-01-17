/*
 * Copyright 2 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.test.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.CopyConstructorTester;
import com.wci.umls.server.helpers.EqualsHashcodeTester;
import com.wci.umls.server.helpers.GetterSetterTester;
import com.wci.umls.server.helpers.ProxyTester;
import com.wci.umls.server.helpers.XmlSerializationTester;
import com.wci.umls.server.jpa.ModelUnitSupport;
import com.wci.umls.server.jpa.content.AttributeJpa;
import com.wci.umls.server.jpa.content.DescriptorJpa;
import com.wci.umls.server.jpa.content.DescriptorRelationshipJpa;
import com.wci.umls.server.jpa.helpers.NullableFieldTester;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.Descriptor;
import com.wci.umls.server.model.content.DescriptorRelationship;

/**
 * Unit testing for {@link DescriptorRelationshipJpa}.
 */
public class DescriptorRelationshipJpaUnitTest extends ModelUnitSupport {

  /** The model object to test. */
  private DescriptorRelationshipJpa object;

  /** test fixture */
  private Descriptor descriptor1;

  /** test fixture */
  private Descriptor descriptor2;

  /** The map fixture 1. */
  private Map<String, String> map1;

  /** The map fixture 2. */
  private Map<String, String> map2;

  /**
   * Setup class.
   */
  @BeforeClass
  public static void setupClass() {
    // do nothing
  }

  /**
   * Setup.
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
    object = new DescriptorRelationshipJpa();

    map1 = new HashMap<>();
    map1.put("1", "1");
    map2 = new HashMap<>();
    map2.put("2", "2");
    map2.put("3", "4");

    ProxyTester tester = new ProxyTester(new DescriptorJpa());
    tester.proxy(Map.class, 1, map1);
    tester.proxy(Map.class, 2, map2);
    descriptor1 = (DescriptorJpa) tester.createObject(1);
    descriptor2 = (DescriptorJpa) tester.createObject(2);

    object.setFrom(descriptor1);
    object.setTo(descriptor2);
  }

  /**
   * Test getter and setter methods of model object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelGetSet() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());
    GetterSetterTester tester = new GetterSetterTester(object);
    tester.exclude("fromId");
    tester.exclude("fromTerminology");
    tester.exclude("fromVersion");
    tester.exclude("fromTerminologyId");
    tester.exclude("fromName");
    tester.exclude("toId");
    tester.exclude("toTerminology");
    tester.exclude("toVersion");
    tester.exclude("toTerminologyId");
    tester.exclude("toName");
    tester.exclude("type");
    tester.exclude("name");
    tester.test();
  }

  /**
   * Test equals and hascode methods.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelEqualsHashcode() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());
    EqualsHashcodeTester tester = new EqualsHashcodeTester(object);
    tester.include("suppressible");
    tester.include("obsolete");
    tester.include("branch");
    tester.include("terminology");
    tester.include("terminologyId");
    tester.include("assertedDirection");
    tester.include("additionalRelationshipType");
    tester.include("group");
    tester.include("inferred");
    tester.include("relationshipType");
    tester.include("stated");
    tester.include("hierarchical");
    tester.include("to");
    tester.include("from");
    tester.exclude("toTerminologyId");
    tester.exclude("fromTerminologyId");

    tester.proxy(Descriptor.class, 1, descriptor1);
    tester.proxy(Descriptor.class, 2, descriptor2);
    tester.proxy(Map.class, 1, map1);
    tester.proxy(Map.class, 2, map2);

    assertTrue(tester.testIdentityFieldEquals());
    assertTrue(tester.testNonIdentityFieldEquals());
    assertTrue(tester.testIdentityFieldNotEquals());
    assertTrue(tester.testIdentityFieldHashcode());
    assertTrue(tester.testNonIdentityFieldHashcode());
    assertTrue(tester.testIdentityFieldDifferentHashcode());
  }

  /**
   * Test copy constructor.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelCopy() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());
    CopyConstructorTester tester = new CopyConstructorTester(object);
    tester.proxy(Descriptor.class, 1, descriptor1);
    tester.proxy(Descriptor.class, 2, descriptor2);
    tester.proxy(Map.class, 1, map1);
    tester.proxy(Map.class, 2, map2);
    assertTrue(tester.testCopyConstructorCollection(DescriptorRelationship.class));
  }

  /**
   * Test deep copy constructor.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelCollectionCopy() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());

    DescriptorRelationship rel = new DescriptorRelationshipJpa();
    ProxyTester tester = new ProxyTester(rel);
    tester.proxy(Map.class, 1, map1);
    rel = (DescriptorRelationship) tester.createObject(1);

    ProxyTester tester2 = new ProxyTester(new DescriptorJpa());
    tester.proxy(Map.class, 1, map1);
    tester.proxy(Map.class, 2, map2);
    Descriptor fromDescriptor = (Descriptor) tester2.createObject(1);
    Descriptor toDescriptor = (Descriptor) tester2.createObject(2);

    ProxyTester tester3 = new ProxyTester(new AttributeJpa());
    Attribute att = (Attribute) tester3.createObject(1);

    rel.setFrom(fromDescriptor);
    rel.setTo(toDescriptor);
    rel.getAttributes().add(att);

    DescriptorRelationship rel2 = new DescriptorRelationshipJpa(rel, false);
    assertEquals(0, rel2.getAttributes().size());

    DescriptorRelationship rel3 = new DescriptorRelationshipJpa(rel, true);
    assertEquals(1, rel3.getAttributes().size());
    assertEquals(att, rel3.getAttributes().iterator().next());

  }

  /**
   * Test XML serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelXmlSerialization() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());
    XmlSerializationTester tester = new XmlSerializationTester(object);
    // The proxy descriptors can have only "id" and "term" set due to xml
    // transient
    Descriptor descriptor1 = new DescriptorJpa();
    descriptor1.setId(1L);
    descriptor1.setName("1");
    Descriptor descriptor2 = new DescriptorJpa();
    descriptor2.setId(2L);
    descriptor2.setName("2");

    tester.proxy(Descriptor.class, 1, descriptor1);
    tester.proxy(Descriptor.class, 2, descriptor2);
    tester.proxy(Map.class, 1, map1);
    tester.proxy(Map.class, 2, map2);
    assertTrue(tester.testXmlSerialization());
  }

  /**
   * Test xml transient fields
   *
   * @throws Exception the exception
   */
  @Test
  public void testXmlTransient() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());

    String xml = ConfigUtility.getStringForGraph(object);
    assertTrue(xml.contains("<fromId>"));
    assertTrue(xml.contains("<fromName>"));
    assertTrue(xml.contains("<toId>"));
    assertTrue(xml.contains("<toName>"));
    assertFalse(xml.contains("<from>"));
    assertFalse(xml.contains("<to>"));

  }

  /**
   * Test not null fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelNotNullField() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());
    NullableFieldTester tester = new NullableFieldTester(object);
    tester.include("timestamp");
    tester.include("lastModified");
    tester.include("lastModifiedBy");
    tester.include("suppressible");
    tester.include("obsolete");
    tester.include("published");
    tester.include("publishable");
    tester.include("terminology");
    tester.include("terminologyId");
    tester.include("version");
    tester.include("assertedDirection");
    tester.include("relationshipType");
    tester.include("inferred");
    tester.include("stated");
    tester.include("hierarchical");
    tester.include("from");
    tester.include("to");
    tester.include("workflowStatus");
    assertTrue(tester.testNotNullFields());
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
