/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.helpers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.PfsParameter;
import com.wci.umls.server.helpers.ResultList;
import com.wci.umls.server.helpers.SearchResult;
import com.wci.umls.server.helpers.SearchResultList;
import com.wci.umls.server.jpa.content.CodeJpa;
import com.wci.umls.server.jpa.content.ConceptJpa;
import com.wci.umls.server.jpa.content.DescriptorJpa;
import com.wci.umls.server.jpa.content.LexicalClassJpa;
import com.wci.umls.server.jpa.content.StringClassJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.services.ContentService;

/**
 * Helper testing class for PfsParameter concept tests.
 */
public class PfsParameterForComponentTest {

  /**
   * Test sort.
   *
   * @param results the results
   * @param pfs the pfs
   * @param sortClass the sort class
   * @return true, if successful
   * @throws Exception the exception
   */

  @SuppressWarnings("unchecked")
  public static boolean testSort(ResultList<?> results, PfsParameter pfs,
    Class<?> sortClass) throws Exception {
    // instantiate content service
    ContentService contentService = new ContentServiceJpa();

    Field field = null;
    Class<?> clazz = sortClass;

    do {
      try {
        field = clazz.getDeclaredField(pfs.getSortField());
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    } while (clazz != null && field == null);

    if (field == null)
      throw new Exception("Could not retrieve field " + pfs.getSortField()
          + " for " + sortClass.getName());

    field.setAccessible(true);

    Object prevValue = null;
    Object thisValue = null;

    List<Object> components = new ArrayList<>();

    for (final Object result : results.getObjects()) {
      Object obj = null;
      SearchResult sr = (SearchResult) result;
      if (LexicalClassJpa.class.isAssignableFrom(sortClass)) {
        obj =
            contentService.getLexicalClass(sr.getTerminologyId(),
                sr.getTerminology(), sr.getVersion(), Branch.ROOT);
      } else if (StringClassJpa.class.isAssignableFrom(sortClass)) {
        obj =
            contentService.getStringClass(sr.getTerminologyId(),
                sr.getTerminology(), sr.getVersion(), Branch.ROOT);
      } else if (ConceptJpa.class.isAssignableFrom(sortClass)) {
        obj =
            contentService.getConcept(sr.getTerminologyId(),
                sr.getTerminology(), sr.getVersion(), Branch.ROOT);
      } else if (DescriptorJpa.class.isAssignableFrom(sortClass)) {
        obj =
            contentService.getDescriptor(sr.getTerminologyId(),
                sr.getTerminology(), sr.getVersion(), Branch.ROOT);
      } else if (CodeJpa.class.isAssignableFrom(sortClass)) {
        obj =
            contentService.getCode(sr.getTerminologyId(), sr.getTerminology(),
                sr.getVersion(), Branch.ROOT);
      } else {
        obj = result;
      }
      if (obj == null) {
        throw new Exception("Unexpected null object for " + sortClass + ", "
            + sr.getTerminologyId() + ", " + sr.getTerminology() + ", "
            + sr.getVersion());
      }
      components.add(obj);
    }

    @SuppressWarnings("rawtypes")
    Comparator comparator = null;

    switch (field.getType().getSimpleName()) {
      case "int":
        comparator = new Comparator<Integer>() {
          @Override
          public int compare(Integer u1, Integer u2) {
            return u1.compareTo(u2);
          }
        };
        break;
      case "Long":
        comparator = new Comparator<Long>() {
          @Override
          public int compare(Long u1, Long u2) {
            return u1.compareTo(u2);
          }
        };
        break;
      case "String":
        comparator = new Comparator<String>() {
          @Override
          public int compare(String u1, String u2) {
            return u1.compareTo(u2);
          }
        };

        break;
      default:
        Logger.getLogger(PfsParameterForComponentTest.class).info(
            "  Concept does not support testing sorting on field type "
                + field.getType().getSimpleName());
        return false;
    }

    for (final Object c : components) {

      thisValue = field.get(c);

      // if not the first value
      if (prevValue != null) {

        // test ascending case
        if (pfs.isAscending() && (comparator.compare(thisValue, prevValue) < 0)) {
          return false;
        }

        // test descending case
        else if (!pfs.isAscending()
            && comparator.compare(prevValue, thisValue) < 0) {
          return false;
        }
      }

      prevValue = thisValue;

    }
    return true;
  }

  /**
   * Test paging.
   *
   * @param results the results
   * @param fullResults the full results
   * @param pfs the pfs
   * @return true, if successful
   */
  public static boolean testPaging(ResultList<?> results,
    ResultList<?> fullResults, PfsParameter pfs) {
    // check results size, must be less than or equal to page size

    int page =
        (int) (Math.floor(pfs.getStartIndex() / pfs.getMaxResults()) + 1);
    int pageSize = pfs.getMaxResults();

    if (results.size() > pageSize)
      return false;

    // check bounds
    if ((page - 1) * pageSize < 0)
      return false;
    if ((page - 1) * pageSize + results.size() > fullResults
        .getTotalCount())
      return false;

    // check paging
    for (int i = 0; i < results.size(); i++) {
      if (!results.getObjects().get(i)
          .equals(fullResults.getObjects().get((page - 1) * pageSize + i)))
        return false;
    }

    return true;
  }

  /**
   * Test query.
   *
   * @param results the results
   * @param query the query
   * @return true, if successful
   */
  public static boolean testQuery(SearchResultList results, String query) {

    // another interesting case, would need to extract indexed fields and
    // perform
    // checks on them
    return true;
  }

  /**
   * Test query restriction.
   *
   * @param results the results
   * @param queryRestriction the query restriction
   * @return true, if successful
   */
  public static boolean testQueryRestriction(SearchResultList results,
    String queryRestriction) {
    return true;
  }

}
