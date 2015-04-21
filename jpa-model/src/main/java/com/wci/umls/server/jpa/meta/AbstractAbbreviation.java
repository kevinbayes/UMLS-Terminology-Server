/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.meta;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;

import com.wci.umls.server.model.meta.Abbreviation;

/**
 * Abstract implementation of {@link Abbreviation} for use with JPA.
 */
@Audited
@MappedSuperclass
public abstract class AbstractAbbreviation extends AbstractHasLastModified
    implements Abbreviation {

  /** The published flag. */
  @Column(nullable = false)
  private boolean published = false;

  /** The publishable flag. */
  @Column(nullable = false)
  private boolean publishable = false;

  /** The abbreviation. */
  @Column(nullable = false)
  private String abbreviation;

  /** The expandedForm. */
  @Column(nullable = false, length = 4000)
  private String expandedForm;

  /** The terminology. */
  @Column(nullable = false)
  private String terminology;

  /** The expandedForm. */
  @Column(nullable = false)
  private String terminologyVersion;

  /** The branch. */
  @Column(nullable = true)
  private String branch;

  /**
   * Instantiates an empty {@link AbstractAbbreviation}.
   */
  protected AbstractAbbreviation() {
    // do nothing
  }

  /**
   * Instantiates a {@link AbstractAbbreviation} from the specified parameters.
   *
   * @param abbreviation the abbreviation
   */
  protected AbstractAbbreviation(Abbreviation abbreviation) {
    super(abbreviation);
    this.abbreviation = abbreviation.getAbbreviation();
    expandedForm = abbreviation.getExpandedForm();
    terminology = abbreviation.getTerminology();
    terminologyVersion = abbreviation.getTerminologyVersion();
    publishable = abbreviation.isPublishable();
    published = abbreviation.isPublished();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#isPublished()
   */
  /**
   * Indicates whether or not published is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  @Override
  public boolean isPublished() {
    return published;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#setPublished(boolean)
   */
  /**
   * Sets the published.
   *
   * @param published the published
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
  /**
   * Indicates whether or not publishable is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  @Override
  public boolean isPublishable() {
    return publishable;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.rf2.Component#setPublishable(boolean)
   */
  /**
   * Sets the publishable.
   *
   * @param publishable the publishable
   */
  @Override
  public void setPublishable(boolean publishable) {
    this.publishable = publishable;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Abbreviation#getBranch()
   */
  @Override
  public String getBranch() {
    return branch;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.Abbreviation#setBranch(java.lang.String)
   */
  @Override
  public void setBranch(String branch) {
    this.branch = branch;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Abbreviation#getAbbreviation()
   */
  /**
   * Returns the abbreviation.
   *
   * @return the abbreviation
   */
  @Override
  public String getAbbreviation() {
    return abbreviation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Abbreviation#getExpandedForm()
   */
  /**
   * Returns the expanded form.
   *
   * @return the expanded form
   */
  @Override
  public String getExpandedForm() {
    return expandedForm;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.Abbreviation#setAbbreviation(java.lang.String
   * )
   */
  /**
   * Sets the abbreviation.
   *
   * @param abbreviation the abbreviation
   */
  @Override
  public void setAbbreviation(String abbreviation) {
    this.abbreviation = abbreviation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.Abbreviation#setExpandedForm(java.lang.String
   * )
   */
  /**
   * Sets the expanded form.
   *
   * @param expandedForm the expanded form
   */
  @Override
  public void setExpandedForm(String expandedForm) {
    this.expandedForm = expandedForm;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.helpers.HasTerminology#getTerminology()
   */
  /**
   * Returns the terminology.
   *
   * @return the terminology
   */
  @Override
  public String getTerminology() {
    return terminology;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.helpers.HasTerminology#setTerminology(java.lang.String)
   */
  /**
   * Sets the terminology.
   *
   * @param terminology the terminology
   */
  @Override
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.helpers.HasTerminology#getTerminologyVersion()
   */
  /**
   * Returns the terminology version.
   *
   * @return the terminology version
   */
  @Override
  public String getTerminologyVersion() {
    return terminologyVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.helpers.HasTerminology#setTerminologyVersion(java.lang
   * .String)
   */
  /**
   * Sets the terminology version.
   *
   * @param terminologyVersion the terminology version
   */
  @Override
  public void setTerminologyVersion(String terminologyVersion) {
    this.terminologyVersion = terminologyVersion;
  }

  /**
   * Hash code.
   *
   * @return the int
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((abbreviation == null) ? 0 : abbreviation.hashCode());
    result =
        prime * result + ((expandedForm == null) ? 0 : expandedForm.hashCode());
    result = prime * result + (publishable ? 1231 : 1237);
    result = prime * result + (published ? 1231 : 1237);
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result =
        prime
            * result
            + ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
    return result;
  }

  /**
   * Equals.
   *
   * @param obj the obj
   * @return true, if successful
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AbstractAbbreviation other = (AbstractAbbreviation) obj;
    if (abbreviation == null) {
      if (other.abbreviation != null)
        return false;
    } else if (!abbreviation.equals(other.abbreviation))
      return false;
    if (expandedForm == null) {
      if (other.expandedForm != null)
        return false;
    } else if (!expandedForm.equals(other.expandedForm))
      return false;
    if (publishable != other.publishable)
      return false;
    if (published != other.published)
      return false;
    if (terminology == null) {
      if (other.terminology != null)
        return false;
    } else if (!terminology.equals(other.terminology))
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
    return "AbstractAbbreviation [id=" + getId() + ", timestamp="
        + getTimestamp() + ", lastModified=" + getLastModified()
        + ", lastModifiedBy=" + getLastModifiedBy() + ", published="
        + published + ", publishable=" + publishable + ", abbreviation="
        + abbreviation + ", expandedForm=" + expandedForm + ", terminology="
        + terminology + ", terminologyVersion=" + terminologyVersion + "]";
  }

}