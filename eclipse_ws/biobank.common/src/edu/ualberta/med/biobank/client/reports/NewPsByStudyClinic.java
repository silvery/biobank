package edu.ualberta.med.biobank.client.reports;

import edu.ualberta.med.biobank.common.util.DateGroup;

public class NewPsByStudyClinic extends BiobankReport {

    protected static final String NAME = "New Patients per Study per Clinic by Date";

    public NewPsByStudyClinic() {
        super(
            "Displays the total number of patients added per study per clinic"
                + " grouped by date processed in a calendar week/month/quarter/year.",
            new String[] { "Study", "Clinic", "", "Total" });
        addOption("Date Range", DateGroup.class, DateGroup.Month);
    }

    @Override
    public String getName() {
        return NAME;
    }

}