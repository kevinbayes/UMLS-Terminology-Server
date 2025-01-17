/**
 * Copyright 2016 West Coast Informatics, LLC
 */
/*
 * 
 */
package com.wci.umls.server.jpa.meta;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;

import com.wci.umls.server.model.meta.SemanticType;

/**
 * JPA and JAXB enabled implementation of {@link SemanticType}.
 */
@Entity
@Table(name = "semantic_types")
@Audited
@XmlRootElement(name = "semanticType")
public class SemanticTypeJpa extends AbstractAbbreviation implements
    SemanticType {

  /** The definition. */
  @Column(nullable = false, length = 4000)
  private String definition;

  /** The example. */
  @Column(nullable = true, length = 4000)
  private String example;

  /** The type id. */
  @Column(nullable = false)
  private String typeId;

  /** The non human. */
  @Column(nullable = false)
  private boolean nonHuman;

  /** The tree number. */
  @Column(nullable = true, length = 4000)
  private String treeNumber;

  /** The usage note. */
  @Column(nullable = true, length = 4000)
  private String usageNote;

  /** The structural chemical. */
  @Column(nullable = false)
  private boolean structuralChemical;

  /** The functional chemical. */
  @Column(nullable = false)
  private boolean functionalChemical;

  /**
   * Instantiates an empty {@link SemanticTypeJpa}.
   */
  public SemanticTypeJpa() {
    super();
    setExpandedForm("n/a");
  }

  /**
   * Instantiates a {@link SemanticTypeJpa} from the specified parameters.
   *
   * @param sty the sty
   */
  public SemanticTypeJpa(SemanticType sty) {
    super(sty);
    definition = sty.getDefinition();
    example = sty.getExample();
    treeNumber = sty.getTreeNumber();
    typeId = sty.getTypeId();
    usageNote = sty.getUsageNote();
    nonHuman = sty.isNonHuman();
    structuralChemical = sty.isStructuralChemical();
    functionalChemical = sty.isFunctionalChemical();
  }

  /* see superclass */
  @Override
  public String getDefinition() {
    return definition;
  }

  /* see superclass */
  @Override
  public void setDefinition(String definition) {
    this.definition = definition;
  }

  /* see superclass */
  @Override
  public String getExample() {
    return example;
  }

  /* see superclass */
  @Override
  public void setExample(String example) {
    this.example = example;
  }

  /* see superclass */
  @Override
  public String getTypeId() {
    return typeId;
  }

  /* see superclass */
  @Override
  public void setTypeId(String typeId) {
    this.typeId = typeId;
  }

  /* see superclass */
  @Override
  public boolean isNonHuman() {
    return nonHuman;
  }

  /* see superclass */
  @Override
  public void setNonHuman(boolean nonHuman) {
    this.nonHuman = nonHuman;
  }

  /* see superclass */
  @Override
  public String getTreeNumber() {
    return treeNumber;
  }

  /* see superclass */
  @Override
  public void setTreeNumber(String treeNumber) {
    this.treeNumber = treeNumber;
  }

  /* see superclass */
  @Override
  public String getUsageNote() {
    return usageNote;
  }

  /* see superclass */
  @Override
  public void setUsageNote(String usageNote) {
    this.usageNote = usageNote;
  }

  /* see superclass */
  @XmlTransient
  @Override
  public boolean isChemical() {
    return isStructuralChemical() || isFunctionalChemical();
  }

  /* see superclass */
  @Override
  public boolean isStructuralChemical() {
    return structuralChemical;
  }

  /* see superclass */
  @Override
  public void setStructuralChemical(boolean structuralChemical) {
    this.structuralChemical = structuralChemical;
  }

  /* see superclass */
  @Override
  public boolean isFunctionalChemical() {
    return functionalChemical;
  }

  /* see superclass */
  @Override
  public void setFunctionalChemical(boolean functionalChemical) {
    this.functionalChemical = functionalChemical;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime * result + ((definition == null) ? 0 : definition.hashCode());
    result = prime * result + ((example == null) ? 0 : example.hashCode());
    result = prime * result + (nonHuman ? 1231 : 1237);
    result =
        prime * result + ((treeNumber == null) ? 0 : treeNumber.hashCode());
    result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
    result = prime * result + ((usageNote == null) ? 0 : usageNote.hashCode());
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
    SemanticTypeJpa other = (SemanticTypeJpa) obj;
    if (definition == null) {
      if (other.definition != null)
        return false;
    } else if (!definition.equals(other.definition))
      return false;
    if (example == null) {
      if (other.example != null)
        return false;
    } else if (!example.equals(other.example))
      return false;
    if (nonHuman != other.nonHuman)
      return false;

    if (treeNumber == null) {
      if (other.treeNumber != null)
        return false;
    } else if (!treeNumber.equals(other.treeNumber))
      return false;
    if (typeId == null) {
      if (other.typeId != null)
        return false;
    } else if (!typeId.equals(other.typeId))
      return false;
    if (usageNote == null) {
      if (other.usageNote != null)
        return false;
    } else if (!usageNote.equals(other.usageNote))
      return false;

    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "SemanticTypeJpa [definition=" + definition + ", example=" + example
        + ", typeId=" + typeId + ", nonHuman=" + nonHuman + ", treeNumber="
        + treeNumber + ", usageNote=" + usageNote + ", structuralChemical="
        + structuralChemical + ", functionalChemical=" + functionalChemical
        + "]";
  }

}
