/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers.meta;

import com.wci.umls.server.helpers.ResultList;
import com.wci.umls.server.model.meta.RelationshipType;

/**
 * Represents a sortable list of {@link RelationshipType}
 */
public interface RelationshipTypeList extends ResultList<RelationshipType> {
  // nothing extra, a simple wrapper for easy serialization
}
