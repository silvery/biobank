package edu.ualberta.med.biobank.forms;

import java.util.List;

import org.acegisecurity.AccessDeniedException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;

import edu.ualberta.med.biobank.BioBankPlugin;
import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.security.User;
import edu.ualberta.med.biobank.common.wrappers.DispatchAliquotWrapper;
import edu.ualberta.med.biobank.common.wrappers.DispatchWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.dialogs.dispatch.SendDispatchDialog;
import edu.ualberta.med.biobank.logs.BiobankLogger;
import edu.ualberta.med.biobank.treeview.dispatch.DispatchAdapter;
import edu.ualberta.med.biobank.views.DispatchAdministrationView;
import edu.ualberta.med.biobank.widgets.BiobankText;
import edu.ualberta.med.biobank.widgets.DispatchAliquotsTreeTable;
import edu.ualberta.med.biobank.widgets.infotables.DispatchAliquotListInfoTable;
import edu.ualberta.med.biobank.widgets.infotables.InfoTableSelection;
import edu.ualberta.med.biobank.widgets.listeners.BiobankEntryFormWidgetListener;
import edu.ualberta.med.biobank.widgets.listeners.MultiSelectEvent;

public class DispatchViewForm extends BiobankViewForm {

    private static BiobankLogger logger = BiobankLogger
        .getLogger(DispatchViewForm.class.getName());

    public static final String ID = "edu.ualberta.med.biobank.forms.DispatchViewForm";

    private DispatchAdapter shipmentAdapter;

    private DispatchWrapper shipment;

    private BiobankText studyLabel;

    private BiobankText senderLabel;

    private BiobankText receiverLabel;

    private BiobankText departedLabel;

    private BiobankText shippingMethodLabel;

    private BiobankText waybillLabel;

    private BiobankText dateReceivedLabel;

    private BiobankText commentLabel;

    private DispatchAliquotsTreeTable aliquotsTree;

    private DispatchAliquotListInfoTable aliquotsNonProcessedTable;

    private boolean canSeeEverything;

    @Override
    protected void init() throws Exception {
        Assert.isTrue((adapter instanceof DispatchAdapter),
            "Invalid editor input: object of type "
                + adapter.getClass().getName());

        shipmentAdapter = (DispatchAdapter) adapter;
        shipment = (DispatchWrapper) adapter.getModelObject();
        retrieveShipment();
        setPartName("Dispatch");
    }

    private void retrieveShipment() {
        try {
            shipment.reload();
        } catch (Exception ex) {
            logger.error(
                "Error while retrieving shipment " + shipment.getWaybill(), ex);
        }
    }

    @Override
    public void reload() throws Exception {
        retrieveShipment();
        setPartName("Dispatch sent on " + shipment.getDeparted());
        setShipmentValues();
        aliquotsTree.refresh();
    }

    @Override
    protected void createFormContent() throws Exception {
        String dateString = "";
        if (shipment.getDeparted() != null) {
            dateString = " on " + shipment.getFormattedDeparted();
        }
        canSeeEverything = true;
        if (shipment.getSender() == null) {
            canSeeEverything = false;
            BioBankPlugin
                .openAsyncError(
                    "Access Denied",
                    "It seems you don't have access to the sender site. Please see administrator to resolve this problem.");
        } else {
            form.setText("Shipment sent" + dateString + " from "
                + shipment.getSender().getNameShort());
        }
        if (shipment.getReceiver() == null) {
            canSeeEverything = false;
            BioBankPlugin
                .openAsyncError(
                    "Access Denied",
                    "It seems you don't have access to the receiver site. Please see administrator to resolve this problem.");
        }
        page.setLayout(new GridLayout(1, false));
        page.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createMainSection();

        if (canSeeEverything) {
            createTreeTableSection();
        }

        setShipmentValues();

        if (canSeeEverything) {
            User user = SessionManager.getUser();
            SiteWrapper currentSite = SessionManager.getCurrentSite();
            if (shipment.canBeSentBy(user, currentSite))
                createSendButton();
            else if (shipment.canBeReceivedBy(user, currentSite))
                createReceiveButtons();
            else if (shipment.canBeClosedBy(user, currentSite))
                createCloseButton();
        }
    }

    @Override
    protected void addEditAction() {
        if (canSeeEverything) {
            super.addEditAction();
        }
    }

    private void createTreeTableSection() {
        if (shipment.isInCreationState()) {
            Composite parent = createSectionWithClient("Aliquot added");
            aliquotsNonProcessedTable = new DispatchAliquotListInfoTable(
                parent, shipment, false) {
                @Override
                public List<DispatchAliquotWrapper> getInternalDispatchAliquots() {
                    return shipment.getNonProcessedDispatchAliquotCollection();
                }

            };
            aliquotsNonProcessedTable.adaptToToolkit(toolkit, true);
            aliquotsNonProcessedTable
                .addClickListener(new IDoubleClickListener() {
                    @Override
                    public void doubleClick(DoubleClickEvent event) {
                        Object selection = event.getSelection();
                        if (selection instanceof InfoTableSelection) {
                            InfoTableSelection tableSelection = (InfoTableSelection) selection;
                            DispatchAliquotWrapper dsa = (DispatchAliquotWrapper) tableSelection
                                .getObject();
                            if (dsa != null) {
                                SessionManager.openViewForm(dsa.getAliquot());
                            }
                        }
                    }
                });
            aliquotsNonProcessedTable
                .addSelectionChangedListener(new BiobankEntryFormWidgetListener() {
                    @Override
                    public void selectionChanged(MultiSelectEvent event) {
                        aliquotsNonProcessedTable.reloadCollection();
                    }
                });
        } else {
            aliquotsTree = new DispatchAliquotsTreeTable(page, shipment, false,
                false);
        }
    }

    private void createReceiveButtons() {
        Composite composite = toolkit.createComposite(page);
        composite.setLayout(new GridLayout(3, false));
        Button sendButton = toolkit
            .createButton(composite, "Receive", SWT.PUSH);
        sendButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                shipmentAdapter.doReceive();
            }
        });

        Button sendProcessButton = toolkit.createButton(composite,
            "Receive and Process", SWT.PUSH);
        sendProcessButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                shipmentAdapter.doReceiveAndProcess();
            }
        });

        Button lostProcessButton = toolkit.createButton(composite, "Lost",
            SWT.PUSH);
        lostProcessButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                shipmentAdapter.doSetAsLost();
            }
        });
    }

    private void createCloseButton() {
        Composite composite = toolkit.createComposite(page);
        composite.setLayout(new GridLayout(2, false));
        Button sendButton = toolkit.createButton(composite, "Close", SWT.PUSH);
        sendButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                shipmentAdapter.doClose();
            }
        });
    }

    private void createSendButton() {
        final Button sendButton = toolkit.createButton(page, "Send", SWT.PUSH);
        sendButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (new SendDispatchDialog(Display.getDefault()
                    .getActiveShell(), shipment).open() == Dialog.OK) {
                    IRunnableContext context = new ProgressMonitorDialog(
                        Display.getDefault().getActiveShell());
                    try {
                        context.run(true, true, new IRunnableWithProgress() {
                            @Override
                            public void run(final IProgressMonitor monitor) {
                                monitor.beginTask("Saving...",
                                    IProgressMonitor.UNKNOWN);
                                shipment.setInTransitState();
                                try {
                                    shipment.persist();
                                } catch (final RemoteConnectFailureException exp) {
                                    BioBankPlugin
                                        .openRemoteConnectErrorMessage(exp);
                                    return;
                                } catch (final RemoteAccessException exp) {
                                    BioBankPlugin
                                        .openRemoteAccessErrorMessage(exp);
                                    return;
                                } catch (final AccessDeniedException ade) {
                                    BioBankPlugin
                                        .openAccessDeniedErrorMessage(ade);
                                    return;
                                } catch (Exception ex) {
                                    BioBankPlugin.openAsyncError("Save error",
                                        ex);
                                    return;
                                }
                                monitor.done();
                            }
                        });
                    } catch (Exception e1) {
                        BioBankPlugin.openAsyncError("Save error", e1);
                    }
                    DispatchAdministrationView.getCurrent().reload();
                    shipmentAdapter.openViewForm();
                }
            }
        });
    }

    private void createMainSection() {
        Composite client = toolkit.createComposite(page);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        client.setLayout(layout);
        client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkit.paintBordersFor(client);

        String stateMessage = null;
        if (shipment.isInLostState())
            stateMessage = " Shipment Lost ";
        else if (shipment.isInClosedState())
            stateMessage = " Shipment Closed ";
        if (stateMessage != null) {
            Label label = widgetCreator.createLabel(client, stateMessage,
                SWT.CENTER, false);
            label.setBackground(Display.getDefault().getSystemColor(
                SWT.COLOR_RED));
            label.setForeground(Display.getDefault().getSystemColor(
                SWT.COLOR_WHITE));
            GridData gd = new GridData();
            gd.horizontalAlignment = SWT.CENTER;
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalSpan = 2;
            label.setLayoutData(gd);
        }

        studyLabel = createReadOnlyLabelledField(client, SWT.NONE, "Study");
        senderLabel = createReadOnlyLabelledField(client, SWT.NONE, "Sender");
        receiverLabel = createReadOnlyLabelledField(client, SWT.NONE,
            "Receiver");
        if (!shipment.isInCreationState()) {
            departedLabel = createReadOnlyLabelledField(client, SWT.NONE,
                "Departed");
            shippingMethodLabel = createReadOnlyLabelledField(client, SWT.NONE,
                "Shipping Method");
            waybillLabel = createReadOnlyLabelledField(client, SWT.NONE,
                "Waybill");
        }
        if (shipment.hasBeenReceived()) {
            dateReceivedLabel = createReadOnlyLabelledField(client, SWT.NONE,
                "Date received");
        }
        commentLabel = createReadOnlyLabelledField(client, SWT.MULTI,
            "Comments");
    }

    private void setShipmentValues() {
        setTextValue(studyLabel, shipment.getStudy().getName());
        setTextValue(senderLabel,
            shipment.getSender() == null ? " ACCESS DENIED" : shipment
                .getSender().getName());
        setTextValue(receiverLabel,
            shipment.getReceiver() == null ? "ACCESS DENIED" : shipment
                .getReceiver().getName());
        if (departedLabel != null)
            setTextValue(departedLabel, shipment.getFormattedDeparted());
        if (shippingMethodLabel != null)
            setTextValue(shippingMethodLabel,
                shipment.getShippingMethod() == null ? "" : shipment
                    .getShippingMethod().getName());
        if (waybillLabel != null)
            setTextValue(waybillLabel, shipment.getWaybill());
        if (dateReceivedLabel != null)
            setTextValue(dateReceivedLabel, shipment.getFormattedDateReceived());
        setTextValue(commentLabel, shipment.getComment());
    }

}
