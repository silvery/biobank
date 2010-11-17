package edu.ualberta.med.biobank.forms;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import edu.ualberta.med.biobank.BioBankPlugin;
import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.wrappers.ActivityStatusWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.treeview.admin.ContainerAdapter;
import edu.ualberta.med.biobank.treeview.admin.SiteAdapter;
import edu.ualberta.med.biobank.validators.DoubleNumberValidator;
import edu.ualberta.med.biobank.validators.NonEmptyStringValidator;
import edu.ualberta.med.biobank.widgets.BasicSiteCombo;
import edu.ualberta.med.biobank.widgets.BiobankText;
import edu.ualberta.med.biobank.widgets.utils.ComboSelectionUpdate;
import gov.nih.nci.system.applicationservice.ApplicationException;

public class ContainerEntryForm extends BiobankEntryForm {
    public static final String ID = "edu.ualberta.med.biobank.forms.ContainerEntryForm";

    public static final String MSG_STORAGE_CONTAINER_NEW_OK = "Creating a new storage container.";

    public static final String MSG_STORAGE_CONTAINER_OK = "Editing an existing storage container.";

    public static final String MSG_CONTAINER_NAME_EMPTY = "Container must have a name";

    public static final String MSG_CONTAINER_TYPE_EMPTY = "Container must have a container type";

    public static final String MSG_INVALID_POSITION = "Position is empty or not a valid number";

    private ContainerAdapter containerAdapter;

    private ContainerWrapper container;

    private BiobankText tempWidget;

    private ContainerTypeWrapper currentContainerType;

    private ComboViewer containerTypeComboViewer;

    private String oldContainerLabel;

    private ComboViewer activityStatusComboViewer;

    private boolean doSave;

    private boolean newName;

    private BasicSiteCombo siteCombo;

    protected List<ContainerTypeWrapper> containerTypes;

    @Override
    public void init() throws Exception {
        Assert.isTrue((adapter instanceof ContainerAdapter),
            "Invalid editor input: object of type "
                + adapter.getClass().getName());
        containerAdapter = (ContainerAdapter) adapter;
        container = containerAdapter.getContainer();

        String tabName;
        if (container.isNew()) {
            tabName = "Container";
            container.setActivityStatus(ActivityStatusWrapper
                .getActiveActivityStatus(appService));
            if (container.hasParent()) {
                container.setLabel(container.getParent().getLabel()
                    + container.getPositionString());
                container
                    .setTemperature(container.getParent().getTemperature());
            }
        } else {
            tabName = "Container " + container.getLabel();
            oldContainerLabel = container.getLabel();
        }
        setPartName(tabName);
    }

    @Override
    protected void createFormContent() throws Exception {
        form.setText("Container");

        currentContainerType = container.getContainerType();
        page.setLayout(new GridLayout(1, false));
        createContainerSection();
        createButtonsSection();
    }

    @SuppressWarnings("unchecked")
    private void createContainerSection() throws Exception {
        Composite client = toolkit.createComposite(page);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        client.setLayout(layout);
        client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkit.paintBordersFor(client);

        widgetCreator.createLabel(client, "Repository Site");
        siteCombo = new BasicSiteCombo(client, appService);
        if (!container.isNew()) {
            List<SiteWrapper> input = new ArrayList<SiteWrapper>();
            input.add(container.getSite());
            siteCombo.setInput(input);
        }
        siteCombo.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                if (!container.hasParent()) {
                    try {
                        containerTypes = ContainerTypeWrapper
                            .getTopContainerTypesInSite(appService,
                                siteCombo.getSite());
                    } catch (ApplicationException e) {
                    }
                } else {
                    containerTypes = container.getParent().getContainerType()
                        .getChildContainerTypeCollection();
                }
                currentContainerType = null;
                setDirty(true);
                containerTypeComboViewer.setInput(containerTypes);
                if (container.isNew())
                    adapter.setParent(((SiteAdapter) SessionManager
                        .getCurrentAdapterViewWithTree().searchNode(
                            siteCombo.getSite())).getContainersGroupNode());
                container.setSite(siteCombo.getSite());
            }
        });

        if ((container.isNew() && container.getParent() == null)
            || (container.getContainerType() != null && Boolean.TRUE
                .equals(container.getContainerType().getTopLevel()))) {
            // only allow edit to label on top level containers
            setFirstControl(createBoundWidgetWithLabel(client,
                BiobankText.class, SWT.NONE, "Label", null, container, "label",
                new NonEmptyStringValidator(MSG_CONTAINER_NAME_EMPTY)));
        } else {
            BiobankText l = createReadOnlyLabelledField(client, SWT.NONE,
                "Label");
            setTextValue(l, container.getLabel());
        }

        Control c = createBoundWidgetWithLabel(client, BiobankText.class,
            SWT.NONE, "Product Barcode", null, container, "productBarcode",
            null);
        if (getFirstControl() == null)
            setFirstControl(c);

        activityStatusComboViewer = createComboViewer(client,
            "Activity Status",
            ActivityStatusWrapper.getAllActivityStatuses(appService),
            container.getActivityStatus(),
            "Container must have an activity status",
            new ComboSelectionUpdate() {
                @Override
                public void doSelection(Object selectedObject) {
                    container
                        .setActivityStatus((ActivityStatusWrapper) selectedObject);
                }
            });

        createBoundWidgetWithLabel(client, BiobankText.class, SWT.MULTI,
            "Comments", null, container, "comment", null);

        createContainerTypesSection(client);
        if (!container.isNew())
            siteCombo
                .setSelection(new StructuredSelection(container.getSite()));
        else
            siteCombo.setSelection(new StructuredSelection(
                ((List<SiteWrapper>) siteCombo.getInput()).get(0)));

    }

    private void createContainerTypesSection(Composite client) {

        containerTypeComboViewer = createComboViewer(client, "Container Type",
            containerTypes, currentContainerType, MSG_CONTAINER_TYPE_EMPTY,
            new ComboSelectionUpdate() {
                @Override
                public void doSelection(Object selectedObject) {
                    ContainerTypeWrapper ct = (ContainerTypeWrapper) selectedObject;
                    container.setContainerType(ct);
                    if (tempWidget != null) {
                        tempWidget.setText("");
                        if (ct != null && Boolean.TRUE.equals(ct.getTopLevel())) {
                            Double temp = ct.getDefaultTemperature();
                            if (temp == null) {
                                tempWidget.setText("");
                            } else {
                                tempWidget.setText(temp.toString());
                            }
                        }
                    }
                }
            });

        tempWidget = (BiobankText) createBoundWidgetWithLabel(client,
            BiobankText.class, SWT.NONE, "Temperature (Celcius)", null,
            container, "temperature", new DoubleNumberValidator(
                "Default temperature is not a valid number"));
        if (container.hasParent())
            tempWidget.setEnabled(false);

        if (currentContainerType != null) {
            containerTypeComboViewer.setSelection(new StructuredSelection(
                currentContainerType));
        }
    }

    private void createButtonsSection() {
        Composite client = toolkit.createComposite(page);
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 10;
        layout.numColumns = 2;
        client.setLayout(layout);
        toolkit.paintBordersFor(client);
    }

    @Override
    protected String getOkMessage() {
        if (container.isNew()) {
            return MSG_STORAGE_CONTAINER_NEW_OK;
        }
        return MSG_STORAGE_CONTAINER_OK;
    }

    @Override
    protected void doBeforeSave() throws Exception {
        doSave = true;
        newName = false;
        if (container.hasChildren() && oldContainerLabel != null
            && !oldContainerLabel.equals(container.getLabel())) {
            doSave = BioBankPlugin
                .openConfirm(
                    "Renaming container",
                    "This container has been renamed. Its children will also be renamed. Are you sure you want to continue ?");
            newName = true;
        }
    }

    @Override
    protected void saveForm() throws Exception {
        if (doSave) {
            container.persist();
            if (newName) {
                container.reload();
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        containerAdapter.rebuild();
                        containerAdapter.performExpand();
                    }
                });
            } else {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        containerAdapter.getParent().addChild(containerAdapter);
                        containerAdapter.getParent().performExpand();
                    }
                });
            }
        } else {
            setDirty(true);
        }
    }

    @Override
    public String getNextOpenedFormID() {
        return ContainerViewForm.ID;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void reset() throws Exception {
        super.reset();
        if (!container.isNew())
            siteCombo
                .setSelection(new StructuredSelection(container.getSite()));
        else
            siteCombo.setSelection(new StructuredSelection(
                ((List<SiteWrapper>) siteCombo.getInput()).get(0)));
        currentContainerType = container.getContainerType();
        if (currentContainerType != null) {
            containerTypeComboViewer.setSelection(new StructuredSelection(
                currentContainerType));
        } else if (containerTypeComboViewer.getCombo().getItemCount() > 1) {
            containerTypeComboViewer.getCombo().deselectAll();
        }
        ActivityStatusWrapper activity = container.getActivityStatus();
        if (activity != null) {
            activityStatusComboViewer.setSelection(new StructuredSelection(
                activity));
        } else if (activityStatusComboViewer.getCombo().getItemCount() > 1) {
            activityStatusComboViewer.getCombo().deselectAll();
        }
    }
}
