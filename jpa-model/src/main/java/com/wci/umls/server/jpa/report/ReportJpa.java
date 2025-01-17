/*
 *    Copyright 2017 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.report;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.EnumBridge;
import org.hibernate.search.bridge.builtin.LongBridge;

import com.wci.umls.server.Project;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.jpa.ProjectJpa;
import com.wci.umls.server.jpa.content.AbstractHasLastModified;
import com.wci.umls.server.model.report.Report;
import com.wci.umls.server.model.report.ReportResult;

/**
 * JPA enabled implementation of {@link Report}.
 */
@Entity
@Table(name = "reports")
@Indexed
@XmlRootElement(name = "report")
public class ReportJpa extends AbstractHasLastModified implements Report {

  /** The id. */
  @Id
  @GenericGenerator(name = "ExistingOrGeneratedId", strategy = "com.wci.umls.server.jpa.helpers.UseExistingOrGenerateIdGenerator")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "ExistingOrGeneratedId")
  private Long id;

  /** The name. */
  @Column(nullable = false)
  private String name;

  /** The auto generated. */
  @Column(nullable = false)
  private boolean autoGenerated;

  /** The query. */
  @Column(nullable = false, length = 10000)
  private String query; // save the query used at the time report was

  /** The query type. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private QueryType queryType;

  /** The query result type. */
  @Column(nullable = false)
  private String resultType;

  /** The project. */
  @ManyToOne(targetEntity = ProjectJpa.class, optional = false)
  // @IndexedEmbedded - don't embed up the indexing tree
  private Project project;

  /** The results. */
  @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, targetEntity = ReportResultJpa.class)
  // @IndexedEmbedded(targetElement = ReportResultJpa.class)
  private List<ReportResult> results = new ArrayList<>();

  // DIFF report stuff
  /** The source reports for a comparison report. */
  @Column(nullable = true)
  private Long report1Id = null;

  /** The report2 id. */
  @Column(nullable = true)
  private Long report2Id = null;

  /** Flags for diff and rate report. */
  @Column(nullable = false)
  private boolean diffReport = false;

  /**
   * Constructors.
   * 
   */

  /**
   * Instantiates a new report jpa.
   */
  public ReportJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link ReportJpa} from the specified parameters.
   *
   * @param report the report
   * @param collectionsFlag the collections flag
   */
  public ReportJpa(Report report, boolean collectionsFlag) {
    super(report);
    id = report.getId();
    name = report.getName();
    autoGenerated = report.getAutoGenerated();
    query = report.getQuery();
    queryType = report.getQueryType();
    resultType = report.getResultType();
    report1Id = report.getReport1Id();
    report2Id = report.getReport2Id();
    diffReport = report.isDiffReport();
    project = report.getProject();

    if (collectionsFlag) {
      results = new ArrayList<>(report.getResults());
    }
  }

  /* see superclass */
  @Override
  @FieldBridge(impl = LongBridge.class)
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public Long getId() {
    return this.id;
  }

  /* see superclass */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Gets the name.
   * 
   * @return the name
   */
  @Fields({
      @Field(name = "name", index = Index.YES, store = Store.NO, analyze = Analyze.YES, analyzer = @Analyzer(definition = "noStopWord")),
      @Field(name = "nameSort", index = Index.YES, analyze = Analyze.NO, store = Store.NO),
  })
  @Override
  public String getName() {
    return name;
  }

  /* see superclass */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public Boolean getAutoGenerated() {
    return autoGenerated;
  }

  @Override
  public void setAutoGenerated(boolean autoGenerated) {
    this.autoGenerated = autoGenerated;
  }

  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getResultType() {
    return resultType;
  }

  @Override
  public void setResultType(String resultType) {
    this.resultType = resultType;
  }

  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public String getQuery() {
    return query;
  }

  @Override
  public void setQuery(String query) {
    this.query = query;
  }

  @Override
  @FieldBridge(impl = EnumBridge.class)
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public QueryType getQueryType() {
    return queryType;
  }

  @Override
  public void setQueryType(QueryType queryType) {
    this.queryType = queryType;
  }

  @Override
  @XmlElement(type = ReportResultJpa.class)
  public List<ReportResult> getResults() {
    if (results == null) {
      results = new ArrayList<>();
    }
    return results;
  }

  @Override
  public void setResults(List<ReportResult> results) {
    this.results = results;
  }

  @Override
  @FieldBridge(impl = LongBridge.class)
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public Long getReport1Id() {
    return this.report1Id;
  }

  @Override
  public void setReport1Id(Long reportId) {
    this.report1Id = reportId;

  }

  @Override
  @FieldBridge(impl = LongBridge.class)
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public Long getReport2Id() {
    return this.report2Id;
  }

  @Override
  public void setReport2Id(Long reportId) {
    this.report2Id = reportId;

  }

  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public boolean isDiffReport() {
    return diffReport;
  }

  @Override
  public void setDiffReport(boolean diffReport) {
    this.diffReport = diffReport;
  }

  /* see superclass */
  @XmlTransient
  @Override
  public Project getProject() {
    return project;
  }

  /* see superclass */
  @Override
  public void setProject(Project project) {
    this.project = project;
  }

  /**
   * Returns the project id. For JAXB
   *
   * @return the project id
   */
  @XmlElement
  @FieldBridge(impl = LongBridge.class)
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public Long getProjectId() {
    return (project != null) ? project.getId() : 0;
  }

  /**
   * Sets the project id.
   *
   * @param projectId the project id
   */
  @SuppressWarnings("unused")
  private void setProjectId(Long projectId) {
    if (project == null) {
      project = new ProjectJpa();
    }
    project.setId(projectId);
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (autoGenerated ? 1231 : 1237);
    result = prime * result + (diffReport ? 1231 : 1237);
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((query == null) ? 0 : query.hashCode());
    result = prime * result + ((queryType == null) ? 0 : queryType.hashCode());
    result = prime * result + ((report1Id == null) ? 0 : report1Id.hashCode());
    result = prime * result + ((report2Id == null) ? 0 : report2Id.hashCode());
    result =
        prime * result + ((resultType == null) ? 0 : resultType.hashCode());
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
    ReportJpa other = (ReportJpa) obj;
    if (autoGenerated != other.autoGenerated)
      return false;
    if (diffReport != other.diffReport)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (query == null) {
      if (other.query != null)
        return false;
    } else if (!query.equals(other.query))
      return false;
    if (queryType != other.queryType)
      return false;
    if (report1Id == null) {
      if (other.report1Id != null)
        return false;
    } else if (!report1Id.equals(other.report1Id))
      return false;
    if (report2Id == null) {
      if (other.report2Id != null)
        return false;
    } else if (!report2Id.equals(other.report2Id))
      return false;
    if (resultType == null) {
      if (other.resultType != null)
        return false;
    } else if (!resultType.equals(other.resultType))
      return false;
    return true;
  }

}
