/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.content;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Table;
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

import com.wci.umls.server.helpers.ComponentInfo;
import com.wci.umls.server.jpa.ComponentInfoJpa;
import com.wci.umls.server.jpa.helpers.MapValueToCsvBridge;
import com.wci.umls.server.model.content.ComponentInfoRelationship;
import com.wci.umls.server.model.meta.IdType;

/**
 * JPA-enabled implementation of {@link ComponentInfoRelationship}.
 */
@Entity
@Table(name = "component_info_relationships", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "version", "id"
}))
@Audited
@Indexed
@XmlRootElement(name = "componentInfoRelationship")
public class ComponentInfoRelationshipJpa extends
    AbstractRelationship<ComponentInfo, ComponentInfo> implements ComponentInfoRelationship {

  private String fromTerminologyId;
  private String fromTerminology;
  private String fromVersion;
  private String fromName;
  private IdType fromType;

  private String toTerminologyId;
  private String toTerminology;
  private String toVersion;
  private String toName;
  private IdType toType;

  /** The alternate terminology ids. */
  @ElementCollection(fetch = FetchType.EAGER)
  @Column(nullable = true)
  private Map<String, String> alternateTerminologyIds; // index
  
  
  /**
   * Instantiates an empty {@link ComponentInfoRelationshipJpa}.
   */
  public ComponentInfoRelationshipJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link ComponentInfoRelationshipJpa} from the specified
   * parameters.
   *
   * @param relationship the relationship
   * @param deepCopy the deep copy
   */
  public ComponentInfoRelationshipJpa(ComponentInfoRelationship relationship,
      boolean deepCopy) {
    super(relationship, deepCopy);
    fromTerminologyId = relationship.getFrom().getTerminologyId();
    fromTerminology = relationship.getFrom().getTerminology();
    fromVersion = relationship.getFrom().getVersion();
    fromName = relationship.getFrom().getName();
    fromType = relationship.getFrom().getType();

    toTerminologyId = relationship.getTo().getTerminologyId();
    toTerminology = relationship.getTo().getTerminology();
    toVersion = relationship.getTo().getVersion();
    toName = relationship.getTo().getName();
    toType = relationship.getTo().getType();

  }

  /* see superclass */
  @Override
  @XmlTransient
  public ComponentInfo getFrom() {
    final ComponentInfo info = new ComponentInfoJpa();
    info.setTerminology(fromTerminology);
    info.setVersion(fromVersion);
    info.setType(fromType);
    info.setTerminologyId(fromTerminologyId);
    info.setName(fromName);
    return info;
  }

  /* see superclass */
  @Override
  public void setFrom(ComponentInfo component) {
    fromTerminology = component.getTerminology();
    fromVersion = component.getVersion();
    fromType = component.getType();
    fromTerminologyId = component.getTerminologyId();
    fromName = component.getName();
  }



  /**
   * Returns the from terminology.
   *
   * @return the from terminology
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getFromTerminology() {
    return fromTerminology;
  }

  /**
   * Sets the from terminology.
   *
   * @param terminology the from terminology
   */
  public void setFromTerminology(String terminology) {
    fromTerminology = terminology;
  }

  /**
   * Returns the from version.
   *
   * @return the from version
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getFromVersion() {
    return fromVersion;
  }

  /**
   * Sets the from terminology id.
   *
   * @param version the from terminology id
   */
  public void setFromVersion(String version) {
    fromVersion = version;
  }

  /**
   * Returns the from terminology id.
   *
   * @return the from terminology id
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getFromTerminologyId() {
    return fromTerminologyId;
  }

  /**
   * Sets the from terminology id.
   *
   * @param terminologyId the from terminology id
   */
  public void setFromTerminologyId(String terminologyId) {
    fromTerminologyId = terminologyId;
  }

  /**
   * Returns the from term. For JAXB.
   *
   * @return the from term
   */
  @Fields({
      @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO, analyzer = @Analyzer(definition = "noStopWord")),
      @Field(name = "fromNameSort", index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  })
  public String getFromName() {
    return fromName;
  }

  /**
   * Sets the from term.
   *
   * @param term the from term
   */
  public void setFromName(String term) {
    fromName = term;
  }

  /* see superclass */
  @Override
  @XmlTransient
  public ComponentInfo getTo() {
    final ComponentInfo info = new ComponentInfoJpa();
    info.setTerminology(toTerminology);
    info.setVersion(toVersion);
    info.setType(toType);
    info.setTerminologyId(toTerminologyId);
    info.setName(toName);
    return info;
  }

  /* see superclass */
  @Override
  public void setTo(ComponentInfo component) {
    toTerminology = component.getTerminology();
    toVersion = component.getVersion();
    toType = component.getType();
    toName = component.getName();
    toTerminologyId = component.getTerminologyId();
  }



  /**
   * Returns the to terminology id.
   *
   * @return the to terminology id
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getToTerminologyId() {
    return toTerminologyId;
  }

  /**
   * Sets the to terminology id.
   *
   * @param terminologyId the to terminology id
   */
  public void setToTerminologyId(String terminologyId) {
    toTerminologyId = terminologyId;
  }

  /**
   * Returns the to terminology.
   *
   * @return the to terminology
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getToTerminology() {
    return toTerminology;
  }

  /**
   * Sets the to terminology.
   *
   * @param terminology the to terminology
   */
  public void setToTerminology(String terminology) {
    toTerminology = terminology;
  }

  /**
   * Returns the to version.
   *
   * @return the to version
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getToVersion() {
    return toVersion;
  }

  /**
   * Sets the to version.
   *
   * @param version the to version
   */
  public void setToVersion(String version) {
    toVersion = version;
  }

  /**
   * Returns the to term. For JAXB.
   *
   * @return the to term
   */
  @Fields({
      @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO),
      @Field(name = "toNameSort", index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  })
  public String getToName() {
    return toName;
  }

  /**
   * Sets the to term.
   *
   * @param term the to term
   */
  public void setToName(String term) {
    toName = term;
  }


  /* see superclass */
  @Override
  @FieldBridge(impl = MapValueToCsvBridge.class)
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
  public void putAlternateTerminologyId(String terminology, String terminologyId) {
    if (alternateTerminologyIds == null) {
      alternateTerminologyIds = new HashMap<>(2);
    }
    alternateTerminologyIds.put(terminology, terminologyId);
  }

  /* see superclass */
  @Override
  public void removeAlternateTerminologyId(String terminology) {
    if (alternateTerminologyIds == null) {
      alternateTerminologyIds = new HashMap<>(2);
    }
    alternateTerminologyIds.remove(terminology);

  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((fromName == null) ? 0 : fromName.hashCode());
    result = prime * result
        + ((fromTerminology == null) ? 0 : fromTerminology.hashCode());
    result = prime * result
        + ((fromTerminologyId == null) ? 0 : fromTerminologyId.hashCode());
    result = prime * result + ((fromType == null) ? 0 : fromType.hashCode());
    result =
        prime * result + ((fromVersion == null) ? 0 : fromVersion.hashCode());
    result = prime * result + ((toName == null) ? 0 : toName.hashCode());
    result = prime * result
        + ((toTerminology == null) ? 0 : toTerminology.hashCode());
    result = prime * result
        + ((toTerminologyId == null) ? 0 : toTerminologyId.hashCode());
    result = prime * result + ((toType == null) ? 0 : toType.hashCode());
    result = prime * result + ((toVersion == null) ? 0 : toVersion.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    ComponentInfoRelationshipJpa other = (ComponentInfoRelationshipJpa) obj;
    if (fromName == null) {
      if (other.fromName != null)
        return false;
    } else if (!fromName.equals(other.fromName))
      return false;
    if (fromTerminology == null) {
      if (other.fromTerminology != null)
        return false;
    } else if (!fromTerminology.equals(other.fromTerminology))
      return false;
    if (fromTerminologyId == null) {
      if (other.fromTerminologyId != null)
        return false;
    } else if (!fromTerminologyId.equals(other.fromTerminologyId))
      return false;
    if (fromType != other.fromType)
      return false;
    if (fromVersion == null) {
      if (other.fromVersion != null)
        return false;
    } else if (!fromVersion.equals(other.fromVersion))
      return false;
    if (toName == null) {
      if (other.toName != null)
        return false;
    } else if (!toName.equals(other.toName))
      return false;
    if (toTerminology == null) {
      if (other.toTerminology != null)
        return false;
    } else if (!toTerminology.equals(other.toTerminology))
      return false;
    if (toTerminologyId == null) {
      if (other.toTerminologyId != null)
        return false;
    } else if (!toTerminologyId.equals(other.toTerminologyId))
      return false;
    if (toType != other.toType)
      return false;
    if (toVersion == null) {
      if (other.toVersion != null)
        return false;
    } else if (!toVersion.equals(other.toVersion))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "ComponentInfoRelationshipJpa ["       
        + "getFrom()=" + getFrom()
        + ", getFromTerminology()=" + getFromTerminology()
        + ", getFromVersion()=" + getFromVersion()
        + ", getFromTerminologyId()=" + getFromTerminologyId()
        + ", getFromName()=" + getFromName() + ", getTo()=" + getTo()
        + ", getToTerminologyId()="
        + getToTerminologyId() + ", getToTerminology()=" + getToTerminology()
        + ", getToVersion()=" + getToVersion() + ", getToName()=" + getToName()
        + ", getAlternateTerminologyIds()=" + getAlternateTerminologyIds()
        + ", hashCode()=" + hashCode() + ", getRelationshipType()="
        + getRelationshipType() + ", getAdditionalRelationshipType()="
        + getAdditionalRelationshipType() + ", getGroup()=" + getGroup()
        + ", isInferred()=" + isInferred() + ", isStated()=" + isStated()
        + ", isHierarchical()=" + isHierarchical() + ", isAssertedDirection()="
        + isAssertedDirection() + ", toString()=" + super.toString()
        + ", getAttributes()=" + getAttributes() + ", getId()=" + getId()
        + ", getObjectId()=" + getObjectId() + ", getTimestamp()="
        + getTimestamp() + ", getLastModified()=" + getLastModified()
        + ", getLastModifiedBy()=" + getLastModifiedBy()
        + ", isSuppressible()=" + isSuppressible() + ", isObsolete()="
        + isObsolete() + ", isPublished()=" + isPublished()
        + ", isPublishable()=" + isPublishable() + ", getBranch()="
        + getBranch() + ", getVersion()=" + getVersion()
        + ", getTerminology()=" + getTerminology() + ", getTerminologyId()="
        + getTerminologyId() + ", getClass()=" + getClass() + "]";
  }

}
