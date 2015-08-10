/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.SearchResult;
import com.wci.umls.server.helpers.SearchResultList;

/**
 * JAXB-enabled implementation of {@link SearchResultList}.
 */
@XmlRootElement(name = "searchResultList")
public class SearchResultListJpa extends AbstractResultList<SearchResult>
    implements SearchResultList {

  /* see superclass */
  @Override
  @XmlElement(type = SearchResultJpa.class, name = "searchResult")
  public List<SearchResult> getObjects() {
    return super.getObjectsTransient();
  }

  /* see superclass */
  @Override
  public String toString() {
    return "SearchResultListJpa [searchResults=" + getObjects()
        + ", getCount()=" + getCount() + "]";
  }

}
