package edu.ualberta.med.biobank.forms;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.Section;

import edu.ualberta.med.biobank.BioBankPlugin;
import edu.ualberta.med.biobank.common.wrappers.ClinicWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.common.wrappers.StudyWrapper;
import edu.ualberta.med.biobank.forms.input.FormInput;
import edu.ualberta.med.biobank.treeview.AdapterBase;
import edu.ualberta.med.biobank.treeview.ClinicAdapter;
import edu.ualberta.med.biobank.treeview.SiteAdapter;
import edu.ualberta.med.biobank.treeview.StudyAdapter;
import edu.ualberta.med.biobank.widgets.infotables.ClinicInfoTable;
import edu.ualberta.med.biobank.widgets.infotables.ContainerInfoTable;
import edu.ualberta.med.biobank.widgets.infotables.ContainerTypeInfoTable;
import edu.ualberta.med.biobank.widgets.infotables.StudyInfoTable;

public class SiteViewForm extends AddressViewFormCommon {
    public static final String ID = "edu.ualberta.med.biobank.forms.SiteViewForm";

    private static final Logger LOGGER = Logger.getLogger(SiteViewForm.class
        .getName());

    private SiteAdapter siteAdapter;

    private SiteWrapper siteWrapper;

    private StudyInfoTable studiesTable;
    private ClinicInfoTable clinicsTable;
    private ContainerTypeInfoTable containerTypesTable;
    private ContainerInfoTable sContainersTable;

    private Text activityStatusLabel;

    private Text commentLabel;

    private SelectionListener addStudySelectionListener = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
            AdapterBase studiesNode = siteAdapter.getStudiesGroupNode();
            StudyAdapter studyAdapter = new StudyAdapter(studiesNode,
                new StudyWrapper(siteAdapter.getAppService()));
            AdapterBase
                .openForm(new FormInput(studyAdapter), StudyEntryForm.ID);
        }
    };

    @Override
    public void init() {
        Assert.isTrue((adapter instanceof SiteAdapter),
            "Invalid editor input: object of type "
                + adapter.getClass().getName());

        siteAdapter = (SiteAdapter) adapter;
        siteWrapper = siteAdapter.getWrapper();
        retrieveSite();
        setPartName("Repository Site " + siteWrapper.getName());
    }

    @Override
    protected void createFormContent() throws Exception {
        form.setText("Repository Site: " + siteWrapper.getName());
        form.getBody().setLayout(new GridLayout(1, false));
        form.getBody().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        form.getBody().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        form.setImage(BioBankPlugin.getDefault().getImageRegistry().get(
            BioBankPlugin.IMG_SITE));

        createSiteSection();
        createAddressSection(siteWrapper);
        createStudySection();
        createClinicSection();
        createContainerTypesSection();
        createContainerSection();
        createButtons();
    }

    private void createSiteSection() {
        Composite client = toolkit.createComposite(form.getBody());
        client.setLayout(new GridLayout(2, false));
        client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkit.paintBordersFor(client);

        activityStatusLabel = createReadOnlyField(client, SWT.NONE,
            "Activity Status");
        commentLabel = createReadOnlyField(client, SWT.NONE, "Comments");
        setSiteSectionValues();
    }

    private void setSiteSectionValues() {
        setTextValue(activityStatusLabel, siteWrapper.getActivityStatus());
        setTextValue(commentLabel, siteWrapper.getComment());
    }

    private void createStudySection() {
        Section section = createSection("Studies");
        Composite client = sectionAddClient(section);

        ToolBar tbar = new ToolBar(section, SWT.FLAT | SWT.HORIZONTAL);
        ToolItem titem = new ToolItem(tbar, SWT.NULL);
        titem.setImage(BioBankPlugin.getDefault().getImageRegistry().get(
            BioBankPlugin.IMG_ADD));
        titem.setToolTipText("Add Study");
        titem.addSelectionListener(addStudySelectionListener);
        section.setTextClient(tbar);

        studiesTable = new StudyInfoTable(client, siteWrapper
            .getStudyCollection());
        studiesTable.adaptToToolkit(toolkit, true);
        studiesTable.addDoubleClickListener(collectionDoubleClickListener);
    }

    public void createClinicSection() {
        Collection<ClinicWrapper> clinics = siteWrapper
            .getClinicCollection(true);
        Section section = toolkit.createSection(form.getBody(), Section.TWISTIE
            | Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Clinics");
        section.setLayout(new GridLayout(1, false));
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        clinicsTable = new ClinicInfoTable(section, clinics);
        section.setClient(clinicsTable);
        clinicsTable.adaptToToolkit(toolkit, true);
        clinicsTable.getTableViewer().addDoubleClickListener(
            collectionDoubleClickListener);
    }

    private void createContainerTypesSection() {
        Composite client = createSectionWithClient("Container Types");

        containerTypesTable = new ContainerTypeInfoTable(client, siteWrapper
            .getContainerTypeCollection());
        containerTypesTable.adaptToToolkit(toolkit, true);

        containerTypesTable
            .addDoubleClickListener(collectionDoubleClickListener);
    }

    private void createContainerSection() {
        Section section = createSection("Top Level Containers");

        try {
            sContainersTable = new ContainerInfoTable(section, siteAdapter
                .getWrapper().getTopContainerCollection());
            section.setClient(sContainersTable);
            sContainersTable.adaptToToolkit(toolkit, true);
            toolkit.paintBordersFor(sContainersTable);

            sContainersTable
                .addDoubleClickListener(collectionDoubleClickListener);
        } catch (Exception e) {
            LOGGER.error("Problem while queriyng top level containers", e);
        }
    }

    private void createButtons() {
        Composite client = toolkit.createComposite(form.getBody());
        client.setLayout(new GridLayout(4, false));
        toolkit.paintBordersFor(client);

        final Button study = toolkit
            .createButton(client, "Add Study", SWT.PUSH);
        study.addSelectionListener(addStudySelectionListener);

        final Button clinic = toolkit.createButton(client, "Add Clinic",
            SWT.PUSH);
        clinic.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ClinicAdapter clinicAdapter = new ClinicAdapter(siteAdapter
                    .getClinicGroupNode(), new ClinicWrapper(appService));
                AdapterBase.openForm(new FormInput(clinicAdapter),
                    ClinicEntryForm.ID);
            }
        });

        final Button containerType = toolkit.createButton(client,
            "Add Container Type", SWT.PUSH);
        containerType.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            }
        });
    }

    @Override
    protected void reload() {
        retrieveSite();
        setPartName("Repository Site " + siteWrapper.getName());
        form.setText("Repository Site: " + siteWrapper.getName());
        setSiteSectionValues();
        setAdressValues(siteWrapper);
        studiesTable.setCollection(siteWrapper.getStudyCollection());
        clinicsTable.setCollection(siteWrapper.getClinicCollection(true));
        containerTypesTable.setCollection(siteWrapper
            .getContainerTypeCollection(true));
        sContainersTable.setCollection(siteWrapper.getContainerCollection());
    }

    private void retrieveSite() {
        try {
            siteWrapper.reload();
        } catch (Exception e) {
            BioBankPlugin.openAsyncError("Can't reload site", e);
        }
    }

    @Override
    protected String getEntryFormId() {
        return SiteEntryForm.ID;
    }
}
