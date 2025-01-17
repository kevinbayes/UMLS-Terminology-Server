/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers.content;

import com.wci.umls.server.helpers.ResultList;
import com.wci.umls.server.model.content.ComponentHasAttributesAndName;
import com.wci.umls.server.model.content.Subset;
import com.wci.umls.server.model.content.SubsetMember;

/**
 * Represents a sortable list of {@link SubsetMember}
 */
public interface SubsetMemberList
    extends
    ResultList<SubsetMember<? extends ComponentHasAttributesAndName, ? extends Subset>> {
  // nothing extra, a simple wrapper for easy serialization
}
