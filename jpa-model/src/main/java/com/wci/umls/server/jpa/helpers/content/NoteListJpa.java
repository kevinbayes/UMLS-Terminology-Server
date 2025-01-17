/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers.content;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.Note;
import com.wci.umls.server.helpers.NoteList;
import com.wci.umls.server.helpers.content.RelationshipList;
import com.wci.umls.server.jpa.content.AbstractNote;

/**
 * JAXB enabled implementation of {@link RelationshipList}.
 */
@XmlRootElement(name = "noteList")
public class NoteListJpa
    extends
    AbstractResultList<Note>
    implements NoteList {

  /* see superclass */
  @Override
  @XmlElement(type = AbstractNote.class, name = "notes")
  public List<Note> getObjects() {
    return super.getObjectsTransient();
  }

}
