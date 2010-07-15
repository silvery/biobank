package edu.ualberta.med.biobank.server.reports;

import edu.ualberta.med.biobank.client.reports.BiobankReport;
import edu.ualberta.med.biobank.model.AliquotPosition;
import edu.ualberta.med.biobank.model.ContainerPath;
import edu.ualberta.med.biobank.model.PatientVisit;

public class PatientWBCImpl extends AbstractReport {

    private static final String TYPE_NAME = "%Cabinet%";

    private static final String QUERY = "Select Alias.patient.study.nameShort, Alias.shipment.clinic.name, "
        + "Alias.patient.pnumber, Alias.dateProcessed, aliquot.sampleType.name, aliquot.inventoryId, aliquot.aliquotPosition.container.label  from "
        + PatientVisit.class.getName()
        + " as Alias left join Alias.aliquotCollection as aliquot where aliquot.aliquotPosition not in (from "
        + AliquotPosition.class.getName()
        + " a where a.container.label like '"
        + SENT_SAMPLES_FREEZER_NAME
        + "') and Alias.patient.study.site "
        + SITE_OPERATOR
        + SITE_ID
        + " and aliquot.sampleType.name LIKE '%DNA%' and aliquot.aliquotPosition.container.id in (select path1.container.id from "
        + ContainerPath.class.getName()
        + " as path1, "
        + ContainerPath.class.getName()
        + " as path2 where locate(path2.path, path1.path) > 0 and path2.container.containerType.name like '"
        + TYPE_NAME + "')";

    public PatientWBCImpl(BiobankReport report) {
        super(QUERY, report);
    }

}
