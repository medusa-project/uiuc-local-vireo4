package org.tdl.vireo.model.repo.impl;

import edu.tamu.weaver.data.model.repo.impl.AbstractWeaverRepoImpl;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.tdl.vireo.model.FilterCriterion;
import org.tdl.vireo.model.NamedSearchFilter;
import org.tdl.vireo.model.SubmissionListColumn;
import org.tdl.vireo.model.repo.FilterCriterionRepo;
import org.tdl.vireo.model.repo.NamedSearchFilterRepo;
import org.tdl.vireo.model.repo.custom.NamedSearchFilterRepoCustom;

public class NamedSearchFilterRepoImpl extends AbstractWeaverRepoImpl<NamedSearchFilter, NamedSearchFilterRepo> implements NamedSearchFilterRepoCustom {

    @Autowired
    private NamedSearchFilterRepo namedSearchFilterRepo;

    @Autowired
    private FilterCriterionRepo filterCriterionRepo;

    @Override
    public NamedSearchFilter create(SubmissionListColumn submissionListColumn) {
        NamedSearchFilter namedSearchFilter = new NamedSearchFilter();
        namedSearchFilter.setName(submissionListColumn.getTitle());
        namedSearchFilter.setSubmissionListColumn(submissionListColumn);
        return namedSearchFilterRepo.save(namedSearchFilter);
    }

    public NamedSearchFilter clone(NamedSearchFilter namedSearchFilter) {
        NamedSearchFilter newNamedSearchFilter = namedSearchFilterRepo.create(namedSearchFilter.getSubmissionListColumn());

        newNamedSearchFilter.setName(namedSearchFilter.getName());
        newNamedSearchFilter.setAllColumnSearch(namedSearchFilter.getAllColumnSearch());
        newNamedSearchFilter.setExactMatch(namedSearchFilter.getExactMatch());

        namedSearchFilter.getFilters().forEach(filter -> {
            newNamedSearchFilter.addFilter(filter);
        });

        return namedSearchFilterRepo.save(newNamedSearchFilter);
    }

    @Override
    public void delete(NamedSearchFilter namedSearchFilter) {
        List<FilterCriterion> filterCriteria = new ArrayList<FilterCriterion>(namedSearchFilter.getFilters());

        namedSearchFilter.setFilters(new HashSet<FilterCriterion>());
        namedSearchFilter.setSubmissionListColumn(null);
        namedSearchFilterRepo.deleteById(namedSearchFilter.getId());

        for (FilterCriterion filterCriterion : filterCriteria) {
            if (namedSearchFilterRepo.countByFilterCriteriaId(filterCriterion.getId()) == 0) {
                filterCriterionRepo.delete(filterCriterion);
            }
        }
    }

    @Override
    protected String getChannel() {
        return "/channel/named-search-filter";
    }

}
