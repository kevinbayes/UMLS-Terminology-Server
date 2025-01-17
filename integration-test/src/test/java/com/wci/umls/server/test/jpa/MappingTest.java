/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.jpa.content.MapSetJpa;
import com.wci.umls.server.jpa.content.MappingJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.jpa.services.handlers.DefaultComputePreferredNameHandler;
import com.wci.umls.server.model.content.MapSet;
import com.wci.umls.server.model.content.Mapping;
import com.wci.umls.server.model.meta.IdType;
import com.wci.umls.server.services.ContentService;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Integration testing for {@link DefaultComputePreferredNameHandler}.
 */
public class MappingTest extends IntegrationUnitSupport {

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
    // n/a
  }

  /**
   * /** Test normal use of the handler object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMappingNormalUse() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    // Add MapSet and Mapping
    ContentService contentService = new ContentServiceJpa();
    contentService.setLastModifiedBy("admin");
    contentService.setMolecularActionFlag(false);

    MapSet mapSet = new MapSetJpa();
    mapSet.setName("Test MapSet");
    mapSet.setMapType("CONCEPT");
    mapSet.setFromComplexity("fcomp");
    mapSet.setToComplexity("toComp");
    mapSet.setFromExhaustive("fromExh");
    mapSet.setToExhaustive("toExh");
    mapSet.setFromTerminology("SNOMEDCT");
    mapSet.setToTerminology("SNOMEDCT");
    mapSet.setFromVersion("20150131");
    mapSet.setToVersion("20150131");
    mapSet.setLastModifiedBy("dss");
    mapSet.setLastModified(new Date());
    mapSet.setTerminology("SNOMECT");
    mapSet.setVersion("20150131");
    mapSet.setTerminologyId("11111");
    mapSet.setTimestamp(new Date());

    mapSet = contentService.addMapSet(mapSet);
    Logger.getLogger(getClass()).info(mapSet);
    assertEquals(mapSet.getName(), "Test MapSet");
    Mapping mapping = new MappingJpa();
    mapping.setAdvice("advice");
    mapping.setFromTerminologyId("");
    mapping.setFromIdType(IdType.getIdType("CUI"));
    mapping.setToTerminologyId("");
    mapping.setToIdType(IdType.getIdType("CUI"));
    mapping.setRank("rank");
    mapping.setGroup("subset/group");
    mapping.setRule("rule");

    mapping.setLastModifiedBy("dss");
    mapping.setLastModified(new Date());
    mapping.setTerminology("SNOMECT");
    mapping.setVersion("20150131");
    mapping.setTerminologyId("11111");
    mapping.setTimestamp(new Date());
    mapping.setMapSet(mapSet);
    mapping = contentService.addMapping(mapping);

    contentService.removeMapping(mapping.getId());

    contentService.removeMapSet(mapSet.getId());

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
