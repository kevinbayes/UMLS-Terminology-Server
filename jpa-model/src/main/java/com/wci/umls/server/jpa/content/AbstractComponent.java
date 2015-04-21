/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.content;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlID;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;

import com.wci.umls.server.model.content.Component;
import com.wci.umls.server.model.content.ComponentHasAttributes;

/**
 * Abstract implementation of {@link ComponentHasAttributes} for use with JPA.
 */
@Audited
@MappedSuperclass
public abstract class AbstractComponent implements Component {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** the timestamp. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp = new Date();

  /** The last modified. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastModified = new Date();

  /** The last modified. */
  @Column(nullable = false)
  private String lastModifiedBy;

  /** The suppressible flag. */
  @Column(nullable = false)
  private boolean suppressible = false;

  /** The obsolete flag. */
  @Column(nullable = false)
  private boolean obsolete = false;

  /** The published flag. */
  @Column(nullable = false)
  private boolean published = false;

  /** The publishable flag. */
  @Column(nullable = false)
  private boolean publishable = false;

  /** The terminology. */
  @Column(nullable = false)
  private String terminology;

  /** The terminology id. */
  @Column(nullable = false)
  private String terminologyId;

  /** The terminology version. */
  @Column(nullable = false)
  private String terminologyVersion;

  /** The branch. */
  @Column(nullable = true)
  private String branch = null;

  /**
   * Instantiates an empty {@link AbstractComponent}.
   */
  public AbstractComponent() {
    // do nothing
  }

  /**
   * Instantiates a {@link AbstractComponent} from the specified parameters.
   *
   * @param component the component
   */
  public AbstractComponent(Component component) {
    id = component.getId();
    lastModified = component.getLastModified();
    lastModifiedBy = component.getLastModifiedBy();
    terminology = component.getTerminology();
    terminologyId = component.getTerminologyId();
    terminologyVersion = component.getTerminologyVersion();
    publishable = component.isPublishable();
    published = component.isPublished();
    obsolete = component.isObsolete();
    suppressible = component.isSuppressible();
    branch = component.getBranch();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#getId()
   */
  @Override
  public Long getId() {
    return this.id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#setId(java.lang.Long)
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.content.Component#timestamp()
   */
  @Override
  public Date getTimestamp() {
    return timestamp;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.content.Component#setTimestamp(java.util.Date)
   */
  @Override
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#getLastModified()
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  @Override
  public Date getLastModified() {
    return lastModified;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#setLastModified(java.util.Date)
   */
  @Override
  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#getLastModifiedBy()
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  @Override
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#setLastModifiedBy(java.lang.String)
   */
  @Override
  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.content.Component#isSuppressible()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public boolean isSuppressible() {
    return suppressible;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.content.Component#setSuppressible(boolean)
   */
  @Override
  public void setSuppressible(boolean suppressible) {
    this.suppressible = suppressible;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.content.Component#isObsolete()
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  @Override
  public boolean isObsolete() {
    return obsolete;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.content.Component#setObsolete(boolean)
   */
  @Override
  public void setObsolete(boolean obsolete) {
    this.obsolete = obsolete;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#isPublished()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public boolean isPublished() {
    return published;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#setPublished(boolean)
   */
  @Override
  public void setPublished(boolean published) {
    this.published = published;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#isPublishable()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public boolean isPublishable() {
    return publishable;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#setPublishable(boolean)
   */
  @Override
  public void setPublishable(boolean publishable) {
    this.publishable = publishable;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.content.Component#getBranch()
   */
  @Override
  public String getBranch() {
    return branch;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.content.Component#setBranch(java.lang.String)
   */
  @Override
  public void setBranch(String branch) {
    this.branch = branch;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#getTerminologyVersion()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getTerminologyVersion() {
    return terminologyVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.ts.rf2.Component#setTerminologyVersion(java.lang.String)
   */
  @Override
  public void setTerminologyVersion(String terminologyVersion) {
    this.terminologyVersion = terminologyVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#getTerminology()
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  @Override
  public String getTerminology() {
    return terminology;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#setTerminology(java.lang.String)
   */
  @Override
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#getTerminologyId()
   */
  @Override
  @XmlID
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getTerminologyId() {
    return terminologyId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#setTerminologyId(java.lang.String)
   */
  @Override
  public void setTerminologyId(String terminologyId) {
    this.terminologyId = terminologyId;
  }

  /**
   * CUSTOM equals: uses .toString() on the concept terminology ids map.
   *
   * @return the int
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (obsolete ? 1231 : 1237);
    result = prime * result + (publishable ? 1231 : 1237);
    result = prime * result + (published ? 1231 : 1237);
    result = prime * result + (suppressible ? 1231 : 1237);
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result =
        prime * result
            + ((terminologyId == null) ? 0 : terminologyId.hashCode());
    result =
        prime
            * result
            + ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AbstractComponent other = (AbstractComponent) obj;

    if (obsolete != other.obsolete)
      return false;
    if (publishable != other.publishable)
      return false;
    if (published != other.published)
      return false;
    if (suppressible != other.suppressible)
      return false;
    if (terminology == null) {
      if (other.terminology != null)
        return false;
    } else if (!terminology.equals(other.terminology))
      return false;
    if (terminologyId == null) {
      if (other.terminologyId != null)
        return false;
    } else if (!terminologyId.equals(other.terminologyId))
      return false;
    if (terminologyVersion == null) {
      if (other.terminologyVersion != null)
        return false;
    } else if (!terminologyVersion.equals(other.terminologyVersion))
      return false;
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "AbstractComponent [id=" + id + ", lastModified=" + lastModified
        + ", lastModifiedBy=" + lastModifiedBy + ", suppressible="
        + suppressible + ", obsolete=" + obsolete + ", published=" + published
        + ", publishable=" + publishable + ", terminology=" + terminology
        + ", terminologyId=" + terminologyId + ", terminologyVersion="
        + terminologyVersion + ", branch=" + branch + "]";
  }

}