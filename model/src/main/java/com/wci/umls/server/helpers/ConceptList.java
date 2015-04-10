/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import com.wci.umls.server.model.content.Concept;

/**
 * Represents a sortable list of {@link Concept}
 */
public interface ConceptList extends ResultList<Concept> {
  // nothing extra, a simple wrapper for easy serialization
}
