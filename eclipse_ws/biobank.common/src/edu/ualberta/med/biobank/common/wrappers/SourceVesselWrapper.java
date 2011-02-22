package edu.ualberta.med.biobank.common.wrappers;

import java.util.ArrayList;
import java.util.List;

import edu.ualberta.med.biobank.common.exception.BiobankCheckException;
import edu.ualberta.med.biobank.common.exception.BiobankException;
import edu.ualberta.med.biobank.common.wrappers.base.SourceVesselBaseWrapper;
import edu.ualberta.med.biobank.model.SourceVessel;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;

public class SourceVesselWrapper extends SourceVesselBaseWrapper {

    public SourceVesselWrapper(WritableApplicationService appService,
        SourceVessel wrappedObject) {
        super(appService, wrappedObject);
    }

    public SourceVesselWrapper(WritableApplicationService appService) {
        super(appService);
    }

    @Override
    public int compareTo(ModelWrapper<SourceVessel> wrapper) {
        if (wrapper instanceof SourceVesselWrapper) {
            SourceVesselWrapper svWrapper = (SourceVesselWrapper) wrapper;
            String name1 = toString();
            String name2 = svWrapper.toString();

            return nullSafeComparator(name1, name2);
        }
        return 0;
    }

    @Override
    public String toString() {
        return getSourceVesselType().getName() + " " + getTimeDrawn();
    }

    @Override
    protected void deleteChecks() throws BiobankException, ApplicationException {
        checkProcessingEvent();
    }

    private void checkProcessingEvent() throws BiobankCheckException {
        ProcessingEventWrapper pevent = getProcessingEvent();
        if (pevent != null) {
            throw new BiobankCheckException(
                "Source vessel has a processing event. cannot be deleted.");
        }
    }

    public static List<SourceVesselWrapper> getAllSourceVessels(
        WritableApplicationService appService) throws ApplicationException {
        List<SourceVessel> list = appService.search(SourceVessel.class,
            new SourceVessel());
        List<SourceVesselWrapper> wrappers = new ArrayList<SourceVesselWrapper>();
        for (SourceVessel ss : list) {
            wrappers.add(new SourceVesselWrapper(appService, ss));
        }
        return wrappers;
    }
}
