package edu.ualberta.med.biobank.common.wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import edu.ualberta.med.biobank.common.BiobankCheckException;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.PatientVisit;
import edu.ualberta.med.biobank.model.Shipment;
import edu.ualberta.med.biobank.model.Study;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class PatientWrapper extends ModelWrapper<Patient> {

    public PatientWrapper(WritableApplicationService appService, Patient patient) {
        super(appService, patient);
    }

    public PatientWrapper(WritableApplicationService appService) {
        super(appService);
    }

    public String getPnumber() {
        return wrappedObject.getPnumber();
    }

    public void setPnumber(String number) {
        String oldNumber = getPnumber();
        wrappedObject.setPnumber(number);
        propertyChangeSupport.firePropertyChange("pnumber", oldNumber, number);
    }

    public StudyWrapper getStudy() {
        Study study = wrappedObject.getStudy();
        if (study == null) {
            return null;
        }
        return new StudyWrapper(appService, study);
    }

    public void setStudy(StudyWrapper study) {
        Study oldStudy = wrappedObject.getStudy();
        Study newStudy = study.wrappedObject;
        wrappedObject.setStudy(newStudy);
        propertyChangeSupport.firePropertyChange("study", oldStudy, newStudy);
    }

    public boolean checkPatientNumberUnique() throws ApplicationException {
        String isSamePatient = "";
        List<Object> params = new ArrayList<Object>();
        params.add(getStudy().getSite().getId());
        params.add(getPnumber());
        if (!isNew()) {
            isSamePatient = " and id <> ?";
            params.add(getId());
        }
        HQLCriteria c = new HQLCriteria("from " + Patient.class.getName()
            + " where study.site.id = ? and pnumber = ?" + isSamePatient,
            params);

        List<Object> results = appService.query(c);
        return results.size() == 0;
    }

    /**
     * When retrieve the values from the database, need to fire the
     * modifications for the different objects contained in the wrapped object
     * 
     * @throws Exception
     */
    @Override
    protected String[] getPropertyChangeNames() {
        return new String[] { "pnumber", "study", "patientVisitCollection",
            "shptSampleSourceCollection", "shipmentCollection" };
    }

    @Override
    protected void persistChecks() throws BiobankCheckException,
        ApplicationException {
        if (!checkPatientNumberUnique()) {
            throw new BiobankCheckException("A patient with number \""
                + getPnumber() + "\" already exists.");
        }
    }

    @SuppressWarnings("unchecked")
    public List<PatientVisitWrapper> getPatientVisitCollection() {
        List<PatientVisitWrapper> patientVisitCollection = (List<PatientVisitWrapper>) propertiesMap
            .get("patientVisitCollection");
        if (patientVisitCollection == null) {
            Collection<PatientVisit> children = wrappedObject
                .getPatientVisitCollection();
            if (children != null) {
                patientVisitCollection = new ArrayList<PatientVisitWrapper>();
                for (PatientVisit pv : children) {
                    patientVisitCollection.add(new PatientVisitWrapper(
                        appService, pv));
                }
                propertiesMap.put("patientVisitCollection",
                    patientVisitCollection);
            }
        }
        return patientVisitCollection;
    }

    public void addPatientVisits(
        Collection<PatientVisitWrapper> newPatientVisits) {
        Collection<PatientVisit> allPvObjects = new HashSet<PatientVisit>();
        List<PatientVisitWrapper> allPvWrappers = new ArrayList<PatientVisitWrapper>();
        // already added visits
        List<PatientVisitWrapper> currentList = getPatientVisitCollection();
        if (currentList != null) {
            for (PatientVisitWrapper visit : currentList) {
                allPvObjects.add(visit.getWrappedObject());
                allPvWrappers.add(visit);
            }
        }
        // new
        for (PatientVisitWrapper visit : newPatientVisits) {
            allPvObjects.add(visit.getWrappedObject());
            allPvWrappers.add(visit);
        }
        Collection<PatientVisit> oldCollection = wrappedObject
            .getPatientVisitCollection();
        wrappedObject.setPatientVisitCollection(allPvObjects);
        propertyChangeSupport.firePropertyChange("patientVisitCollection",
            oldCollection, allPvObjects);
        propertiesMap.put("patientVisitCollection", allPvWrappers);
    }

    /**
     * Search patient visits with the given date processed.
     */
    public List<PatientVisitWrapper> getVisits(Date dateProcessed)
        throws ApplicationException {
        HQLCriteria criteria = new HQLCriteria("select visits from "
            + Patient.class.getName()
            + " as p left join p.patientVisitCollection as visits"
            + " where p.id = ? and visits.dateProcessed = ?", Arrays
            .asList(new Object[] { getId(), dateProcessed }));
        List<PatientVisit> visits = appService.query(criteria);
        List<PatientVisitWrapper> result = new ArrayList<PatientVisitWrapper>();
        for (PatientVisit visit : visits) {
            result.add(new PatientVisitWrapper(appService, visit));
        }
        return result;
    }

    /**
     * Search a patient in the site with the given number
     */
    public static PatientWrapper getPatientInSite(
        WritableApplicationService appService, String patientNumber,
        SiteWrapper siteWrapper) throws ApplicationException {
        HQLCriteria criteria = new HQLCriteria("from "
            + Patient.class.getName()
            + " where study.site.id = ? and pnumber = ?", Arrays
            .asList(new Object[] { siteWrapper.getId(), patientNumber }));
        List<Patient> patients = appService.query(criteria);
        if (patients.size() == 1) {
            return new PatientWrapper(appService, patients.get(0));
        }
        return null;
    }

    /**
     * Get the shipment collection. To link patients and shipments, use
     * Shipment.setPatientCollection method
     */
    @SuppressWarnings("unchecked")
    public List<ShipmentWrapper> getShipmentCollection(boolean sort) {
        List<ShipmentWrapper> shipmentCollection = (List<ShipmentWrapper>) propertiesMap
            .get("shipmentCollection");
        if (shipmentCollection == null) {
            Collection<Shipment> children = wrappedObject
                .getShipmentCollection();
            if (children != null) {
                shipmentCollection = new ArrayList<ShipmentWrapper>();
                for (Shipment ship : children) {
                    shipmentCollection
                        .add(new ShipmentWrapper(appService, ship));
                }
                propertiesMap.put("shipmentCollection", shipmentCollection);
            }
        }
        if (sort) {
            Collections.sort(shipmentCollection);
        }
        return shipmentCollection;
    }

    public List<ShipmentWrapper> getShipmentCollection() {
        return getShipmentCollection(false);
    }

    @Override
    public Class<Patient> getWrappedClass() {
        return Patient.class;
    }

    @Override
    protected void deleteDependencies() throws Exception {
        List<PatientVisitWrapper> visits = getPatientVisitCollection();
        if (visits != null) {
            for (PatientVisitWrapper visit : visits) {
                visit.delete();
            }
        }
    }

    @Override
    protected void deleteChecks() throws BiobankCheckException,
        ApplicationException {
        if (hasSamples()) {
            throw new BiobankCheckException("Unable to delete patient "
                + getPnumber()
                + " since patient has samples stored in database.");
        }
    }

    public long getSampleCount() throws ApplicationException,
        BiobankCheckException {
        HQLCriteria c = new HQLCriteria("select count(samples) from "
            + Patient.class.getName() + " as p"
            + " join p.patientVisitCollection as visits"
            + " join visits.sampleCollection as samples where p = ?", Arrays
            .asList(new Object[] { wrappedObject }));
        List<Long> results = appService.query(c);
        if (results.size() != 1) {
            throw new BiobankCheckException("Invalid size for HQL query result");
        }
        return results.get(0);
    }

    public boolean hasSamples() throws ApplicationException,
        BiobankCheckException {
        return (getSampleCount() > 0);
    }

    @Override
    public int compareTo(ModelWrapper<Patient> wrapper) {
        if (wrapper instanceof PatientWrapper) {
            String number1 = wrappedObject.getPnumber();
            String number2 = wrapper.wrappedObject.getPnumber();
            return number1.compareTo(number2);
        }
        return 0;
    }

    @Override
    public String toString() {
        return getPnumber();
    }
}