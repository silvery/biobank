package edu.ualberta.med.biobank.common.wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import edu.ualberta.med.biobank.common.action.eventattr.EventAttrTypeEnum;
import edu.ualberta.med.biobank.common.exception.BiobankCheckException;
import edu.ualberta.med.biobank.common.exception.BiobankException;
import edu.ualberta.med.biobank.common.exception.BiobankQueryResultSizeException;
import edu.ualberta.med.biobank.common.peer.AliquotedSpecimenPeer;
import edu.ualberta.med.biobank.common.peer.ClinicPeer;
import edu.ualberta.med.biobank.common.peer.CollectionEventPeer;
import edu.ualberta.med.biobank.common.peer.ContactPeer;
import edu.ualberta.med.biobank.common.peer.PatientPeer;
import edu.ualberta.med.biobank.common.peer.StudyPeer;
import edu.ualberta.med.biobank.common.wrappers.WrapperTransaction.TaskList;
import edu.ualberta.med.biobank.common.wrappers.base.StudyBaseWrapper;
import edu.ualberta.med.biobank.common.wrappers.internal.EventAttrTypeWrapper;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.AliquotedSpecimen;
import edu.ualberta.med.biobank.model.CollectionEvent;
import edu.ualberta.med.biobank.model.Contact;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.Study;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class StudyWrapper extends StudyBaseWrapper {

    private Map<String, StudyEventAttrWrapper> studyEventAttrMap;

    public StudyWrapper(WritableApplicationService appService,
        Study wrappedObject) {
        super(appService, wrappedObject);
    }

    public StudyWrapper(WritableApplicationService appService) {
        super(appService);
    }

    protected Collection<StudyEventAttrWrapper> getStudyEventAttrCollection() {
        Map<String, StudyEventAttrWrapper> map = getStudyEventAttrMap();
        if (map == null) {
            return null;
        }
        return map.values();
    }

    private Map<String, StudyEventAttrWrapper> getStudyEventAttrMap() {
        if (studyEventAttrMap != null)
            return studyEventAttrMap;

        studyEventAttrMap = new HashMap<String, StudyEventAttrWrapper>();

        List<StudyEventAttrWrapper> eventAttrList =
            getStudyEventAttrCollection(false);
        // StudyEventAttrWrapper.getStudyEventAttrCollection(this);

        for (StudyEventAttrWrapper studyEventAttr : eventAttrList) {
            studyEventAttrMap.put(studyEventAttr.getGlobalEventAttr()
                .getLabel(), studyEventAttr);
        }
        return studyEventAttrMap;
    }

    private void updateStudyEventAttrCollection() {
        if (studyEventAttrMap != null) {
            List<StudyEventAttrWrapper> allStudyEventAttrWrappers =
                new ArrayList<StudyEventAttrWrapper>();
            for (StudyEventAttrWrapper ss : studyEventAttrMap.values()) {
                allStudyEventAttrWrappers.add(ss);
            }
            setWrapperCollection(StudyPeer.STUDY_EVENT_ATTRS,
                allStudyEventAttrWrappers);
        }
    }

    public String[] getStudyEventAttrLabels() {
        getStudyEventAttrMap();
        return studyEventAttrMap.keySet().toArray(new String[] {});
    }

    public StudyEventAttrWrapper getStudyEventAttr(String label)
        throws Exception {
        getStudyEventAttrMap();
        StudyEventAttrWrapper studyEventAttr = studyEventAttrMap.get(label);
        if (studyEventAttr == null) {
            throw new Exception("StudyEventAttr with label \"" + label //$NON-NLS-1$
                + "\" is invalid"); //$NON-NLS-1$
        }
        return studyEventAttr;
    }

    public EventAttrTypeEnum getStudyEventAttrType(String label)
        throws Exception {
        return EventAttrTypeEnum.getEventAttrType(getStudyEventAttr(label)
            .getGlobalEventAttr().getEventAttrType().getName());
    }

    /**
     * Retrieves the permissible values for a patient visit attribute.
     * 
     * @param label The label to be used by the attribute.
     * @return Semicolon separated list of allowed values.
     * @throws Exception hrown if there is no patient visit information item with the label
     *             specified.
     */
    public String[] getStudyEventAttrPermissible(String label) throws Exception {
        String joinedPossibleValues = getStudyEventAttr(label).getPermissible();
        if (joinedPossibleValues == null)
            return null;
        return joinedPossibleValues.split(";"); //$NON-NLS-1$
    }

    /**
     * Retrieves the activity status for a patient visit attribute. If locked, patient visits will
     * not allow information to be saved for this attribute.
     * 
     * @param label
     * @return True if the attribute is locked. False otherwise.
     * @throws Exception
     */
    public ActivityStatus getStudyEventAttrActivityStatus(String label)
        throws Exception {
        return getStudyEventAttr(label).getActivityStatus();
    }

    /**
     * Assigns patient visit attributes to be used for this study.
     * 
     * @param label The label used for the attribute.
     * @param type The string corresponding to the type of the attribute.
     * @param permissibleValues If the attribute is of type "select_single" or "select_multiple"
     *            this array contains the possible values as a String array. Otherwise, this
     *            parameter should be set to null.
     * 
     * @throws Exception Thrown if the attribute type does not exist.
     */
    public void setStudyEventAttr(String label, EventAttrTypeEnum type,
        String[] permissibleValues) throws Exception {
        Map<String, EventAttrTypeWrapper> EventAttrTypeMap =
            EventAttrTypeWrapper
                .getAllEventAttrTypesMap(appService);
        EventAttrTypeWrapper EventAttrType = EventAttrTypeMap.get(type
            .getName());
        if (EventAttrType == null) {
            throw new Exception("the pv attribute type \"" + type //$NON-NLS-1$
                + "\" does not exist"); //$NON-NLS-1$
        }

        StudyEventAttrWrapper studyEventAttr = getStudyEventAttrMap()
            .get(label);

        if (type.isSelectType()) {
            // type has permissible values
            if ((studyEventAttr == null) && (permissibleValues == null)) {
                // nothing to do
                return;
            }

            if ((studyEventAttr != null) && (permissibleValues == null)) {
                deleteStudyEventAttr(label);
                return;
            }
        }

        if (studyEventAttr == null) {
            // does not yet exist
            studyEventAttr = new StudyEventAttrWrapper(appService);
            studyEventAttr.getGlobalEventAttr().setLabel(label);
            studyEventAttr.getGlobalEventAttr().setEventAttrType(EventAttrType);
            studyEventAttr.setStudy(this);
        }

        studyEventAttr.setActivityStatus(ActivityStatus.ACTIVE);
        studyEventAttr.setPermissible(StringUtils.join(permissibleValues, ';'));
        studyEventAttrMap.put(label, studyEventAttr);

        updateStudyEventAttrCollection();
    }

    /**
     * Assigns patient visit attributes to be used for this study.
     * 
     * @param label The label to be used for the attribute.
     * @param type The string corresponding to the type of the attribute.
     * 
     * @throws Exception Thrown if there is no possible patient visit with the label specified.
     */
    public void setStudyEventAttr(String label, EventAttrTypeEnum type)
        throws Exception {
        setStudyEventAttr(label, type, null);
    }

    /**
     * Used to enable or disable the locked status of a patient visit attribute. If an attribute is
     * locked the patient visits will not allow information to be saved for this attribute.
     * 
     * @param label The label used for the attribute. Note: the label must already exist.
     * @param enable True to enable the lock, false otherwise.
     * 
     * @throws Exception if attribute with label does not exist.
     */
    public void setStudyEventAttrActivityStatus(String label,
        ActivityStatus activityStatus) throws Exception {
        getStudyEventAttrMap();
        StudyEventAttrWrapper studyEventAttr = getStudyEventAttr(label);
        studyEventAttr.setActivityStatus(activityStatus);
    }

    /**
     * Used to delete a patient visit attribute.
     * 
     * @param label The label used for the attribute.
     * @throws Exception if attribute with label does not exist.
     */
    public void deleteStudyEventAttr(String label) throws Exception {
        getStudyEventAttrMap();
        StudyEventAttrWrapper studyEventAttr = getStudyEventAttr(label);
        if (studyEventAttr.isUsedByCollectionEvents()) {
            throw new BiobankCheckException("StudyEventAttr with label \"" //$NON-NLS-1$
                + label + "\" is in use by patient visits"); //$NON-NLS-1$
        }
        studyEventAttrMap.remove(label);
        updateStudyEventAttrCollection();
    }

    public List<ClinicWrapper> getClinicCollection() {
        // unique clinics
        List<ContactWrapper> contacts = getContactCollection(false);
        HashSet<ClinicWrapper> clinicWrappers = new HashSet<ClinicWrapper>();
        if (contacts != null)
            for (ContactWrapper contact : contacts) {
                clinicWrappers.add(contact.getClinic());
            }
        return Arrays.asList(clinicWrappers.toArray(new ClinicWrapper[] {}));
    }

    @SuppressWarnings("nls")
    private static final String PATIENT_QRY = "select patients from "
        + Study.class.getName() + " as study inner join study."
        + StudyPeer.PATIENTS.getName()
        + " as patients where patients." + PatientPeer.PNUMBER.getName()
        + " = ? and study." + StudyPeer.ID.getName() + " = ?";

    public PatientWrapper getPatient(String patientNumber) throws Exception {
        HQLCriteria criteria = new HQLCriteria(PATIENT_QRY,
            Arrays.asList(new Object[] { patientNumber, getId() }));
        List<Patient> result = appService.query(criteria);
        if (result.size() > 1) {
            throw new BiobankQueryResultSizeException();
        } else if (result.size() == 1) {
            return new PatientWrapper(appService, result.get(0));
        }
        return null;
    }

    public boolean hasPatients() throws ApplicationException, BiobankException {
        return getPatientCount(true) > 0;
    }

    public long getPatientCount(boolean fast) throws ApplicationException,
        BiobankException {
        return getPropertyCount(StudyPeer.PATIENTS, fast);
    }

    @Override
    public int compareTo(ModelWrapper<Study> wrapper) {
        if (wrapper instanceof StudyWrapper) {
            String nameShort1 = getNameShort();
            String nameShort2 = wrapper.wrappedObject.getNameShort();

            int compare = 0;
            if ((nameShort1 != null) && (nameShort2 != null)) {
                compare = nameShort1.compareTo(nameShort2);
            }
            if (compare == 0) {
                String name1 = getName();
                String name2 = wrapper.wrappedObject.getName();

                return name1.compareTo(name2);
            }
            return compare;
        }
        return 0;
    }

    @SuppressWarnings("nls")
    public static final String IS_LINKED_TO_CLINIC_QRY =
        "select count(clinics) from "
            + Contact.class.getName()
            + " as contacts join contacts."
            + ContactPeer.CLINIC.getName()
            + " as clinics where contacts."
            + Property.concatNames(ContactPeer.STUDIES, StudyPeer.ID)
            + " = ? and clinics." + ClinicPeer.ID.getName() + " = ?";

    /**
     * return true if this study is linked to the given clinic (through contacts)
     */
    public boolean isLinkedToClinic(ClinicWrapper clinic)
        throws ApplicationException, BiobankException {
        HQLCriteria c = new HQLCriteria(IS_LINKED_TO_CLINIC_QRY,
            Arrays.asList(new Object[] { getId(), clinic.getId() }));
        return getCountResult(appService, c) != 0;
    }

    @Override
    public void resetInternalFields() {
        studyEventAttrMap = null;
    }

    @SuppressWarnings("nls")
    public static final String ALL_STUDIES_QRY = "from "
        + Study.class.getName();

    public static List<StudyWrapper> getAllStudies(
        WritableApplicationService appService) throws ApplicationException {
        HQLCriteria c = new HQLCriteria(ALL_STUDIES_QRY);
        return ModelWrapper.wrapModelCollection(appService,
            appService.query(c), StudyWrapper.class);
    }

    @SuppressWarnings("nls")
    public static final String COUNT_QRY = "select count (*) from "
        + Study.class.getName();

    public static long getCount(WritableApplicationService appService)
        throws BiobankException, ApplicationException {
        return getCountResult(appService, new HQLCriteria(COUNT_QRY));
    }

    @Override
    public String toString() {
        return getName();
    }

    @SuppressWarnings("nls")
    private static final String COLLECTION_EVENT_COUNT_QRY =
        "select count(distinct ce) from "
            + CollectionEvent.class.getName()
            + " as ce where ce."
            + Property.concatNames(CollectionEventPeer.PATIENT,
                PatientPeer.STUDY,
                StudyPeer.ID) + "=?";

    public long getCollectionEventCount() throws ApplicationException,
        BiobankException {
        HQLCriteria c = new HQLCriteria(COLLECTION_EVENT_COUNT_QRY,
            Arrays.asList(new Object[] { getId() }));
        return getCountResult(appService, c);
    }

    @Override
    public boolean canUpdate(UserWrapper user, CenterWrapper<?> center,
        StudyWrapper study) {
        return super.canUpdate(user, center, study);
    }

    @Deprecated
    @Override
    protected void addPersistTasks(TaskList tasks) {
        tasks.deleteRemoved(this, StudyPeer.STUDY_EVENT_ATTRS);
        tasks.deleteRemoved(this, StudyPeer.SOURCE_SPECIMENS);
        tasks.deleteRemoved(this, StudyPeer.ALIQUOTED_SPECIMENS);

        super.addPersistTasks(tasks);

        tasks.persistAdded(this, StudyPeer.STUDY_EVENT_ATTRS);
        tasks.persistAdded(this, StudyPeer.SOURCE_SPECIMENS);
        tasks.persistAdded(this, StudyPeer.ALIQUOTED_SPECIMENS);
    }

    @Deprecated
    @Override
    protected void addDeleteTasks(TaskList tasks) {
        tasks.delete(this, StudyPeer.STUDY_EVENT_ATTRS);
        tasks.delete(this, StudyPeer.SOURCE_SPECIMENS);
        tasks.delete(this, StudyPeer.ALIQUOTED_SPECIMENS);

        super.addDeleteTasks(tasks);
    }

    // public List<PatientWrapper> getPatientCollection(boolean sort) {
    // return HQLAccessor.getCachedCollection(this, PatientPeer.STUDY,
    // Patient.class, PatientWrapper.class, sort);
    // }

    @SuppressWarnings("nls")
    private static final String ACTIVE_ALIQUOTED_SPECIMENS_TYPE_QRY =
        "select aspec from "
            + AliquotedSpecimen.class.getName()
            + " as aspec where aspec."
            + Property.concatNames(AliquotedSpecimenPeer.STUDY, StudyPeer.ID)
            + " = ? and aspec.activityStatus = "
            + ActivityStatus.ACTIVE.getId();

    @Deprecated
    public List<AliquotedSpecimenWrapper> getAuthorizedActiveAliquotedTypes(
        List<SpecimenTypeWrapper> authorizedTypes) throws ApplicationException {
        List<AliquotedSpecimen> raw = appService.query(new HQLCriteria(
            ACTIVE_ALIQUOTED_SPECIMENS_TYPE_QRY, Arrays
                .asList(new Object[] { getId() })));
        if (raw == null) {
            return new ArrayList<AliquotedSpecimenWrapper>();
        }
        List<AliquotedSpecimenWrapper> studiesAliquotedTypes =
            new ArrayList<AliquotedSpecimenWrapper>();
        for (AliquotedSpecimen st : raw) {
            AliquotedSpecimenWrapper atype =
                new AliquotedSpecimenWrapper(appService, st);
            SpecimenTypeWrapper type = atype.getSpecimenType();
            if (authorizedTypes == null || authorizedTypes.contains(type)) {
                studiesAliquotedTypes.add(atype);
            }
        }
        return studiesAliquotedTypes;
    }
}
