package org.tdl.vireo.model;

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.REFRESH;
import static javax.persistence.FetchType.EAGER;
import static javax.persistence.FetchType.LAZY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.tdl.vireo.model.validation.NamedSearchFilterValidator;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import edu.tamu.weaver.validation.model.ValidatingBaseEntity;

@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "name" }) })
public class NamedSearchFilterGroup extends ValidatingBaseEntity {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @ManyToOne(optional = false)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, scope = User.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private User user;

    @Column(nullable = true)
    private String name;

    @Column(nullable = false)
    private Boolean publicFlag;

    @Column(nullable = false)
    private Boolean columnsFlag;

    @Column(nullable = false)
    private Boolean umiRelease;

    @Column(nullable = true)
    private String sortColumnTitle;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private Sort sortDirection;

    @OrderColumn
    @ManyToMany(cascade = { REFRESH, MERGE }, fetch = LAZY)
    private List<SubmissionListColumn> savedColumns;

    @Fetch(FetchMode.SELECT)
    @OneToMany(cascade = { REFRESH, MERGE }, fetch = EAGER, orphanRemoval = true)
    private Set<NamedSearchFilter> namedSearchFilters;

    public NamedSearchFilterGroup() {
        setPublicFlag(false);
        setColumnsFlag(false);
        setUmiRelease(false);
        setSavedColumns(new ArrayList<SubmissionListColumn>());
        setNamedSearchFilters(new HashSet<NamedSearchFilter>());
        setModelValidator(new NamedSearchFilterValidator());
    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user
     *            the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the publicFlag
     */
    public Boolean getPublicFlag() {
        return publicFlag;
    }

    /**
     * @param publicFlag
     *            the publicFlag to set
     */
    public void setPublicFlag(Boolean publicFlag) {
        this.publicFlag = publicFlag;
    }

    public Boolean getColumnsFlag() {
        return columnsFlag;
    }

    public void setColumnsFlag(Boolean columnsFlag) {
        this.columnsFlag = columnsFlag;
    }

    /**
     * @return the umiRelease
     */
    public Boolean getUmiRelease() {
        return umiRelease;
    }

    public String getSortColumnTitle() {
        return sortColumnTitle;
    }

    public void setSortColumnTitle(String sortColumnTitle) {
        this.sortColumnTitle = sortColumnTitle;
    }

    public Sort getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(Sort sortDirection) {
        this.sortDirection = sortDirection;
    }

    public List<SubmissionListColumn> getSavedColumns() {
        return savedColumns;
    }

    public void setSavedColumns(List<SubmissionListColumn> savedColumns) {
        this.savedColumns = savedColumns;
    }

    public void addSavedColumn(SubmissionListColumn submissionListColumn) {
        if (!savedColumns.contains(submissionListColumn)) {
            savedColumns.add(submissionListColumn);
        }
    }

    public void removeSavedColumn(SubmissionListColumn submissionListColumn) {
        savedColumns.remove(submissionListColumn);
    }

    /**
     * @param umiRelease
     *            the umiRelease to set
     */
    public void setUmiRelease(Boolean umiRelease) {
        this.umiRelease = umiRelease;
    }

    /**
     * @return the filterCriteria
     */
    public Set<NamedSearchFilter> getNamedSearchFilters() {
        return namedSearchFilters;
    }

    /**
     * @param namedSearchFilters
     *            the filterCriteria to set
     */
    public void setNamedSearchFilters(Set<NamedSearchFilter> namedSearchFilters) {
        this.namedSearchFilters = namedSearchFilters;
    }

    public void addFilterCriterion(NamedSearchFilter namedSearchFilter) {
        if (!namedSearchFilters.contains(namedSearchFilter)) {
            namedSearchFilters.add(namedSearchFilter);
        }
    }

    public void removeNamedSearchFilter(NamedSearchFilter namedSearchFilter) {
        namedSearchFilters.remove(namedSearchFilter);
    }

    public NamedSearchFilter getNamedSearchFilter(Long namedSearchFilterId) {
        for (NamedSearchFilter namedSearchFilter : namedSearchFilters) {
            if (namedSearchFilter.getId() == namedSearchFilterId) {
                return namedSearchFilter;
            }
        }
        return null;
    }

}
