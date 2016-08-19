/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.hibernate.envers.Audited;

import com.wci.umls.server.AlgorithmInfo;
import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ProcessInfo;

/**
 * JPA and JAXB enabled implementation of {@link AlgorithmInfo}.
 * @param <T> the process info type (e.g. config or execution)
 */
@Audited
@MappedSuperclass
@XmlSeeAlso({
    AlgorithmConfigJpa.class, AlgorithmExecutionJpa.class
})
public abstract class AbstractAlgorithmInfo<T extends ProcessInfo<?>>
    implements AlgorithmInfo<T> {

  /** The id. */
  @TableGenerator(name = "EntityIdGen", table = "table_generator", pkColumnValue = "Entity")
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "EntityIdGen")
  private Long id;

  /** The last modified. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastModified;

  /** The last modified. */
  @Column(nullable = false)
  private String lastModifiedBy;

  /** The last modified. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp = new Date();

  /** The name. */
  @Column(nullable = false)
  private String name;

  /** The description. */
  @Column(nullable = false)
  private String description;

  /** The algorithm key. */
  @Column(nullable = false)
  private String algorithmKey;

  /** The terminology. */
  @Column(nullable = false)
  private String terminology;

  /** The version. */
  @Column(nullable = false)
  private String version;

  /** parameters. */
  @OneToMany(targetEntity = AlgorithmParameterJpa.class, orphanRemoval = true)
  private List<AlgorithmParameter> parameters = new ArrayList<>();

  /**
   * Instantiates an empty {@link AbstractAlgorithmInfo}.
   */
  public AbstractAlgorithmInfo() {
    // n/a
  }

  /**
   * Instantiates a {@link AbstractAlgorithmInfo} from the specified parameters.
   *
   * @param info the config
   */
  public AbstractAlgorithmInfo(AlgorithmInfo<?> info) {
    id = info.getId();
    name = info.getName();
    description = info.getDescription();
    terminology = info.getTerminology();
    version = info.getVersion();
    for (final AlgorithmParameter param : info.getParameters()) {
      getParameters().add(new AlgorithmParameterJpa(param));
    }
    algorithmKey = info.getAlgorithmKey();

  }

  /* see superclass */
  @Override
  public Long getId() {
    return id;
  }

  /* see superclass */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /* see superclass */
  @Override
  public Date getLastModified() {
    return lastModified;
  }

  /* see superclass */
  @Override
  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  /* see superclass */
  @Override
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  /* see superclass */
  @Override
  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  /* see superclass */
  @Override
  public Date getTimestamp() {
    return timestamp;
  }

  /* see superclass */
  @Override
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  /* see superclass */
  @Override
  public String getName() {
    return name;
  }

  /* see superclass */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return description;
  }

  /* see superclass */
  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  /* see superclass */
  @Override
  public String getAlgorithmKey() {
    return algorithmKey;
  }

  /* see superclass */
  @Override
  public void setAlgorithmKey(String algorithmKey) {
    this.algorithmKey = algorithmKey;

  }

  /* see superclass */
  @Override
  public String getTerminology() {
    return terminology;
  }

  /* see superclass */
  @Override
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /* see superclass */
  @Override
  public String getVersion() {
    return version;
  }

  /* see superclass */
  @Override
  public void setVersion(String version) {
    this.version = version;
  }

  /* see superclass */
  @Override
  @XmlElement(type = AlgorithmParameterJpa.class)
  public List<AlgorithmParameter> getParameters() {
    if (parameters == null) {
      parameters = new ArrayList<>();
    }
    return parameters;
  }

  /* see superclass */
  @Override
  public void setParameters(List<AlgorithmParameter> parameters) {
    this.parameters = parameters;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((algorithmKey == null) ? 0 : algorithmKey.hashCode());
    result =
        prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());

    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    @SuppressWarnings("rawtypes")
    AbstractAlgorithmInfo other = (AbstractAlgorithmInfo) obj;
    if (algorithmKey == null) {
      if (other.algorithmKey != null)
        return false;
    } else if (!algorithmKey.equals(other.algorithmKey))
      return false;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (terminology == null) {
      if (other.terminology != null)
        return false;
    } else if (!terminology.equals(other.terminology))
      return false;
    if (version == null) {
      if (other.version != null)
        return false;
    } else if (!version.equals(other.version))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "AbstractAlgorithmInfo [id=" + id + ", lastModified=" + lastModified
        + ", lastModifiedBy=" + lastModifiedBy + ", timestamp=" + timestamp
        + ", name=" + name + ", description=" + description + ", algorithmKey="
        + algorithmKey + ", terminology=" + terminology + ", version=" + version
        + ", parameters=" + parameters + "]";
  }



}