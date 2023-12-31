package org.tdl.vireo.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tdl.vireo.model.ControlledVocabulary;
import org.tdl.vireo.model.DefaultConfiguration;
import org.tdl.vireo.model.Degree;
import org.tdl.vireo.model.DegreeLevel;
import org.tdl.vireo.model.DocumentType;
import org.tdl.vireo.model.EmailRecipient;
import org.tdl.vireo.model.EmailTemplate;
import org.tdl.vireo.model.EmailWorkflowRule;
import org.tdl.vireo.model.Embargo;
import org.tdl.vireo.model.FieldPredicate;
import org.tdl.vireo.model.FieldProfile;
import org.tdl.vireo.model.GraduationMonth;
import org.tdl.vireo.model.InputType;
import org.tdl.vireo.model.Language;
import org.tdl.vireo.model.ManagedConfiguration;
import org.tdl.vireo.model.Note;
import org.tdl.vireo.model.Organization;
import org.tdl.vireo.model.OrganizationCategory;
import org.tdl.vireo.model.Sort;
import org.tdl.vireo.model.SubmissionListColumn;
import org.tdl.vireo.model.SubmissionStatus;
import org.tdl.vireo.model.VocabularyWord;
import org.tdl.vireo.model.WorkflowStep;
import org.tdl.vireo.model.depositor.SWORDv1Depositor;
import org.tdl.vireo.model.formatter.DSpaceMetsFormatter;
import org.tdl.vireo.model.formatter.DSpaceSimpleFormatter;
import org.tdl.vireo.model.formatter.ExcelFormatter;
import org.tdl.vireo.model.formatter.Marc21Formatter;
import org.tdl.vireo.model.formatter.MarcXML21Formatter;
import org.tdl.vireo.model.formatter.ProQuestUmiFormatter;
import org.tdl.vireo.model.repo.AbstractEmailRecipientRepo;
import org.tdl.vireo.model.repo.AbstractPackagerRepo;
import org.tdl.vireo.model.repo.ConfigurationRepo;
import org.tdl.vireo.model.repo.ControlledVocabularyRepo;
import org.tdl.vireo.model.repo.DegreeLevelRepo;
import org.tdl.vireo.model.repo.DegreeRepo;
import org.tdl.vireo.model.repo.DocumentTypeRepo;
import org.tdl.vireo.model.repo.EmailTemplateRepo;
import org.tdl.vireo.model.repo.EmailWorkflowRuleRepo;
import org.tdl.vireo.model.repo.EmbargoRepo;
import org.tdl.vireo.model.repo.FieldPredicateRepo;
import org.tdl.vireo.model.repo.FieldProfileRepo;
import org.tdl.vireo.model.repo.GraduationMonthRepo;
import org.tdl.vireo.model.repo.InputTypeRepo;
import org.tdl.vireo.model.repo.LanguageRepo;
import org.tdl.vireo.model.repo.NoteRepo;
import org.tdl.vireo.model.repo.OrganizationCategoryRepo;
import org.tdl.vireo.model.repo.OrganizationRepo;
import org.tdl.vireo.model.repo.SubmissionListColumnRepo;
import org.tdl.vireo.model.repo.SubmissionStatusRepo;
import org.tdl.vireo.model.repo.VocabularyWordRepo;
import org.tdl.vireo.model.repo.WorkflowStepRepo;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

@Service
public class SystemDataLoader {

    private final Logger logger = LoggerFactory.getLogger(SystemDataLoader.class);

    private final Pattern SUBJECT_PATTERN = Pattern.compile("\\s*Subject:(.*)[\\n\\r]{1}");

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    @Autowired
    private AssetService fileIOUtility;

    @Autowired
    private InputTypeRepo inputTypeRepo;

    @Autowired
    private EmailTemplateRepo emailTemplateRepo;

    @Autowired
    private AbstractEmailRecipientRepo abstractEmailRecipientRepo;

    @Autowired
    private EmbargoRepo embargoRepo;

    @Autowired
    private OrganizationRepo organizationRepo;

    @Autowired
    private OrganizationCategoryRepo organizationCategoryRepo;

    @Autowired
    private WorkflowStepRepo workflowStepRepo;

    @Autowired
    private NoteRepo noteRepo;

    @Autowired
    private FieldProfileRepo fieldProfileRepo;

    @Autowired
    private ConfigurationRepo configurationRepo;

    @Autowired
    private FieldPredicateRepo fieldPredicateRepo;

    @Autowired
    private ControlledVocabularyRepo controlledVocabularyRepo;

    @Autowired
    private VocabularyWordRepo vocabularyRepo;

    @Autowired
    private LanguageRepo languageRepo;

    @Autowired
    private DegreeRepo degreeRepo;

    @Autowired
    private DegreeLevelRepo degreeLevelRepo;

    @Autowired
    private GraduationMonthRepo graduationMonthRepo;

    @Autowired
    private DocumentTypeRepo documentTypeRepo;

    @Autowired
    private EmailWorkflowRuleRepo emailWorkflowRuleRepo;

    @Autowired
    private SubmissionStatusRepo submissionStatusRepo;

    @Autowired
    private SubmissionListColumnRepo submissionListColumnRepo;

    @Autowired
    private DefaultSubmissionListColumnService defaultSubmissionListColumnService;

    @Autowired
    private DefaultFiltersService defaultFiltersService;

    @Autowired
    private AbstractPackagerRepo abstractPackagerRepo;

    @Autowired
    private ProquestCodesService proquesteCodesService;

    @Autowired
    private DepositorService depositorService;

    @Autowired
    private DefaultSettingsService defaultSettingsService;

    @Transactional
    public void loadSystemData() {

        logger.info("Loading default languages");
        loadLanguages();

        logger.info("Loading default input types");
        loadInputTypes();

        logger.info("Loading default email templates");
        loadEmailTemplates();

        logger.info("Loading default degree levels");
        loadDegreeLevels();

        logger.info("Loading default degrees");
        loadDegrees();

        logger.info("Loading default graduation months");
        loadGraduationMonths();

        logger.info("Loading default embargos");
        loadEmbargos();

        logger.info("Loading default submission statuses");
        loadSubmissionStatuses();

        logger.info("Loading default organization catagories");
        loadOrganizationCategories();

        logger.info("Loading default document types");
        loadDocumentTypes();

        logger.info("Loading default organization");
        loadOrganization();

        logger.info("Loading default controlled vocabularies");
        loadControlledVocabularies();

        logger.info("Loading default settings");
        loadSystemDefaults();

        logger.info("Loading default Proquest language codes");
        loadProquestLanguageCodes();

        logger.info("Loading default Proquest degree codes");
        loadProquestDegreeCodes();

        logger.info("Loading default Proquest subject codes");
        loadProquestSubjectCodes();

        logger.info("Loading default Submission List Columns");
        loadSubmissionListColumns();

        logger.info("Loading default Submission List Columns Filters");
        loadSubmissionListColumnsFilters();

        logger.info("Loading default Packagers");
        loadPackagers();

        logger.info("Loading default Depositors");
        loadDepositors();

        logger.info("Finished loading system defaults");
    }

    private void loadControlledVocabularies() {

        List<File> controlledVocabularyFiles = new ArrayList<File>();
        try {
            controlledVocabularyFiles = fileIOUtility.getResourceDirectoryListing("classpath:/controlled_vocabularies/");
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (File vocabularyJson : controlledVocabularyFiles) {
            try {
                ControlledVocabulary cv = objectMapper.readValue(vocabularyJson, ControlledVocabulary.class);

                // check to see if Controlled Vocabulary exists, and if so, merge up with it
                ControlledVocabulary persistedCV = controlledVocabularyRepo.findByName(cv.getName());

                if (persistedCV == null) {
                    persistedCV = controlledVocabularyRepo.create(cv.getName());
                }

                for (VocabularyWord vw : cv.getDictionary()) {

                    VocabularyWord persistedVW = vocabularyRepo.findByNameAndControlledVocabulary(vw.getName(), persistedCV);

                    if (persistedVW == null) {
                        persistedVW = vocabularyRepo.create(persistedCV, vw.getName(), vw.getDefinition(), vw.getIdentifier(), vw.getContacts());
                        persistedCV = controlledVocabularyRepo.findByName(cv.getName());
                    } else {
                        persistedVW.setDefinition(vw.getDefinition());
                        persistedVW.setIdentifier(vw.getIdentifier());
                        persistedVW.setContacts(vw.getContacts());
                        persistedVW = vocabularyRepo.save(persistedVW);
                    }
                }

            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void loadOrganizationCategories() {
        try {

            List<OrganizationCategory> organizationCategories = objectMapper.readValue(getFileFromResource("classpath:/organization_categories/SYSTEM_Organizaiton_Categories.json"), new TypeReference<List<OrganizationCategory>>() {});

            for (OrganizationCategory organizationCategory : organizationCategories) {
                OrganizationCategory dbOrganizationCategory = organizationCategoryRepo.findByName(organizationCategory.getName());

                if (dbOrganizationCategory == null) {
                    dbOrganizationCategory = organizationCategoryRepo.create(organizationCategory.getName());
                }

            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            logger.debug("Unable to initialize default embargos. ", e);
        }
    }

    private void loadDegreeLevels() {
        try {

            List<DegreeLevel> degreeLevels = objectMapper.readValue(getFileFromResource("classpath:/degree_levels/SYSTEM_Degree_Levels.json"), new TypeReference<List<DegreeLevel>>() {});

            for (DegreeLevel degreeLevel : degreeLevels) {
                DegreeLevel dbDegreeLevel = degreeLevelRepo.findByName(degreeLevel.getName());

                if (dbDegreeLevel == null) {
                    dbDegreeLevel = degreeLevelRepo.create(degreeLevel.getName());
                }

            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            logger.debug("Unable to initialize default degree levels.", e);
        }
    }

    private void loadDegrees() {
        try {

            List<Degree> degrees = objectMapper.readValue(getFileFromResource("classpath:/degrees/SYSTEM_Degrees.json"), new TypeReference<List<Degree>>() {});

            for (Degree degree : degrees) {

                DegreeLevel degreeLevel = degreeLevelRepo.findByName(degree.getLevel().getName());

                if (degreeLevel == null) {
                    degreeLevel = degreeLevelRepo.create(degree.getLevel().getName());
                } else {
                    degreeLevel.setName(degree.getLevel().getName());
                    degreeLevel = degreeLevelRepo.save(degreeLevel);
                }

                Degree dbDegree = degreeRepo.findByNameAndLevel(degree.getName(), degreeLevel);

                if (dbDegree == null) {
                    dbDegree = degreeRepo.create(degree.getName(), degreeLevel);
                } else {
                    dbDegree.setName(degree.getName());
                    dbDegree.setLevel(degreeLevel);
                    dbDegree = degreeRepo.save(dbDegree);
                }

            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            logger.debug("Unable to initialize default degrees.", e);
        }
    }

    private void loadGraduationMonths() {
        try {

            List<GraduationMonth> graduationMonths = objectMapper.readValue(getFileFromResource("classpath:/graduation_months/SYSTEM_Graduation_Months.json"), new TypeReference<List<GraduationMonth>>() {});

            for (GraduationMonth graduationMonth : graduationMonths) {
                GraduationMonth persistedGraduationMonth = graduationMonthRepo.findByMonth(graduationMonth.getMonth());

                if (persistedGraduationMonth == null) {
                    persistedGraduationMonth = graduationMonthRepo.create(graduationMonth.getMonth());
                }

            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            logger.debug("Unable to initialize default graduation months.", e);
        }
    }

    private void loadOrganization() {

        Organization organization = null;

        try {
            // read and map json to Organization
            Organization systemOrganization = objectMapper.readValue(getFileFromResource("classpath:/organization/SYSTEM_Organization_Definition.json"), Organization.class);

            // check to see if organization category exists
            OrganizationCategory category = organizationCategoryRepo.findByName(systemOrganization.getCategory().getName());

            // create organization category if does not already exists
            if (category == null) {
                category = organizationCategoryRepo.create(systemOrganization.getCategory().getName());
            }

            // check to see if organization with organization category exists
            organization = organizationRepo.findByNameAndCategory(systemOrganization.getName(), category);

            // create new organization if not already exists
            if (organization == null) {
                organization = organizationRepo.create(systemOrganization.getName(), category);
                organization.setAcceptsSubmissions(systemOrganization.getAcceptsSubmissions());
            }
            // else set systemOrganization to existing organization
            else {
                systemOrganization = organization;
            }

            List<WorkflowStep> systemOrganizationWorkflowSteps = new ArrayList<>(systemOrganization.getOriginalWorkflowSteps());

            organization.setAggregateWorkflowSteps(processWorkflowSteps(organization, systemOrganizationWorkflowSteps));

            organization = organizationRepo.save(organization);

            processEmailWorflowRules();

        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate system organization", e);
        }

    }

    private List<WorkflowStep> processWorkflowSteps(Organization organization, List<WorkflowStep> systemOrganizationWorkflowSteps) {

        List<WorkflowStep> workflowSteps = new ArrayList<WorkflowStep>();

        for (WorkflowStep workflowStep : systemOrganizationWorkflowSteps) {

            // check to see if the WorkflowStep exists
            WorkflowStep newWorkflowStep = workflowStepRepo.findByNameAndOriginatingOrganization(workflowStep.getName(), organization);

            // create new workflow step if not already exists
            if (newWorkflowStep == null) {
                organization = organizationRepo.findById(organization.getId()).get();
                newWorkflowStep = workflowStepRepo.create(workflowStep.getName(), organization);
            }

            List<FieldProfile> originalFieldProfiles = new ArrayList<>(workflowStep.getOriginalFieldProfiles());

            for (FieldProfile fieldProfile : originalFieldProfiles) {
                // check to see if the FieldPredicate exists
                FieldPredicate fieldPredicate = fieldPredicateRepo.findByValue(fieldProfile.getFieldPredicate().getValue());

                // create new FieldPredicate if not already exists
                if (fieldPredicate == null) {
                    fieldPredicate = fieldPredicateRepo.create(fieldProfile.getFieldPredicate().getValue(), fieldProfile.getFieldPredicate().getDocumentTypePredicate());
                }

                // check to see if the InputType exists
                InputType inputType = inputTypeRepo.findByName(fieldProfile.getInputType().getName());

                if (inputType == null) {
                    inputType = inputTypeRepo.create(fieldProfile.getInputType().getName());
                }

                // check to see if the ControlledVocabulary exists
                ControlledVocabulary controlledVocabulary = fieldProfile.getControlledVocabulary();

                // create new ControlledVocabulary if not already exists
                if (controlledVocabulary != null) {
                    ControlledVocabulary persistedControlledVocabulary = controlledVocabularyRepo.findByName(controlledVocabulary.getName());
                    if (persistedControlledVocabulary == null) {
                        persistedControlledVocabulary = controlledVocabularyRepo.create(controlledVocabulary.getName(), controlledVocabulary.getIsEntityProperty());
                    }
                    fieldProfile.setControlledVocabulary(persistedControlledVocabulary);
                }

                // check to see if the ManagedConfiguration exists
                ManagedConfiguration managedConfiguration = fieldProfile.getMappedShibAttribute();

                // create new ManagedConfiguration if not already exists
                if (managedConfiguration != null) {
                    ManagedConfiguration persistedManagedConfiguration = configurationRepo.findByNameAndType(managedConfiguration.getName(), managedConfiguration.getType());
                    if (persistedManagedConfiguration == null) {
                        persistedManagedConfiguration = configurationRepo.create(managedConfiguration);
                    }
                    fieldProfile.setMappedShibAttribute(persistedManagedConfiguration);
                }

                // check to see if the FieldProfile exists
                FieldProfile newFieldProfile = fieldProfileRepo.findByFieldPredicateAndOriginatingWorkflowStep(fieldPredicate, newWorkflowStep);

                // create new FieldProfile if not already exists
                if (newFieldProfile == null) {
                    newWorkflowStep = workflowStepRepo.findById(newWorkflowStep.getId()).get();
                    newFieldProfile = fieldProfileRepo.create(newWorkflowStep, fieldPredicate, inputType, fieldProfile.getUsage(), fieldProfile.getHelp(), fieldProfile.getGloss(), fieldProfile.getRepeatable(), fieldProfile.getOverrideable(), fieldProfile.getEnabled(), fieldProfile.getOptional(), fieldProfile.getHidden(), fieldProfile.getFlagged(), fieldProfile.getLogged(), fieldProfile.getControlledVocabulary(), fieldProfile.getMappedShibAttribute(), fieldProfile.getDefaultValue());
                }

                newWorkflowStep.addOriginalFieldProfile(newFieldProfile);

                newWorkflowStep = workflowStepRepo.save(newWorkflowStep);
            }

            // temporary list of Note
            List<Note> notes = new ArrayList<Note>();

            for (Note note : workflowStep.getOriginalNotes()) {

                // check to see if the Note exists
                Note newNote = noteRepo.findByNameAndOriginatingWorkflowStep(note.getName(), newWorkflowStep);

                // create new Note if not already exists
                if (newNote == null) {
                    newNote = noteRepo.create(newWorkflowStep, note.getName(), note.getText());
                    newWorkflowStep = workflowStepRepo.findById(newWorkflowStep.getId()).get();
                }

                notes.add(newNote);

            }

            newWorkflowStep.setOriginalNotes(notes);

            newWorkflowStep.setInstructions(workflowStep.getInstructions());

            newWorkflowStep = workflowStepRepo.save(newWorkflowStep);

            workflowSteps.add(newWorkflowStep);
        }
        return workflowSteps;
    }

    private void processEmailWorflowRules() {

        Organization organization = organizationRepo.findById(1L).get();

        try {
            Organization systemOrganization = objectMapper.readValue(getFileFromResource("classpath:/organization/SYSTEM_Organization_Definition.json"), Organization.class);

            // temporary set of EmailWorkflowRule
            List<EmailWorkflowRule> emailWorkflowRules = new ArrayList<EmailWorkflowRule>();

            systemOrganization.getEmailWorkflowRules().forEach(emailWorkflowRule -> {

                // check to see if the SubmissionStatus exists
                SubmissionStatus newSubmissionStatus = submissionStatusRepo.findByName(emailWorkflowRule.getSubmissionStatus().getName());

                // create new SubmissionStatus if not already exists
                if (newSubmissionStatus == null) {
                    newSubmissionStatus = submissionStatusRepo.create(emailWorkflowRule.getSubmissionStatus().getName(), emailWorkflowRule.getSubmissionStatus().isArchived(), emailWorkflowRule.getSubmissionStatus().isPublishable(), emailWorkflowRule.getSubmissionStatus().isDeletable(), emailWorkflowRule.getSubmissionStatus().isEditableByReviewer(), emailWorkflowRule.getSubmissionStatus().isEditableByStudent(), emailWorkflowRule.getSubmissionStatus().isActive(),
                            emailWorkflowRule.getSubmissionStatus().getSubmissionState());
                    newSubmissionStatus = submissionStatusRepo.save(recursivelyFindOrCreateSubmissionStatus(emailWorkflowRule.getSubmissionStatus()));
                }

                // check to see if the EmailTemplate exists
                EmailTemplate newEmailTemplate = emailTemplateRepo.findByNameAndSystemRequired(emailWorkflowRule.getEmailTemplate().getName(), emailWorkflowRule.getEmailTemplate().getSystemRequired());

                // create new EmailTemplate if not already exists
                if (newEmailTemplate == null) {
                    newEmailTemplate = emailTemplateRepo.create(emailWorkflowRule.getEmailTemplate().getName(), emailWorkflowRule.getEmailTemplate().getSubject(), emailWorkflowRule.getEmailTemplate().getMessage());
                }

                if (emailWorkflowRule.getEmailRecipient() == null) {

                    if (newEmailTemplate.getName().equals("SYSTEM Advisor Review Request")) {
                        organization.getAggregateWorkflowSteps().forEach(awfs -> {
                            awfs.getAggregateFieldProfiles().forEach(afp -> {
                                if (afp.getFieldPredicate().getValue().equals("dc.contributor.advisor")) {
                                    EmailRecipient recipient = abstractEmailRecipientRepo.createContactRecipient(afp.getGloss(), afp.getFieldPredicate());
                                    emailWorkflowRule.setEmailRecipient(recipient);
                                }
                            });
                        });

                    }

                    if (newEmailTemplate.getName().equals("SYSTEM Initial Submission")) {
                        EmailRecipient recipient = abstractEmailRecipientRepo.createOrganizationRecipient(organization);
                        emailWorkflowRule.setEmailRecipient(recipient);
                    }

                }

                // check to see if the EmailWorkflowRule exists
                EmailWorkflowRule newEmailWorkflowRule = emailWorkflowRuleRepo.findBySubmissionStatusAndEmailRecipientAndEmailTemplate(newSubmissionStatus, emailWorkflowRule.getEmailRecipient(), newEmailTemplate);

                if (newEmailWorkflowRule == null) {
                    newEmailWorkflowRule = emailWorkflowRuleRepo.create(newSubmissionStatus, emailWorkflowRule.getEmailRecipient(), newEmailTemplate, emailWorkflowRule.isSystem());
                }

                emailWorkflowRules.add(newEmailWorkflowRule);

            });

            organization.setEmailWorkflowRules(emailWorkflowRules);

            organizationRepo.save(organization);

        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate system organization", e);
        }

    }

    private void loadSubmissionStatuses() {
        try {
            File statusesFile = getFileFromResource("classpath:/submission_statuses/SYSTEM_Submission_Statuses.json");
            File transitionsFile = getFileFromResource("classpath:/submission_statuses/SYSTEM_Submission_Statuses_Transitions.json");

            // read and map json to SubmissionStatus
            TypeFactory tf = TypeFactory.defaultInstance();
            CollectionType type = tf.constructCollectionType(ArrayList.class, SubmissionStatus.class);
            List<SubmissionStatus> systemSubmissionStatuses = objectMapper.readValue(statusesFile, type);

            systemSubmissionStatuses.forEach(ss -> {
                SubmissionStatus found = submissionStatusRepo.findByName(ss.getName());

                if (found == null) {
                    ss.setTransitionSubmissionStatuses(new ArrayList<>());
                    found = submissionStatusRepo.create(ss.getName(), ss.isArchived(), ss.isPublishable(), ss.isDeletable(), ss.isEditableByReviewer(), ss.isEditableByStudent(), ss.isActive(), ss.getSubmissionState());
                }
            });

            type = tf.constructCollectionType(ArrayList.class, HashMap.class);
            List<HashMap<?, ?>> transitions = objectMapper.readValue(transitionsFile, type);

            for (Map<?, ?> transition : transitions) {
                List<SubmissionStatus> list = new ArrayList<>();
                Long transitionId = ((Integer) transition.get("id")).longValue();
                SubmissionStatus ss = submissionStatusRepo.findById(transitionId).get();

                for (Object idObject : (ArrayList<?>) transition.get("transitionSubmissionStatuses")) {
                    Long id = ((Integer) idObject).longValue();
                    Optional<SubmissionStatus> submissionStatus = submissionStatusRepo.findById(id);
                    if (submissionStatus.isPresent()) {
                        list.add(submissionStatus.get());
                    }
                }

                ss.setTransitionSubmissionStatuses(list);
                submissionStatusRepo.save(ss);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate system organization", e);
        }
    }

    private SubmissionStatus recursivelyFindOrCreateSubmissionStatus(SubmissionStatus submissionStatus) {

        // check to see if the SubmissionStatus exists
        SubmissionStatus newSubmissionStatus = submissionStatusRepo.findByName(submissionStatus.getName());

        // create new SubmissionStatus if not already exists
        if (newSubmissionStatus == null) {
            newSubmissionStatus = submissionStatusRepo.create(submissionStatus.getName(), submissionStatus.isArchived(), submissionStatus.isPublishable(), submissionStatus.isDeletable(), submissionStatus.isEditableByReviewer(), submissionStatus.isEditableByStudent(), submissionStatus.isActive(), submissionStatus.getSubmissionState());
        }

        // temporary list of SubmissionState
        List<SubmissionStatus> transitionStatuses = new ArrayList<SubmissionStatus>();

        submissionStatus.getTransitionSubmissionStatuses().forEach(transitionStatus -> {

            // check to see if the Transistion SubmissionStatus exists
            SubmissionStatus newTransitionStatus = submissionStatusRepo.findByName(transitionStatus.getName());

            // create new Transistion SubmissionStatus if not already exists
            if (newTransitionStatus == null) {
                newTransitionStatus = submissionStatusRepo.create(transitionStatus.getName(), transitionStatus.isArchived(), transitionStatus.isPublishable(), transitionStatus.isDeletable(), transitionStatus.isEditableByReviewer(), transitionStatus.isEditableByStudent(), transitionStatus.isActive(), transitionStatus.getSubmissionState());
                newTransitionStatus = submissionStatusRepo.save(recursivelyFindOrCreateSubmissionStatus(transitionStatus));
            }

            transitionStatuses.add(newTransitionStatus);

        });

        newSubmissionStatus.setTransitionSubmissionStatuses(transitionStatuses);

        return submissionStatusRepo.save(newSubmissionStatus);
    }

    private EmailTemplate loadSystemEmailTemplate(String name) {

        try {
            Resource resource = resourcePatternResolver.getResource("classpath:/emails/" + encodeTemplateName(name));
            String data = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);

            // Remove any comment lines
            data = data.replaceAll("\\s*#.*[\\n\\r]{1}", "");

            // Extract the subject
            Matcher subjectMatcher = SUBJECT_PATTERN.matcher(data);
            if (!subjectMatcher.find()) {
                throw new IllegalStateException("Unable to identify the template's subject.");
            }
            String subject = subjectMatcher.group(1).trim();

            // Trim the subject leaving just the body.
            int index = data.indexOf("\n");
            if (index < 0) {
                index = data.indexOf("\r");
            }

            String message = data.substring(index);

            if (subject == null || subject.length() == 0) {
                throw new IllegalStateException("Unable to identify the template's subject.");
            }

            if (message == null || message.length() == 0) {
                throw new IllegalStateException("Unable to identify the template's message.");
            }

            EmailTemplate template = emailTemplateRepo.findByNameAndSystemRequired(name, true);

            if (template == null) {
                template = emailTemplateRepo.create(name, subject, message);
                template.setSystemRequired(true);
            } else {
                template.setSubject(subject);
                template.setMessage(message);
            }

            return emailTemplateRepo.save(template);

        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate system email template: " + name, e);
        }
    }

    private void loadLanguages() {
        try {
            List<Language> languages = objectMapper.readValue(getFileFromResource("classpath:/languages/SYSTEM_Languages.json"), new TypeReference<List<Language>>() {});

            for (Language language : languages) {
                Language persistedLanguage = languageRepo.findByName(language.getName());

                if (persistedLanguage == null) {
                    persistedLanguage = languageRepo.create(language.getName());
                } else {
                    persistedLanguage.setName(language.getName());
                    persistedLanguage = languageRepo.save(persistedLanguage);
                }
            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            logger.debug("Unable to initialize default languages. ", e);
        }
    }

    private void loadInputTypes() {
        try {
            List<InputType> inputTypes = objectMapper.readValue(getFileFromResource("classpath:/input_types/SYSTEM_Input_Types.json"), new TypeReference<List<InputType>>() {});

            for (InputType inputType : inputTypes) {
                InputType persistedInputType = inputTypeRepo.findByName(inputType.getName());

                if (persistedInputType == null) {
                    persistedInputType = inputTypeRepo.create(inputType);
                } else {
                    persistedInputType.setName(inputType.getName());
                    persistedInputType = inputTypeRepo.save(persistedInputType);
                }
            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            logger.debug("Unable to initialize default input types. ", e);
        }
    }

    private void loadEmailTemplates() {

        for (String name : getAllSystemEmailTemplateNames()) {

            // try to see if it already exists in the DB
            EmailTemplate dbTemplate = emailTemplateRepo.findByNameAndSystemRequired(name, true);

            // create template or upgrade the old one
            if (dbTemplate == null) {
                dbTemplate = loadSystemEmailTemplate(name);
                logger.info("New System Email template being installed [" + dbTemplate.getName() + "]");
            } else {
                EmailTemplate loadedTemplate = loadSystemEmailTemplate(name);

                // if the template in the DB doesn't match in content with the
                // one loaded from .email file
                if (!(dbTemplate.getMessage().equals(loadedTemplate.getMessage())) || !(dbTemplate.getSubject().equals(loadedTemplate.getSubject()))) {

                    EmailTemplate possibleCustomTemplate = emailTemplateRepo.findByNameAndSystemRequired(name, false);

                    // if this System template already has a custom template
                    // (meaning one named the same but that is
                    // !systemRequired)
                    if (possibleCustomTemplate != null) {

                        // a custom version of this System email template
                        // already exists, it's safe to override dbTemplate's
                        // data and save
                        dbTemplate.setMessage(loadedTemplate.getMessage());
                        dbTemplate.setSubject(loadedTemplate.getSubject());
                        dbTemplate.setSystemRequired(true);

                        logger.info("Upgrading Old System Email Template for [" + dbTemplate.getName() + "]");

                        emailTemplateRepo.save(dbTemplate);
                    }
                    // there is no custom one yet, we need to make the
                    // dbTemplate !systemRequired and the save loadedTemplate
                    else {
                        logger.info("Upgrading Old System Email Template and creating custom version for [" + dbTemplate.getName() + "]");
                        dbTemplate.setSystemRequired(false);
                        emailTemplateRepo.save(dbTemplate);
                        emailTemplateRepo.save(loadedTemplate);
                    }
                }
            }
        }
    }

    private void loadSubmissionListColumns() {

        try {

            List<SubmissionListColumn> submissionListColumns = objectMapper.readValue(getFileFromResource("classpath:/submission_list_columns/SYSTEM_Default_Submission_List_Columns.json"), new TypeReference<List<SubmissionListColumn>>() {});

            for (SubmissionListColumn submissionListColumn : submissionListColumns) {
                SubmissionListColumn dbSubmissionListColumn = submissionListColumnRepo.findByTitle(submissionListColumn.getTitle());

                // check to see if the InputType exists
                InputType inputType = inputTypeRepo.findByName(submissionListColumn.getInputType().getName());

                if (inputType == null) {
                    inputType = inputTypeRepo.create(submissionListColumn.getInputType().getName());
                } else {
                    inputType.setName(submissionListColumn.getInputType().getName());
                    inputType = inputTypeRepo.save(inputType);
                }

                if (dbSubmissionListColumn == null) {
                    if (submissionListColumn.getPredicate() != null) {
                        submissionListColumnRepo.create(submissionListColumn.getTitle(), submissionListColumn.getSort(), submissionListColumn.getPredicate(), inputType);
                    } else {
                        submissionListColumnRepo.create(submissionListColumn.getTitle(), submissionListColumn.getSort(), submissionListColumn.getValuePath(), inputType);
                    }
                } else {
                    dbSubmissionListColumn.setSort(submissionListColumn.getSort());
                    if (submissionListColumn.getPredicate() != null) {
                        dbSubmissionListColumn.setPredicate(submissionListColumn.getPredicate());
                    }
                    dbSubmissionListColumn.setValuePath(submissionListColumn.getValuePath());
                    submissionListColumnRepo.save(dbSubmissionListColumn);
                }
            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            logger.debug("Unable to initialize submission list columns. ", e);
        }

        try {

            String[] defaultSubmissionListColumnTitles = objectMapper.readValue(getFileFromResource("classpath:/submission_list_columns/SYSTEM_Default_Submission_List_Column_Titles.json"), new TypeReference<String[]>() {
            });
            int count = 0;
            for (String defaultTitle : defaultSubmissionListColumnTitles) {
                SubmissionListColumn dbSubmissionListColumn = submissionListColumnRepo.findByTitle(defaultTitle);
                if (dbSubmissionListColumn != null) {
                    if (dbSubmissionListColumn.getSort() != Sort.NONE) {
                        dbSubmissionListColumn.setSortOrder(++count);
                        defaultSubmissionListColumnService.addDefaultSubmissionListColumn(submissionListColumnRepo.save(dbSubmissionListColumn));
                    } else {
                        defaultSubmissionListColumnService.addDefaultSubmissionListColumn(dbSubmissionListColumn);
                    }
                } else {
                    logger.warn("Unable to find submission list column with title " + defaultTitle);
                }
            }

        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            logger.debug("Unable to initialize default submission list column titles. ", e);
        }

    }

    private void loadSubmissionListColumnsFilters() {
        try {
            List<SubmissionListColumn> defaultFilterColumns = objectMapper.readValue(getFileFromResource("classpath:/filter_columns/default_filter_columns.json"), new TypeReference<List<SubmissionListColumn>>() {});

            for (SubmissionListColumn defaultFilterColumn : defaultFilterColumns) {
                SubmissionListColumn dbDefaultFilterColumn = submissionListColumnRepo.findByTitle(defaultFilterColumn.getTitle());
                if (dbDefaultFilterColumn != null) {
                    defaultFiltersService.addDefaultFilter(dbDefaultFilterColumn);
                } else {
                    logger.warn("Unable to find default filter for column " + defaultFilterColumn.getTitle() + "!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> getAllSystemEmailTemplateNames() {
        try {

            List<String> names = new ArrayList<String>();
            for (Resource resource : resourcePatternResolver.getResources("classpath:/emails/*.email")) {
                String fileName = resource.getFilename();
                String templateName = decodeTemplateName(fileName);
                names.add(templateName);
            }

            return names;

        } catch (IOException e) {
            throw new IllegalStateException("Unable to get emails directory from classpath.");
        }
    }

    private void loadEmbargos() {

        try {

            List<Embargo> embargoDefinitions = objectMapper.readValue(getFileFromResource("classpath:/embargos/SYSTEM_Embargo_Definitions.json"), new TypeReference<List<Embargo>>() {});

            for (Embargo embargoDefinition : embargoDefinitions) {
                Embargo dbEmbargo = embargoRepo.findByNameAndGuarantorAndSystemRequired(embargoDefinition.getName(), embargoDefinition.getGuarantor(), true);

                if (dbEmbargo == null) {
                    dbEmbargo = embargoRepo.create(embargoDefinition.getName(), embargoDefinition.getDescription(), embargoDefinition.getDuration(), embargoDefinition.getGuarantor(), embargoDefinition.isActive());
                    dbEmbargo.setSystemRequired(true);
                    embargoRepo.save(dbEmbargo);
                } else {
                    dbEmbargo.setDescription(embargoDefinition.getDescription());
                    dbEmbargo.setDuration(embargoDefinition.getDuration());
                    dbEmbargo.setGuarantor(embargoDefinition.getGuarantor());
                    dbEmbargo.isActive(embargoDefinition.isActive());
                    dbEmbargo.setSystemRequired(embargoDefinition.getSystemRequired());
                    embargoRepo.save(dbEmbargo);
                }
            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            logger.debug("Unable to initialize default embargos. ", e);
        }
    }

    public void loadSystemDefaults() {
        try {
            JsonNode systemDefaults = objectMapper.readTree(getFileFromResource("classpath:/settings/SYSTEM_Defaults.json"));
            Iterator<Entry<String, JsonNode>> it = systemDefaults.fields();

            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) it.next();
                List<DefaultConfiguration> defaultConfigurations = new ArrayList<DefaultConfiguration>();
                if (entry.getValue().isArray()) {
                    for (JsonNode objNode : entry.getValue()) {
                        objNode.fieldNames().forEachRemaining(n -> {
                            defaultConfigurations.add(new DefaultConfiguration(n, objNode.get(n).asText(), entry.getKey()));
                        });
                    }
                }
                defaultSettingsService.addSettings(entry.getKey(), defaultConfigurations);
            }
            defaultSettingsService.getTypes().forEach(t -> {
                logger.debug("Stored preferences for type: " + t);
                defaultSettingsService.getSettingsByType(t).forEach(c -> {
                    logger.debug(c.getName() + ": " + c.getValue());
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDocumentTypes() {

        try {

            List<DocumentType> documentTypes = objectMapper.readValue(getFileFromResource("classpath:/document_types/SYSTEM_Document_Types.json"), new TypeReference<List<DocumentType>>() {});

            for (DocumentType documentType : documentTypes) {

                FieldPredicate fieldPredicate = documentType.getFieldPredicate();

                FieldPredicate dbFieldPredicate = fieldPredicateRepo.findByValue(fieldPredicate.getValue());
                if (dbFieldPredicate == null) {
                    dbFieldPredicate = fieldPredicateRepo.create(fieldPredicate.getValue(), Boolean.valueOf(true));
                } else {
                    dbFieldPredicate.setValue(fieldPredicate.getValue());
                    dbFieldPredicate.setDocumentTypePredicate(fieldPredicate.getDocumentTypePredicate());
                    dbFieldPredicate = fieldPredicateRepo.save(dbFieldPredicate);
                }

                DocumentType dbDocumentType = documentTypeRepo.findByNameAndFieldPredicate(documentType.getName(), dbFieldPredicate);

                if (dbDocumentType == null) {
                    dbDocumentType = documentTypeRepo.create(documentType.getName(), dbFieldPredicate);
                } else {
                    dbDocumentType.setName(documentType.getName());
                    dbDocumentType.setFieldPredicate(dbFieldPredicate);
                    documentTypeRepo.save(dbDocumentType);
                }
            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            logger.debug("Unable to initialize default document types. ", e);
        }
    }

    private void loadProquestLanguageCodes() {
        proquesteCodesService.setCodes("languages", getProquestCodes("language_codes.xls"));
    }

    private void loadProquestDegreeCodes() {
        proquesteCodesService.setCodes("degrees", getProquestCodes("degree_codes.xls"));
    }

    private void loadProquestSubjectCodes() {
        Map<String, String> subjectCodes = getProquestCodes("umi_subjects.xls");

        proquesteCodesService.setCodes("subjects", subjectCodes);

        createSubjectControlledVocabulary(subjectCodes);
    }

    private void createSubjectControlledVocabulary(Map<String, String> subjectCodes) {

        // check to see if Controlled Vocabulary exists, and if so, merge up with it
        ControlledVocabulary persistedCV = controlledVocabularyRepo.findByName("Subjects");

        if (persistedCV == null) {
            persistedCV = controlledVocabularyRepo.create("Subjects");
        }

        for (Map.Entry<String, String> entry : subjectCodes.entrySet()) {
            String code = entry.getKey();
            String description = entry.getValue();

            VocabularyWord persistedVW = vocabularyRepo.findByNameAndControlledVocabulary(description, persistedCV);

            if (persistedVW == null) {
                persistedVW = vocabularyRepo.create(persistedCV, description, "", code, new ArrayList<String>());
                persistedCV = controlledVocabularyRepo.findByName("Subjects");
            } else {
                persistedVW.setDefinition("");
                persistedVW.setIdentifier(code);
                persistedVW = vocabularyRepo.save(persistedVW);
            }
        }

    }

    private Map<String, String> getProquestCodes(String xslFileName) {
        Map<String, String> proquestCodes = new HashMap<String, String>();
        Resource resource = resourcePatternResolver.getResource("classpath:/proquest/" + xslFileName);

        InputStream file = null;
        try {
            file = resource.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file != null) {

            HSSFWorkbook workbook = null;
            try {
                workbook = new HSSFWorkbook(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (workbook != null) {

                HSSFSheet sheet = workbook.getSheetAt(0);

                Iterator<Row> rowIterator = sheet.iterator();
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();

                    String code = null, description = "";

                    Iterator<Cell> cellIterator = row.cellIterator();
                    while (cellIterator.hasNext()) {

                        Cell cell = cellIterator.next();
                        if (cell.getCellType() == CellType.STRING) {
                            String cellValue = cell.getStringCellValue();
                            if (code == null) {
                                code = cellValue;
                            } else {
                                description = cellValue;
                            }
                        }
                    }

                    proquestCodes.put(code, description);
                }
            }

            try {
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return proquestCodes;
    }

    private void loadPackagers() {
        if (abstractPackagerRepo.findByName("DSpaceMETS") == null) {
            abstractPackagerRepo.createDSpaceMetsPackager("DSpaceMETS", new DSpaceMetsFormatter());
        }
        if (abstractPackagerRepo.findByName("DSpaceSimple") == null) {
            abstractPackagerRepo.createDSpaceSimplePackager("DSpaceSimple", new DSpaceSimpleFormatter());
        }
        if (abstractPackagerRepo.findByName("ProQuest") == null) {
            abstractPackagerRepo.createProQuestUmiPackager("ProQuest", new ProQuestUmiFormatter());
        }
        if (abstractPackagerRepo.findByName("Excel") == null) {
            abstractPackagerRepo.createExcelPackager("Excel", new ExcelFormatter());
        }
        if (abstractPackagerRepo.findByName("MarcXML21") == null) {
            abstractPackagerRepo.createMARC21XMLPackager("MarcXML21", new MarcXML21Formatter());
        }
        if (abstractPackagerRepo.findByName("Marc21") == null) {
            abstractPackagerRepo.createMARC21Packager("Marc21", new Marc21Formatter());
        }
    }

    private void loadDepositors() {
        depositorService.addDepositor(new SWORDv1Depositor());
    }

    private static String encodeTemplateName(String name) {
        return name.replaceAll(" ", "_") + ".email";
    }

    private String decodeTemplateName(String path) {
        if (path.endsWith(".email")) {
            path = path.substring(0, path.length() - ".email".length());
        }
        return path.replaceAll("_", " ");
    }

    private File getFileFromResource(String resourcePath) throws IOException {
        return fileIOUtility.getFileFromResource(resourcePath);
    }

}
