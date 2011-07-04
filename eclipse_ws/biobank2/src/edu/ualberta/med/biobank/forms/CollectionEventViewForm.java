package edu.ualberta.med.biobank.forms;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import edu.ualberta.med.biobank.Messages;
import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.wrappers.CollectionEventWrapper;
import edu.ualberta.med.biobank.common.wrappers.EventAttrTypeEnum;
import edu.ualberta.med.biobank.common.wrappers.StudyWrapper;
import edu.ualberta.med.biobank.gui.common.BgcLogger;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;
import edu.ualberta.med.biobank.model.PvAttrCustom;
import edu.ualberta.med.biobank.treeview.patient.CollectionEventAdapter;
import edu.ualberta.med.biobank.widgets.infotables.SpecimenInfoTable;
import edu.ualberta.med.biobank.widgets.infotables.SpecimenInfoTable.ColumnsShown;

public class CollectionEventViewForm extends BiobankViewForm {

    public static final String ID = "edu.ualberta.med.biobank.forms.CollectionEventViewForm";

    private static BgcLogger logger = BgcLogger
        .getLogger(CollectionEventViewForm.class.getName());

    private CollectionEventAdapter ceventAdapter;

    private CollectionEventWrapper cevent;

    private BgcBaseText studyLabel;

    private List<FormPvCustomInfo> pvCustomInfoList;

    private BgcBaseText patientLabel;

    private BgcBaseText visitNumberLabel;

    private BgcBaseText commentLabel;

    private SpecimenInfoTable sourceSpecimenTable;

    private BgcBaseText activityStatusLabel;

    private SpecimenInfoTable aliquotedSpecimenTable;

    private static class FormPvCustomInfo extends PvAttrCustom {
        BgcBaseText widget;
    }

    @Override
    public void init() throws Exception {
        Assert.isTrue((adapter instanceof CollectionEventAdapter),
            "Invalid editor input: object of type "
                + adapter.getClass().getName());

        ceventAdapter = (CollectionEventAdapter) adapter;
        cevent = ceventAdapter.getWrapper();
        retrievePatientVisit();
        SessionManager.logLookup(cevent);

        setPartName(Messages.getString("CollectionEventViewForm.title", cevent
            .getPatient().getPnumber(), cevent.getVisitNumber()));
    }

    @Override
    protected void createFormContent() throws Exception {
        form.setText(Messages.getString("CollectionEventViewForm.main.title",
            +cevent.getVisitNumber()));
        page.setLayout(new GridLayout(1, false));
        page.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createMainSection();
        createSourceSpecimensSection();
        createAliquotedSpecimensSection();
    }

    private void createMainSection() throws Exception {
        Composite client = toolkit.createComposite(page);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        client.setLayout(layout);
        client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkit.paintBordersFor(client);

        studyLabel = createReadOnlyLabelledField(client, SWT.NONE, "Study");
        patientLabel = createReadOnlyLabelledField(client, SWT.NONE, "Patient");
        visitNumberLabel = createReadOnlyLabelledField(client, SWT.NONE,
            "Visit#");
        activityStatusLabel = createReadOnlyLabelledField(client, SWT.NONE,
            Messages.getString("label.activity"));

        createPvDataSection(client);

        commentLabel = createReadOnlyLabelledField(client, SWT.MULTI,
            Messages.getString("label.comments"));

        setCollectionEventValues();
    }

    private void createPvDataSection(Composite client) throws Exception {
        StudyWrapper study = cevent.getPatient().getStudy();
        String[] labels = study.getStudyEventAttrLabels();
        if (labels == null)
            return;

        pvCustomInfoList = new ArrayList<FormPvCustomInfo>();

        for (String label : labels) {
            FormPvCustomInfo combinedPvInfo = new FormPvCustomInfo();
            combinedPvInfo.setLabel(label);
            combinedPvInfo.setType(study.getStudyEventAttrType(label));

            int style = SWT.NONE;
            if (combinedPvInfo.getType() == EventAttrTypeEnum.SELECT_MULTIPLE) {
                style |= SWT.WRAP;
            }

            String value = cevent.getEventAttrValue(label);
            if (combinedPvInfo.getType() == EventAttrTypeEnum.SELECT_MULTIPLE
                && (value != null)) {
                combinedPvInfo.setValue(value.replace(';', '\n'));
            } else {
                combinedPvInfo.setValue(value);
            }

            combinedPvInfo.widget = createReadOnlyLabelledField(client, style,
                label, combinedPvInfo.getValue());
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            combinedPvInfo.widget.setLayoutData(gd);

            pvCustomInfoList.add(combinedPvInfo);
        }
    }

    private void setCollectionEventValues() {
        setTextValue(studyLabel, cevent.getPatient().getStudy().getName());
        setTextValue(patientLabel, cevent.getPatient().getPnumber());
        setTextValue(visitNumberLabel, cevent.getVisitNumber());
        setTextValue(activityStatusLabel, cevent.getActivityStatus().getName());
        setTextValue(commentLabel, cevent.getComment());
        // assign PvInfo
        for (FormPvCustomInfo combinedPvInfo : pvCustomInfoList) {
            setTextValue(combinedPvInfo.widget, combinedPvInfo.getValue());
        }
    }

    private void createSourceSpecimensSection() {
        Composite client = createSectionWithClient(Messages
            .getString("CollectionEventViewForm.sourcespecimens.title"));
        sourceSpecimenTable = new SpecimenInfoTable(client,
            cevent.getOriginalSpecimenCollection(true),
            ColumnsShown.SOURCE_SPECIMENS, 10);
        sourceSpecimenTable.adaptToToolkit(toolkit, true);
    }

    private void createAliquotedSpecimensSection() {
        // FIXME should we show that to clinics ?
        Composite client = createSectionWithClient(Messages
            .getString("CollectionEventViewForm.aliquotedspecimens.title"));
        aliquotedSpecimenTable = new SpecimenInfoTable(client,
            cevent.getAliquotedSpecimenCollection(true), ColumnsShown.ALIQUOTS,
            10);
        aliquotedSpecimenTable.adaptToToolkit(toolkit, true);
    }

    @Override
    public void reload() {
        retrievePatientVisit();
        setPartName(Messages.getString("CollectionEventViewForm.title", cevent
            .getPatient().getPnumber(), cevent.getVisitNumber()));
        form.setText(Messages.getString("CollectionEventViewForm.main.title",
            +cevent.getVisitNumber()));
        setCollectionEventValues();
        sourceSpecimenTable.setCollection(cevent
            .getOriginalSpecimenCollection(true));
        aliquotedSpecimenTable.setCollection(cevent
            .getAliquotedSpecimenCollection(true));
    }

    private void retrievePatientVisit() {
        try {
            cevent.reload();
        } catch (Exception ex) {
            logger.error(
                "Error while retrieving patient visit "
                    + cevent.getVisitNumber() + "(patient "
                    + cevent.getPatient() + ")", ex);
        }
    }

}
