/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.meta;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;

import com.wci.umls.server.model.meta.Citation;
import com.wci.umls.server.model.meta.IdentifierType;
import com.wci.umls.server.model.meta.RootTerminology;
import com.wci.umls.server.model.meta.Terminology;

/**
 * JPA-enabled implementation of {@link Terminology}.
 */
@Entity
@Table(name = "terminologies", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminology", "terminologyVersion"
}))
@Audited
@XmlRootElement(name = "terminology")
public class TerminologyJpa implements Terminology {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The terminology. */
  @Column(nullable = false)
  private String terminology;

  /** The citation. */
  @OneToOne(targetEntity = CitationJpa.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER, optional = true)
  private Citation citation;

  /** The end date. */
  @Column(nullable = true)
  @Temporal(TemporalType.TIMESTAMP)
  private Date endDate;

  /** The organizing class type. */
  @ManyToOne(targetEntity = IdentifierTypeJpa.class, fetch = FetchType.EAGER, optional = false)
  @JoinColumn(nullable = false)
  private IdentifierType organizingClassType;

  /** The preferred name. */
  @Column(nullable = false, length = 3000)
  private String preferredName;

  /** The root terminology. */
  @ManyToOne(targetEntity = RootTerminologyJpa.class, fetch = FetchType.EAGER, optional = false)
  @JoinColumn(nullable = false)
  private RootTerminology rootTerminology;

  /** The start date. */
  @Column(nullable = true)
  @Temporal(TemporalType.TIMESTAMP)
  private Date startDate;

  /** The synonymous names. */
  @ElementCollection
  @Column(nullable = true)
  private List<String> synonymousNames;

  /** The terminology version. */
  @Column(nullable = false)
  private String terminologyVersion;

  /** The is asserts rel direction. */
  @Column(nullable = false)
  private boolean assertsRelDirection = false;

  /** The is current. */
  @Column(nullable = false)
  private boolean current = false;

  /**
   * Instantiates an empty {@link TerminologyJpa}.
   */
  public TerminologyJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link TerminologyJpa} from the specified parameters.
   *
   * @param terminology the terminology
   */
  public TerminologyJpa(Terminology terminology) {
    id = terminology.getId();
    this.terminology = terminology.getTerminology();
    citation = terminology.getCitation();
    endDate = terminology.getEndDate();
    organizingClassType = terminology.getOrganizingClassType();
    preferredName = terminology.getPreferredName();
    rootTerminology = terminology.getRootTerminology();
    startDate = terminology.getStartDate();
    synonymousNames = terminology.getSynonymousNames();
    terminologyVersion = terminology.getTerminologyVersion();
    assertsRelDirection = terminology.isAssertsRelDirection();
    current = terminology.isCurrent();
  }

  @Override
  @XmlTransient
  public Long getId() {
    return this.id;
  }

  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Terminology#getCitation()
   */
  @Override
  @XmlElement(type = CitationJpa.class, name = "citation")
  public Citation getCitation() {
    return citation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.Terminology#setCitation(com.wci.umls.server
   * .model.meta.Citation)
   */
  @Override
  public void setCitation(Citation citation) {
    this.citation = citation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Terminology#getEndDate()
   */
  @Override
  public Date getEndDate() {
    return endDate;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Terminology#setEndDate(java.util.Date)
   */
  @Override
  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Terminology#getOrganizingClassType()
   */
  @Override
  @XmlElement(type = IdentifierTypeJpa.class, name = "organizingClassType")
  public IdentifierType getOrganizingClassType() {
    return organizingClassType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.Terminology#setOrganizingClassType(com.wci
   * .umls.server.model.meta.IdentifierType)
   */
  @Override
  public void setOrganizingClassType(IdentifierType organizingClassType) {
    this.organizingClassType = organizingClassType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Terminology#getPreferredName()
   */
  @Override
  public String getPreferredName() {
    return preferredName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.Terminology#setPreferredName(java.lang.String
   * )
   */
  @Override
  public void setPreferredName(String preferredName) {
    this.preferredName = preferredName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Terminology#getRootTerminology()
   */
  @Override
  @XmlTransient
  public RootTerminology getRootTerminology() {
    return rootTerminology;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.Terminology#setRootTerminology(com.wci.umls
   * .server.model.meta.RootTerminology)
   */
  @Override
  public void setRootTerminology(RootTerminology rootTerminology) {
    this.rootTerminology = rootTerminology;
  }

  /**
   * Returns the root terminology id. For JAXB.
   *
   * @return the root terminology id
   */
  public Long getRootTerminologyId() {
    return rootTerminology == null ? null : rootTerminology.getId();
  }

  /**
   * Sets the root terminology id.
   *
   * @param id the root terminology id
   */
  public void setRootTerminologyId(Long id) {
    if (rootTerminology == null) {
      rootTerminology = new RootTerminologyJpa();
    }
    rootTerminology.setId(id);
  }

  /**
   * Returns the root terminology abbreviation. For JAXB
   *
   * @return the root terminology abbreviation
   */
  public String getRootTerminologyAbbreviation() {
    return rootTerminology == null ? null : rootTerminology.getTerminology();
  }

  /**
   * Sets the root terminology abbreviation.
   *
   * @param abbreviation the root terminology abbreviation
   */
  public void setRootTerminologyAbbreviation(String abbreviation) {
    if (rootTerminology == null) {
      rootTerminology = new RootTerminologyJpa();
    }
    rootTerminology.setTerminology(abbreviation);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Terminology#getStartDate()
   */
  @Override
  public Date getStartDate() {
    return startDate;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.Terminology#setStartDate(java.util.Date)
   */
  @Override
  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Terminology#getSynonymousNames()
   */
  @Override
  public List<String> getSynonymousNames() {
    return synonymousNames;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.Terminology#setSynonymousNames(java.util
   * .List)
   */
  @Override
  public void setSynonymousNames(List<String> synonymousNames) {
    this.synonymousNames = synonymousNames;
  }

  @Override
  public String getTerminology() {
    return terminology;
  }

  @Override
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Terminology#getTerminologyVersion()
   */
  @Override
  public String getTerminologyVersion() {
    return terminologyVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.Terminology#setTerminologyVersion(java.lang
   * .String)
   */
  @Override
  public void setTerminologyVersion(String terminologyVersion) {
    this.terminologyVersion = terminologyVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Terminology#isAssertsRelDirection()
   */
  @Override
  public boolean isAssertsRelDirection() {
    return assertsRelDirection;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.Terminology#setAssertsRelDirection(boolean)
   */
  @Override
  public void setAssertsRelDirection(boolean isAssertsRelDirection) {
    this.assertsRelDirection = isAssertsRelDirection;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.Terminology#isCurrent()
   */
  @Override
  public boolean isCurrent() {
    return current;
  }

  /**
   * Sets the current.
   *
   * @param isCurrent the current
   */
  public void setCurrent(boolean isCurrent) {
    this.current = isCurrent;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (assertsRelDirection ? 1231 : 1237);
    result = prime * result + ((citation == null) ? 0 : citation.hashCode());
    result = prime * result + (current ? 1231 : 1237);
    result = prime * result + ((endDate == null) ? 0 : endDate.hashCode());
    result =
        prime
            * result
            + ((organizingClassType == null) ? 0 : organizingClassType
                .hashCode());
    result =
        prime * result
            + ((preferredName == null) ? 0 : preferredName.hashCode());
    result = prime * result + ((startDate == null) ? 0 : startDate.hashCode());
    result =
        prime * result
            + ((synonymousNames == null) ? 0 : synonymousNames.hashCode());
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result =
        prime
            * result
            + ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TerminologyJpa other = (TerminologyJpa) obj;
    if (assertsRelDirection != other.assertsRelDirection)
      return false;
    if (citation == null) {
      if (other.citation != null)
        return false;
    } else if (!citation.equals(other.citation))
      return false;
    if (current != other.current)
      return false;
    if (endDate == null) {
      if (other.endDate != null)
        return false;
    } else if (!endDate.equals(other.endDate))
      return false;
    if (organizingClassType == null) {
      if (other.organizingClassType != null)
        return false;
    } else if (!organizingClassType.equals(other.organizingClassType))
      return false;
    if (preferredName == null) {
      if (other.preferredName != null)
        return false;
    } else if (!preferredName.equals(other.preferredName))
      return false;
    if (startDate == null) {
      if (other.startDate != null)
        return false;
    } else if (!startDate.equals(other.startDate))
      return false;
    if (synonymousNames == null) {
      if (other.synonymousNames != null)
        return false;
    } else if (!synonymousNames.equals(other.synonymousNames))
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
}