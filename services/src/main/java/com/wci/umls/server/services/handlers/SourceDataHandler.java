/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.services.handlers;

import com.wci.umls.server.SourceData;
import com.wci.umls.server.algo.Algorithm;
import com.wci.umls.server.helpers.Configurable;

/**
 * Generically represents a sour.
 */
public interface SourceDataHandler extends Algorithm, Configurable {

  /**
   * Sets the source data.
   *
   * @param sourceData the source data
   */
  public void setSourceData(SourceData sourceData);

  
  
}