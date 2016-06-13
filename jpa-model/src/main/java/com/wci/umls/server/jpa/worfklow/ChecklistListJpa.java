/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.worfklow;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.model.workflow.Checklist;
import com.wci.umls.server.model.workflow.ChecklistList;


/**
 * JAXB enabled implementation of {@link ChecklistList}.
 */
@XmlRootElement(name = "checklistList")
public class ChecklistListJpa extends AbstractResultList<Checklist>
    implements ChecklistList {

  /* see superclass */
  @Override
  @XmlElement(type = ChecklistJpa.class, name = "checklists")
  public List<Checklist> getObjects() {
    return super.getObjectsTransient();
  }

}