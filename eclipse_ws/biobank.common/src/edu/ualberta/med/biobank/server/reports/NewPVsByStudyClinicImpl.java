package edu.ualberta.med.biobank.server.reports;

import edu.ualberta.med.biobank.common.reports.BiobankReport;
import edu.ualberta.med.biobank.common.util.AbstractRowPostProcess;
import edu.ualberta.med.biobank.common.util.DateRangeRowPostProcess;
import edu.ualberta.med.biobank.model.PatientVisit;

public class NewPVsByStudyClinicImpl extends AbstractReport {

    private static final String QUERY = "Select Alias.patient.study.nameShort, "
        + "Alias.shipment.clinic.name, Year(Alias.dateProcessed), "
        + GROUPBY_DATE
        + "(Alias.dateProcessed), count(*) from "
        + PatientVisit.class.getName()
        + " as Alias where Alias.dateProcessed between ? and ? and Alias.patient.study.site "
        + SITE_OPERATOR
        + SITE_ID
        + " GROUP BY Alias.patient.study.nameShort, Alias.shipment.clinic.name, "
        + "Year(Alias.dateProcessed), "
        + GROUPBY_DATE
        + "(Alias.dateProcessed)";

    private DateRangeRowPostProcess dateRangePostProcess;

    public NewPVsByStudyClinicImpl(BiobankReport report) {
        super(QUERY, report);
        dateRangePostProcess = new DateRangeRowPostProcess(report.getGroupBy()
            .equals("Year"), 2);
    }

    @Override
    protected AbstractRowPostProcess getRowPostProcess() {
        return dateRangePostProcess;
    }

}