package edu.ualberta.med.biobank.common.wrappers;

import java.util.ArrayList;
import java.util.List;

import edu.ualberta.med.biobank.common.BiobankCheckException;
import edu.ualberta.med.biobank.model.ContainerType;
import edu.ualberta.med.biobank.model.SampleType;
import edu.ualberta.med.biobank.model.Site;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;

public class SampleTypeWrapper extends ModelWrapper<SampleType> {

    public SampleTypeWrapper(WritableApplicationService appService,
        SampleType wrappedObject) {
        super(appService, wrappedObject);
    }

    @Override
    protected void firePropertyChanges(SampleType oldWrappedObject,
        SampleType newWrappedObject) {

    }

    @Override
    protected Class<SampleType> getWrappedClass() {
        return SampleType.class;
    }

    @Override
    protected void persistChecks() throws BiobankCheckException, Exception {
    }

    /**
     * get all sample types in a site for containers which type name contains
     * "typeNameContains"
     */
    public static List<SampleType> getSampleTypeForContainerTypes(
        WritableApplicationService appService, Site site,
        String typeNameContains) throws ApplicationException {
        List<ContainerType> types = ContainerTypeWrapper
            .getContainerTypesInSite(appService, site, typeNameContains, false);
        List<SampleType> sampleTypes = new ArrayList<SampleType>();
        for (ContainerType type : types) {
            sampleTypes.addAll(new ContainerTypeWrapper(appService, type)
                .getSampleTypes(true));
        }
        return sampleTypes;
    }

    @Override
    public boolean checkIntegrity() {
        return true;
    }

    @Override
    protected void deleteChecks() throws BiobankCheckException, Exception {
        // TODO Auto-generated method stub
    }
}
