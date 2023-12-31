package org.tdl.vireo.model.repo.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.tdl.vireo.exception.ComponentNotPresentOnOrgException;
import org.tdl.vireo.exception.WorkflowStepNonOverrideableException;
import org.tdl.vireo.model.FieldProfile;
import org.tdl.vireo.model.Note;
import org.tdl.vireo.model.Organization;
import org.tdl.vireo.model.WorkflowStep;
import org.tdl.vireo.model.inheritance.HeritableComponent;
import org.tdl.vireo.model.repo.FieldProfileRepo;
import org.tdl.vireo.model.repo.NoteRepo;
import org.tdl.vireo.model.repo.OrganizationRepo;
import org.tdl.vireo.model.repo.WorkflowStepRepo;
import org.tdl.vireo.model.repo.custom.WorkflowStepRepoCustom;

import edu.tamu.weaver.data.model.repo.impl.AbstractWeaverRepoImpl;

public class WorkflowStepRepoImpl extends AbstractWeaverRepoImpl<WorkflowStep, WorkflowStepRepo> implements WorkflowStepRepoCustom {

    final static Logger logger = LoggerFactory.getLogger(WorkflowStepRepoImpl.class);

    @Autowired
    private NoteRepo noteRepo;

    @Autowired
    private FieldProfileRepo fieldProfileRepo;

    @Autowired
    private WorkflowStepRepo workflowStepRepo;

    @Autowired
    private OrganizationRepo organizationRepo;

    @Override
    public WorkflowStep create(String name, Organization originatingOrganization) {
        WorkflowStep workflowStep = workflowStepRepo.save(new WorkflowStep(name, originatingOrganization));
        originatingOrganization.addOriginalWorkflowStep(workflowStep);
        organizationRepo.save(originatingOrganization);
        organizationRepo.broadcast(organizationRepo.findAllByOrderByIdAsc());
        return workflowStepRepo.findById(workflowStep.getId()).get();
    }

    public WorkflowStep reorderFieldProfiles(Organization requestingOrganization, WorkflowStep workflowStep, int src, int dest) throws WorkflowStepNonOverrideableException, ComponentNotPresentOnOrgException {

        if (workflowStep.getOriginatingOrganization().getId().equals(requestingOrganization.getId()) || workflowStep.getOverrideable()) {
            // if requesting organization is not the workflow step's orignating organization
            if (!workflowStep.getOriginatingOrganization().getId().equals(requestingOrganization.getId())) {
                // create a new workflow step
                workflowStep = update(workflowStep, requestingOrganization);
            }

            // reorder aggregate field profiles
            workflowStep.reorderAggregateFieldProfile(src, dest);

            // save workflow step
            return workflowStepRepo.save(workflowStep);
        } else {
            throw new WorkflowStepNonOverrideableException();
        }
    }

    public WorkflowStep swapFieldProfiles(Organization requestingOrganization, WorkflowStep workflowStep, FieldProfile fp1, FieldProfile fp2) throws WorkflowStepNonOverrideableException, ComponentNotPresentOnOrgException {

        if (workflowStep.getOriginatingOrganization().getId().equals(requestingOrganization.getId()) || workflowStep.getOverrideable()) {
            // if requesting organization is not the workflow step's orignating organization
            if (!workflowStep.getOriginatingOrganization().getId().equals(requestingOrganization.getId())) {
                // create a new workflow step
                workflowStep = update(workflowStep, requestingOrganization);
            }

            // swap aggregate field profiles
            workflowStep.swapAggregateFieldProfile(fp1, fp2);

            // save workflow step
            return workflowStepRepo.save(workflowStep);
        } else {
            throw new WorkflowStepNonOverrideableException();
        }
    }

    public WorkflowStep reorderNotes(Organization requestingOrganization, WorkflowStep workflowStep, int src, int dest) throws WorkflowStepNonOverrideableException, ComponentNotPresentOnOrgException {

        if (workflowStep.getOriginatingOrganization().getId().equals(requestingOrganization.getId()) || workflowStep.getOverrideable()) {
            // if requesting organization is not the workflow step's orignating organization
            if (!workflowStep.getOriginatingOrganization().getId().equals(requestingOrganization.getId())) {
                // create a new workflow step
                workflowStep = update(workflowStep, requestingOrganization);
            }

            // reorder aggregate field profiles
            workflowStep.reorderAggregateNote(src, dest);

            // save workflow step
            return workflowStepRepo.save(workflowStep);
        } else {
            throw new WorkflowStepNonOverrideableException();
        }
    }

    public WorkflowStep swapNotes(Organization requestingOrganization, WorkflowStep workflowStep, Note n1, Note n2) throws WorkflowStepNonOverrideableException, ComponentNotPresentOnOrgException {

        if (workflowStep.getOriginatingOrganization().getId().equals(requestingOrganization.getId()) || workflowStep.getOverrideable()) {
            // if requesting organization is not the workflow step's orignating organization
            if (!workflowStep.getOriginatingOrganization().getId().equals(requestingOrganization.getId())) {
                // create a new workflow step
                workflowStep = update(workflowStep, requestingOrganization);
            }

            // swap aggregate field profiles
            workflowStep.swapAggregateNote(n1, n2);

            // save workflow step
            return workflowStepRepo.save(workflowStep);
        } else {
            throw new WorkflowStepNonOverrideableException();
        }
    }

    public void removeFromOrganization(Organization requestingOrg, WorkflowStep workflowStepToRemove) {

        // if requesting organization is the workflow step's orignating organization
        if (requestingOrg.getId().equals(workflowStepToRemove.getOriginatingOrganization().getId())) {
            // the requesting organization is the owning organization so just delete
            workflowStepRepo.delete(workflowStepToRemove);
        } else {
            // the requesting organization is not the owning organization so only remove from aggregate workflowsteps
            requestingOrg.removeAggregateWorkflowStep(workflowStepToRemove);
            organizationRepo.save(requestingOrg);
        }

        organizationRepo.broadcast(organizationRepo.findAllByOrderByIdAsc());
    }

    public WorkflowStep update(WorkflowStep pendingWorkflowStep, Organization requestingOrganization) throws WorkflowStepNonOverrideableException, ComponentNotPresentOnOrgException {

        WorkflowStep resultingWorkflowStep = null;

        WorkflowStep persistedWorkflowStep = workflowStepRepo.findById(pendingWorkflowStep.getId()).get();

        pendingWorkflowStep.setOriginatingOrganization(persistedWorkflowStep.getOriginatingOrganization());

        boolean overridabilityOfPersistedWorkflowStep = persistedWorkflowStep.getOverrideable();

        // The requestingOrganization does not have the workflow step being updated
        if (!requestingOrganization.getAggregateWorkflowSteps().contains(persistedWorkflowStep)) {
            throw new ComponentNotPresentOnOrgException();
        }

        // if the requestingOrganization originates the workflowStep, make the change directly
        if (requestingOrganization.getId().equals(persistedWorkflowStep.getOriginatingOrganization().getId())) {

            logger.info("Requesting organization " + requestingOrganization.getName() + " originates step " + persistedWorkflowStep.getId());

            if (!pendingWorkflowStep.getOverrideable() && overridabilityOfPersistedWorkflowStep) {

                persistedWorkflowStep.setOverrideable(false);

                WorkflowStep savedWorkflowStep = workflowStepRepo.save(persistedWorkflowStep);

                List<WorkflowStep> descendentWorkflowSteps = getDescendantsOfStep(persistedWorkflowStep);

                for (WorkflowStep descendentWorkflowStep : descendentWorkflowSteps) {
                    for (Organization organization : organizationRepo.findByAggregateWorkflowStepsId(descendentWorkflowStep.getId())) {
                        organization.replaceAggregateWorkflowStep(descendentWorkflowStep, savedWorkflowStep);
                        organizationRepo.save(organization);
                    }
                }

                requestingOrganization = organizationRepo.findById(requestingOrganization.getId()).get();

                requestingOrganization.addAggregateWorkflowStep(savedWorkflowStep, requestingOrganization.getAggregateWorkflowSteps().indexOf(savedWorkflowStep));
                organizationRepo.save(requestingOrganization);

                descendentWorkflowSteps.forEach(descendentWorkflowStep -> {
                    delete(descendentWorkflowStep);
                });

                resultingWorkflowStep = savedWorkflowStep;

            } else {
                Boolean overrideable = pendingWorkflowStep.getOverrideable();
                String name = pendingWorkflowStep.getName();

                List<Note> originalNotes = new ArrayList<Note>();
                for (Note n : pendingWorkflowStep.getOriginalNotes()) {
                    originalNotes.add(n);
                }

                List<Note> aggregateNotes = new ArrayList<Note>();
                for (Note n : pendingWorkflowStep.getAggregateNotes()) {
                    aggregateNotes.add(n);
                }

                List<FieldProfile> originalFieldProfiles = new ArrayList<FieldProfile>();
                for (FieldProfile fp : pendingWorkflowStep.getOriginalFieldProfiles()) {
                    originalFieldProfiles.add(fp);
                }

                List<FieldProfile> aggregateFieldProfiles = new ArrayList<FieldProfile>();
                for (FieldProfile fp : pendingWorkflowStep.getAggregateFieldProfiles()) {
                    aggregateFieldProfiles.add(fp);
                }

                persistedWorkflowStep.setOverrideable(overrideable);
                persistedWorkflowStep.setName(name);

                persistedWorkflowStep.setOriginalNotes(originalNotes);
                persistedWorkflowStep.setAggregateNotes(aggregateNotes);

                persistedWorkflowStep.setOriginalFieldProfiles(originalFieldProfiles);
                persistedWorkflowStep.setAggregateFieldProfiles(aggregateFieldProfiles);

                persistedWorkflowStep.setInstructions(pendingWorkflowStep.getInstructions());

                // TODO: handle additional properties to the workflow step

                resultingWorkflowStep = workflowStepRepo.save(persistedWorkflowStep);
            }

        }
        // if the requestingOrganization is not originator of workflowStep, make
        // a new workflow step to override the original
        else {

            logger.info("Requesting organization " + requestingOrganization.getName() + " does not originate step " + persistedWorkflowStep.getId() + " ... making a new step.");

            if (overridabilityOfPersistedWorkflowStep) {

                WorkflowStep clonedWorkflowStep = pendingWorkflowStep.clone();

                Long requestingOrganizationId = requestingOrganization.getId();

                clonedWorkflowStep.setOriginatingWorkflowStep(persistedWorkflowStep);
                clonedWorkflowStep.setOriginatingOrganization(requestingOrganization);

                WorkflowStep newWorkflowStep = workflowStepRepo.save(clonedWorkflowStep);
                logger.info("Created new Workflow Step " + newWorkflowStep.getName() + "(" + newWorkflowStep.getId() + ")" + " originating at Org " + requestingOrganization.getName() + "(" + requestingOrganization.getId() + ")");

                requestingOrganization = organizationRepo.findById(requestingOrganizationId).get();

                // in descendant organizations, replace this overriden workflow step with the override
                for (Organization organization : getContainingDescendantOrganization(requestingOrganization, persistedWorkflowStep)) {
                    organization.replaceAggregateWorkflowStep(persistedWorkflowStep, newWorkflowStep);
                    organizationRepo.save(organization);
                }

                // in descendant organizations, have WSs that originated from
                // the step being overridden now originate from the override
                logger.info("In descendant orgs, have WSs that originated from the step being overridden now originate from the override:");
                for (Organization organization : organizationRepo.getDescendantOrganizations(requestingOrganization)) {
                    logger.info("\t" + organization.getName() + "(" + organization.getId() + ").  Therein orginate WSs: ");
                    for (WorkflowStep ws : organization.getOriginalWorkflowSteps()) {
                        logger.info("\t\t" + ws.getName() + "(" + ws.getId() + ")");
                    }
                }

                Set<WorkflowStep> workflowStepsToSave = new HashSet<WorkflowStep>();
                for (Organization organization : organizationRepo.getDescendantOrganizations(requestingOrganization)) {
                    for (WorkflowStep ws : organization.getOriginalWorkflowSteps()) {
                        if (ws.getOriginatingWorkflowStep() != null && ws.getOriginatingWorkflowStep().equals(persistedWorkflowStep)) {
                            ws.setOriginatingWorkflowStep(newWorkflowStep);
                            workflowStepsToSave.add(ws);
                        }
                    }
                }
                for (WorkflowStep workflowStepToSave : workflowStepsToSave) {
                    workflowStepRepo.save(workflowStepToSave);
                }

                // if change was to make it non-overrideable
                if (!pendingWorkflowStep.getOverrideable()) {

                    List<WorkflowStep> descendentWorkflowSteps = getDescendantsOfStep(persistedWorkflowStep);

                    for (WorkflowStep descendentWorkflowStep : descendentWorkflowSteps) {

                        for (Organization organization : organizationRepo.findByAggregateWorkflowStepsId(descendentWorkflowStep.getId())) {
                            organization.replaceAggregateWorkflowStep(descendentWorkflowStep, newWorkflowStep);
                            organizationRepo.save(organization);
                        }

                        // delete if not belonging to any aggregate
                        if (organizationRepo.findByAggregateWorkflowStepsId(descendentWorkflowStep.getId()).size() == 0) {
                            workflowStepRepo.delete(descendentWorkflowStep);
                        }
                    }

                }

                resultingWorkflowStep = newWorkflowStep;
            }
            // if the workflow step to be updated was not overrideable, then
            // this non-originating organization can't make the change
            else {
                throw new WorkflowStepNonOverrideableException();
            }

        }
        organizationRepo.broadcast(organizationRepo.findAllByOrderByIdAsc());
        return resultingWorkflowStep;
    }

    @Override
    public void delete(WorkflowStep workflowStep) {

        // allows for delete by iterating through findAll, while still deleting descendents
        if (workflowStepRepo.findById(workflowStep.getId()).isPresent()) {

            Organization originatingOrganization = workflowStep.getOriginatingOrganization();

            originatingOrganization.removeOriginalWorkflowStep(workflowStep);

            if (workflowStep.getOriginatingWorkflowStep() != null) {
                workflowStep.setOriginatingWorkflowStep(null);
            }

            organizationRepo.findByAggregateWorkflowStepsId(workflowStep.getId()).forEach(organization -> {
                organization.removeAggregateWorkflowStep(workflowStep);
                organizationRepo.save(organization);
            });

            workflowStepRepo.findByOriginatingWorkflowStep(workflowStep).forEach(ws -> {
                ws.setOriginatingWorkflowStep(null);
            });

            List<FieldProfile> fieldProfilesToDelete = new ArrayList<FieldProfile>();

            fieldProfileRepo.findByOriginatingWorkflowStep(workflowStep).forEach(fp -> {
                fieldProfilesToDelete.add(fp);
            });

            fieldProfilesToDelete.forEach(fp -> {
                fieldProfileRepo.delete(fp);
            });

            List<Note> notesToDelete = new ArrayList<Note>();

            noteRepo.findByOriginatingWorkflowStep(workflowStep).forEach(n -> {
                notesToDelete.add(n);
            });

            notesToDelete.forEach(n -> {
                noteRepo.delete(n);
            });

            deleteDescendantsOfStep(workflowStep);

            workflowStepRepo.deleteById(workflowStep.getId());
        }

    }

    private void deleteDescendantsOfStep(WorkflowStep workflowStep) {
        workflowStepRepo.findByOriginatingWorkflowStep(workflowStep).forEach(desendantWorflowStep -> {
            delete(desendantWorflowStep);
        });
    }

    @Override
    public List<WorkflowStep> getDescendantsOfStep(WorkflowStep workflowStep) {
        List<WorkflowStep> descendantWorkflowSteps = new ArrayList<WorkflowStep>();
        List<WorkflowStep> currentDescendentsWorkflowSteps = workflowStepRepo.findByOriginatingWorkflowStep(workflowStep);
        descendantWorkflowSteps.addAll(currentDescendentsWorkflowSteps);
        currentDescendentsWorkflowSteps.forEach(desendantWorflowStep -> {
            descendantWorkflowSteps.addAll(getDescendantsOfStep(desendantWorflowStep));
        });
        return descendantWorkflowSteps;
    }

    /**
     * Get a list of WorkflowSteps that are descendants of a given WorkflowStep and are on an Organization that descends from given Organization
     */
    @Override
    public List<WorkflowStep> getDescendantsOfStepUnderOrganization(WorkflowStep workflowStep, Organization organization) {

        List<WorkflowStep> allDescendants = getDescendantsOfStep(workflowStep);

        List<WorkflowStep> localDescendants = new ArrayList<WorkflowStep>();

        for (Organization org : organization.getChildrenOrganizations()) {
            for (WorkflowStep ws : org.getOriginalWorkflowSteps()) {
                if (allDescendants.contains(ws) && !localDescendants.contains(ws)) {
                    localDescendants.add(ws);
                }
            }

            for (WorkflowStep candidateDescendant : getDescendantsOfStepUnderOrganization(workflowStep, org)) {
                if (!localDescendants.contains(candidateDescendant)) {
                    localDescendants.add(candidateDescendant);
                }
            }
        }

        return localDescendants;
    }

    @Override
    public List<Organization> getContainingDescendantOrganization(Organization organization, WorkflowStep workflowStep) {
        List<Organization> descendantOrganizationsContainingWorkflowStep = new ArrayList<Organization>();
        if (organization.getAggregateWorkflowSteps().contains(workflowStep)) {
            descendantOrganizationsContainingWorkflowStep.add(organization);
        }
        organization.getChildrenOrganizations().forEach(descendantOrganization -> {
            descendantOrganizationsContainingWorkflowStep.addAll(getContainingDescendantOrganization(descendantOrganization, workflowStep));
        });
        return descendantOrganizationsContainingWorkflowStep;
    }

    @Override
    public List<WorkflowStep> findByAggregateHeritableModel(@SuppressWarnings("rawtypes") HeritableComponent persistedHeritableModel) {
        if (persistedHeritableModel instanceof FieldProfile) {
            return workflowStepRepo.findByAggregateFieldProfilesId(persistedHeritableModel.getId());
        } else if (persistedHeritableModel instanceof Note) {
            return workflowStepRepo.findByAggregateNotesId(persistedHeritableModel.getId());
        } else {
            return new ArrayList<WorkflowStep>();
        }
    }

    @Override
    protected String getChannel() {
        return "/channel/workflow-step";
    }

}
