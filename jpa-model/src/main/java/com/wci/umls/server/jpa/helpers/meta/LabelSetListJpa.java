/*
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers.meta;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.meta.LabelSetList;
import com.wci.umls.server.jpa.meta.LabelSetJpa;
import com.wci.umls.server.model.meta.LabelSet;

/**
 * JAXB enabled implementation of {@link LabelSetList}.
 */
@XmlRootElement(name = "labelSetList")
public class LabelSetListJpa extends AbstractResultList<LabelSet> implements
    LabelSetList {

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.helpers.AbstractResultList#getObjects()
   */
  @Override
  @XmlElement(type = LabelSetJpa.class, name = "name")
  public List<LabelSet> getObjects() {
    return super.getObjectsTransient();
  }

}