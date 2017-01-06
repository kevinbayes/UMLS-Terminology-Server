/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.release;

import java.util.Properties;

import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.jpa.algo.AbstractAlgorithm;

/**
 * Algorithm to write the special NCI-META files.
 */
public class WriteNciMetaFilesAlgorithm extends AbstractAlgorithm {

  /**
   * Instantiates an empty {@link WriteNciMetaFilesAlgorithm}.
   *
   * @throws Exception the exception
   */
  public WriteNciMetaFilesAlgorithm() throws Exception {
    // TODO Auto-generated constructor stub
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    // TODO Auto-generated method stub

  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    // TODO Auto-generated method stub

  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    checkRequiredProperties(new String[] {
        ""
    }, p);
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return ConfigUtility.getNameFromClass(getClass());
  }
}