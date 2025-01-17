/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import java.util.Map;

import com.wci.umls.server.model.meta.TermType;

/**
 * Represents an ordered list of {@link TermType}s for use in computing atom
 * ranks.
 */
public interface PrecedenceList extends HasTerminology, HasLastModified {

  /**
   * Returns the name.
   * 
   * @return the name
   */
  public String getName();

  /**
   * Sets the name.
   * 
   * @param name the name
   */
  public void setName(String name);

  /**
   * Sets the terminology, term type tuples.
   * 
   * @param precedence the terminology, term type tuples
   */
  public void setPrecedence(KeyValuePairList precedence);

  /**
   * Returns the terminology, term type tuples.
   * 
   * @return the terminology, term type tuples
   */
  public KeyValuePairList getPrecedence();

  /**
   * Adds the terminology, term type tuple to the end of the list.
   *
   * @param terminology the terminology
   * @param termType the term type
   */
  public void addTerminologyTermType(String terminology, String termType);

  /**
   * Removes the terminology, term type tuplefrom the list.
   *
   * @param terminology the terminology
   * @param termType the term type
   */
  public void removeTerminologyTermType(String terminology, String termType);

  /**
   * Returns the branch.
   *
   * @return the branch
   */
  public String getBranch();

  /**
   * Sets the branch.
   *
   * @param branch the branch
   */
  public void setBranch(String branch);

  /**
   * Returns the term type rank map.
   *
   * @return the term type rank map
   */
  public Map<String, String> getTermTypeRankMap();

  /**
   * Returns the terminology rank map.
   *
   * @return the terminology rank map
   */
  public Map<String, String> getTerminologyRankMap();
}
