/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.content;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.LongBridge;

import com.wci.umls.server.jpa.helpers.MapKeyValueToCsvBridge;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomRelationship;
import com.wci.umls.server.model.content.Relationship;

/**
 * JPA and JAXB enabled implementation of {@link AtomRelationship}.
 */
@Entity
@Table(name = "atom_relationships", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "version", "id"
}))
@Audited
@Indexed
@XmlRootElement(name = "atomRelationship")
public class AtomRelationshipJpa extends AbstractRelationship<Atom, Atom>
    implements AtomRelationship {

  /**  The rela map. */
  @Transient
  private final Set<String> relaMap = new HashSet<>(Arrays.asList(new String[] {
      "transliterated_form_of", "british_form_of", "has_alias",
      "permuted_term_of", "sort_version_of", "common_name_of",
      "mth_british_form_of", "plain_text_form_of", "expanded_form_of",
      "mth_has_plain_text_form", "mth_has_xml_form", "mth_expanded_form_of",
      "entry_version_of", "translation_of"
  }));

  /** The from atom. */
  @ManyToOne(targetEntity = AtomJpa.class, optional = false)
  @JoinColumn(nullable = false)
  private Atom from;

  /** the to atom. */
  @ManyToOne(targetEntity = AtomJpa.class, optional = false)
  @JoinColumn(nullable = false)
  private Atom to;

  /** The alternate terminology ids. */
  @ElementCollection
  @MapKeyColumn(length = 100)
  @Column(nullable = true, length = 100)
  private Map<String, String> alternateTerminologyIds;

  /**
   * Instantiates an empty {@link AtomRelationshipJpa}.
   */
  public AtomRelationshipJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link AtomRelationshipJpa} from the specified parameters.
   *
   * @param relationship the concept relationship
   * @param collectionCopy the deep copy
   */
  public AtomRelationshipJpa(AtomRelationship relationship,
      boolean collectionCopy) {
    super(relationship, collectionCopy);
    to = relationship.getTo();
    from = relationship.getFrom();
    if (collectionCopy) {
      alternateTerminologyIds =
          new HashMap<>(relationship.getAlternateTerminologyIds());
    }
  }

  /* see superclass */
  @Override
  @XmlTransient
  public Atom getFrom() {
    return from;
  }

  /* see superclass */
  @Override
  public void setFrom(Atom component) {
    this.from = component;
  }

  /**
   * Returns the from id. For JAXB.
   *
   * @return the from id
   */
  @FieldBridge(impl = LongBridge.class)
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public Long getFromId() {
    return from == null ? null : from.getId();
  }

  /**
   * Sets the from id.
   *
   * @param id the from id
   */
  public void setFromId(Long id) {
    if (from == null) {
      from = new AtomJpa();
    }
    from.setId(id);
  }

  /**
   * Returns the from terminology id.
   *
   * @return the from terminology id
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getFromTerminologyId() {
    return from == null ? null : from.getTerminologyId();
  }

  /**
   * Sets the from terminology id.
   *
   * @param terminologyId the from terminology id
   */
  public void setFromTerminologyId(String terminologyId) {
    if (from == null) {
      from = new AtomJpa();
    }
    from.setTerminologyId(terminologyId);
  }

  /**
   * Returns the from name. For JAXB.
   *
   * @return the from name
   */
  @Fields({
      @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO, analyzer = @Analyzer(definition = "noStopWord")),
      @Field(name = "fromNameSort", index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  })
  public String getFromName() {
    return from == null ? null : from.getName();
  }

  /**
   * Sets the from name.
   *
   * @param name the from name
   */
  public void setFromName(String name) {
    if (from == null) {
      from = new AtomJpa();
    }
    from.setName(name);
  }

  /* see superclass */
  @Override
  @XmlTransient
  public Atom getTo() {
    return to;
  }

  /**
   * Returns the to id.
   *
   * @return the to id
   */
  @FieldBridge(impl = LongBridge.class)
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public Long getToId() {
    return to == null ? null : to.getId();
  }

  /**
   * Sets the to id.
   *
   * @param id the to id
   */
  public void setToId(Long id) {
    if (to == null) {
      to = new AtomJpa();
    }
    to.setId(id);
  }

  /**
   * Returns the to terminology id.
   *
   * @return the to terminology id
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getToTerminologyId() {
    return to == null ? null : to.getTerminologyId();
  }

  /**
   * Sets the to terminology id.
   *
   * @param terminologyId the to terminology id
   */
  public void setToTerminologyId(String terminologyId) {
    if (to == null) {
      to = new AtomJpa();
    }
    to.setTerminologyId(terminologyId);
  }

  /**
   * Returns the to name.
   *
   * @return the to name
   */
  @Fields({
      @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO),
      @Field(name = "toNameSort", index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  })
  public String getToName() {
    return to == null ? null : to.getName();
  }

  /**
   * Sets the to name.
   *
   * @param name the to name
   */
  public void setToName(String name) {
    if (to == null) {
      to = new AtomJpa();
    }
    to.setName(name);
  }

  /* see superclass */
  @Override
  public void setTo(Atom component) {
    this.to = component;
  }

  /* see superclass */
  @Override
  @FieldBridge(impl = MapKeyValueToCsvBridge.class)
  @Field(name = "alternateTerminologyIds", index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public Map<String, String> getAlternateTerminologyIds() {
    if (alternateTerminologyIds == null) {
      alternateTerminologyIds = new HashMap<>(2);
    }
    return alternateTerminologyIds;
  }

  /* see superclass */
  @Override
  public void setAlternateTerminologyIds(
    Map<String, String> alternateTerminologyIds) {
    this.alternateTerminologyIds = alternateTerminologyIds;
  }

  /* see superclass */
  @Override
  public Relationship<Atom, Atom> createInverseRelationship(
    Relationship<Atom, Atom> relationship, String inverseRelType,
    String inverseAdditionalRelType) throws Exception {

    final AtomRelationship inverseRelationship =
        new AtomRelationshipJpa((AtomRelationship) relationship, false);

    return populateInverseRelationship(relationship, inverseRelationship,
        inverseRelType, inverseAdditionalRelType);
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((from == null) ? 0 : from.hashCode());
    result = prime * result + ((to == null) ? 0 : to.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    AtomRelationshipJpa other = (AtomRelationshipJpa) obj;
    if (from == null) {
      if (other.from != null)
        return false;
    } else if (!from.equals(other.from))
      return false;
    if (to == null) {
      if (other.to != null)
        return false;
    } else if (!to.equals(other.to))
      return false;
    return true;
  }

  @Override
  @XmlTransient
  public boolean isShortFormLongForm() {
    return getRelationshipType().equals("SY")
        && relaMap.contains(getAdditionalRelationshipType());
  }

  // Use superclass toString()

}
