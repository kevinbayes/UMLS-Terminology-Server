/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.content;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import com.wci.umls.server.model.content.Subset;

/**
 * Abstract JPA-enabled implementation of {@link Subset}.
 */
@Entity
@Table(name = "subsets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 50)
@Audited
public abstract class AbstractSubset extends AbstractComponentHasAttributes
    implements Subset {

  /** The name. */
  @Column(nullable = true)
  private String name;

  /** The description. */
  @Column(nullable = true, length = 4000)
  private String description;

  /** The disjoint subset. */
  @Column(nullable = false)
  private boolean disjointSubset = false;

  /**
   * Instantiates an empty {@link AbstractSubset}.
   */
  public AbstractSubset() {
    // do nothing
  }

  /**
   * Instantiates a {@link AbstractSubset} from the specified parameters.
   *
   * @param subset the subset
   * @param deepCopy the deep copy
   */
  public AbstractSubset(Subset subset, boolean deepCopy) {
    super(subset, deepCopy);
    name = subset.getName();
    description = subset.getDescription();
    disjointSubset = subset.isDisjointSubset();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.content.Subset#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.content.Subset#setName(java.lang.String)
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.content.Subset#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.content.Subset#setDescription(java.lang.String)
   */
  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.content.Subset#isDisjointSubset()
   */
  @Override
  public boolean isDisjointSubset() {
    return disjointSubset;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.content.Subset#setDisjointSubset(boolean)
   */
  @Override
  public void setDisjointSubset(boolean disjointSubset) {
    this.disjointSubset = disjointSubset;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.jpa.content.AbstractComponent#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + (disjointSubset ? 1231 : 1237);
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.content.AbstractComponent#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    AbstractSubset other = (AbstractSubset) obj;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    if (disjointSubset != other.disjointSubset)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.jpa.content.AbstractComponent#toString()
   */
  @Override
  public String toString() {
    return "AbstractSubset [name=" + name + ", description=" + description
        + ", disjointSubset=" + disjointSubset + "]";
  }

}
