package edu.ualberta.med.biobank.server.reports;

import java.text.MessageFormat;

import edu.ualberta.med.biobank.client.reports.BiobankReport;
import edu.ualberta.med.biobank.common.util.AbstractRowPostProcess;
import edu.ualberta.med.biobank.common.util.DateRangeRowPostProcess;
import edu.ualberta.med.biobank.model.PatientVisit;

public class NewPVsByStudyClinicImpl extends AbstractReport {

    private static final String QUERY = "Select Alias.patient.study.nameShort, "
        + "Alias.shipment.clinic.name, Year(Alias.dateProcessed), "
        + "{0}(Alias.dateProcessed), count(*) from "
        + PatientVisit.class.getName()
        + " as Alias where Alias.patient.study.site "
        + SITE_OPERATOR
        + SITE_ID
        + " GROUP BY Alias.patient.study.nameShort, Alias.shipment.clinic.name, "
        + "Year(Alias.dateProcessed), {0}(Alias.dateProcessed)";

    private DateRangeRowPostProcess dateRangePostProcess;

    public NewPVsByStudyClinicImpl(BiobankReport report) {
        super(QUERY, report);
        String groupBy = report.getStrings().get(0);
        queryString = MessageFormat.format(queryString, groupBy);
        dateRangePostProcess = new DateRangeRowPostProcess(
            groupBy.equals("Year"), 2);
    }

    @Override
    protected AbstractRowPostProcess getRowPostProcess() {
        return dateRangePostProcess;
    }

}