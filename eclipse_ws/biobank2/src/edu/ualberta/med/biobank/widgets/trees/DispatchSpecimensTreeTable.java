package edu.ualberta.med.biobank.widgets.trees;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PlatformUI;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.util.StringUtil;
import edu.ualberta.med.biobank.common.wrappers.CommentWrapper;
import edu.ualberta.med.biobank.common.wrappers.DispatchSpecimenWrapper;
import edu.ualberta.med.biobank.common.wrappers.DispatchWrapper;
import edu.ualberta.med.biobank.dialogs.dispatch.ModifyStateDispatchDialog;
import edu.ualberta.med.biobank.forms.utils.DispatchTableGroup;
import edu.ualberta.med.biobank.forms.utils.TableGroup;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseWidget;
import edu.ualberta.med.biobank.gui.common.widgets.BgcLabelProvider;
import edu.ualberta.med.biobank.gui.common.widgets.utils.BgcClipboard;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.model.type.DispatchSpecimenState;
import edu.ualberta.med.biobank.treeview.AbstractAdapterBase;
import edu.ualberta.med.biobank.treeview.Node;
import edu.ualberta.med.biobank.treeview.TreeItemAdapter;
import edu.ualberta.med.biobank.treeview.request.RequestContainerAdapter;
import edu.ualberta.med.biobank.treeview.util.AdapterFactory;

public class DispatchSpecimensTreeTable extends BgcBaseWidget {
    private static final I18n i18n = I18nFactory
        .getI18n(DispatchSpecimensTreeTable.class);

    private TreeViewer tv;
    private DispatchWrapper shipment;
    protected List<DispatchTableGroup> groups;
    private MenuItem editItem;
    private Menu menu;

    @SuppressWarnings("nls")
    public DispatchSpecimensTreeTable(Composite parent,
        final DispatchWrapper shipment,
        final boolean editSpecimensState) {
        super(parent, SWT.NONE);

        this.shipment = shipment;

        setLayout(new FillLayout());
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.heightHint = 400;
        setLayoutData(gd);

        tv = new TreeViewer(this, SWT.MULTI | SWT.BORDER);
        Tree tree = tv.getTree();
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);

        TreeColumn tc = new TreeColumn(tree, SWT.LEFT);
        tc.setText(Specimen.PropertyName.INVENTORY_ID.toString());
        tc.setWidth(200);

        tc = new TreeColumn(tree, SWT.LEFT);
        tc.setText(i18n.tr("Type"));
        tc.setWidth(100);

        tc = new TreeColumn(tree, SWT.LEFT);
        tc.setText(Patient.PropertyName.PNUMBER.toString());
        tc.setWidth(120);

        tc = new TreeColumn(tree, SWT.LEFT);
        tc.setText(ActivityStatus.NAME.singular().toString());
        tc.setWidth(120);

        tc = new TreeColumn(tree, SWT.LEFT);
        tc.setText(i18n.tr("Dispatch comment"));
        tc.setWidth(100);

        menu = new Menu(parent);
        tv.getTree().setMenu(menu);

        ITreeContentProvider contentProvider = new ITreeContentProvider() {
            @Override
            public void dispose() {
                //
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput,
                Object newInput) {
                if (newInput != null)
                    groups =
                        DispatchTableGroup
                            .getGroupsForShipment(
                            DispatchSpecimensTreeTable.this.shipment);
            }

            @Override
            public Object[] getElements(Object inputElement) {
                return groups.toArray();
            }

            @Override
            public Object[] getChildren(Object parentElement) {
                return ((Node) parentElement).getChildren().toArray();
            }

            @Override
            public Object getParent(Object element) {
                return ((Node) element).getParent();
            }

            @Override
            public boolean hasChildren(Object element) {
                return ((Node) element).getChildren().size() != 0;
            }
        };
        tv.setContentProvider(contentProvider);

        final BgcLabelProvider labelProvider = new BgcLabelProvider() {
            @Override
            public String getColumnText(Object element, int columnIndex) {
                if (element instanceof TableGroup) {
                    if (columnIndex == 0)
                        return ((TableGroup<?>) element).getTitle();
                    return StringUtil.EMPTY_STRING;
                } else if (element instanceof RequestContainerAdapter) {
                    if (columnIndex == 0)
                        return ((RequestContainerAdapter) element)
                            .getLabelInternal();
                    return StringUtil.EMPTY_STRING;
                } else if (element instanceof TreeItemAdapter) {
                    if (columnIndex == 4)
                        return CommentWrapper
                            .commentListToString(((DispatchSpecimenWrapper) ((TreeItemAdapter) element)
                                .getSpecimen()).getCommentCollection(false));
                    return ((TreeItemAdapter) element)
                        .getColumnText(columnIndex);
                }
                return StringUtil.EMPTY_STRING;
            }

            @Override
            public Image getColumnImage(Object element, int columnIndex) {
                return null;
            }
        };
        tv.setLabelProvider(labelProvider);
        tv.setInput("root");
        menu.addListener(SWT.Show, new Listener() {
            @Override
            public void handleEvent(Event event) {
                DispatchSpecimenWrapper dsa = getSelectedSpecimen();
                if (dsa != null) {
                    for (MenuItem menuItem : menu.getItems()) {
                        if (!menuItem.equals(editItem))
                            menuItem.dispose();
                    }

                    BgcClipboard
                        .addClipboardCopySupport(tv, menu, labelProvider, 5);

                    if (editSpecimensState) {
                        if (dsa.getState() == DispatchSpecimenState.NONE)
                            addSetMissingMenu(menu);
                        addModifyCommentMenu(menu);
                        if (dsa.getState() != DispatchSpecimenState.NONE)
                            addDeleteExtraMenu(menu);
                    }
                }
            }
        });
    }

    protected DispatchSpecimenWrapper getSelectedSpecimen() {
        IStructuredSelection selection = (IStructuredSelection) tv
            .getSelection();
        if (selection != null && selection.size() > 0
            && selection.getFirstElement() instanceof TreeItemAdapter) {
            return (DispatchSpecimenWrapper) ((TreeItemAdapter) selection
                .getFirstElement()).getSpecimen();
        }
        return null;
    }

    @SuppressWarnings("nls")
    protected void addModifyCommentMenu(Menu menu) {
        MenuItem item;
        item = new MenuItem(menu, SWT.PUSH);
        item.setText(
            // menu item label.
            i18n.tr("Modify comment"));
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                modifyCommentAndState((IStructuredSelection) tv.getSelection(),
                    null);
            }
        });
    }

    @SuppressWarnings("nls")
    private void addSetMissingMenu(final Menu menu) {
        MenuItem item;
        item = new MenuItem(menu, SWT.PUSH);
        item.setText(
            // menu item label.
            i18n.tr("Set as missing"));
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                modifyCommentAndState((IStructuredSelection) tv.getSelection(),
                    DispatchSpecimenState.MISSING);
            }
        });
    }

    @SuppressWarnings("nls")
    private void addDeleteExtraMenu(final Menu menu) {
        MenuItem item;
        item = new MenuItem(menu, SWT.PUSH);
        item.setText(
            // menu item label.
            i18n.tr("Delete"));
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                DispatchSpecimensTreeTable.this.shipment.removeDispatchSpecimens(Arrays
                    .asList((DispatchSpecimenWrapper) ((TreeItemAdapter) ((IStructuredSelection) tv
                        .getSelection())
                        .getFirstElement())
                        .getSpecimen()));
                notifyListeners();
                tv.refresh();
            }
        });
    }

    private void modifyCommentAndState(
        IStructuredSelection iStructuredSelection,
        DispatchSpecimenState newState) {
        String previousComment = null;
        if (iStructuredSelection.size() == 1) {
            previousComment =
                CommentWrapper
                    .commentListToString(((DispatchSpecimenWrapper) ((TreeItemAdapter) iStructuredSelection
                        .getFirstElement()).getSpecimen())
                        .getCommentCollection(false));
        }
        ModifyStateDispatchDialog dialog = new ModifyStateDispatchDialog(
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
            previousComment, newState);
        int res = dialog.open();
        if (res == Dialog.OK) {
            String comment = dialog.getComment();
            for (Iterator<?> iter = iStructuredSelection.iterator(); iter
                .hasNext();) {
                DispatchSpecimenWrapper dsa =
                    (DispatchSpecimenWrapper) ((TreeItemAdapter) iter
                        .next()).getSpecimen();
                CommentWrapper commentOb = new CommentWrapper(
                    SessionManager.getAppService());
                commentOb.setCreatedAt(new Date());
                commentOb.setUser(SessionManager.getUser());
                commentOb.setMessage(comment);
                dsa.addToCommentCollection(Arrays.asList(commentOb));
                if (newState != null) {
                    dsa.setDispatchSpecimenState(newState);
                }
            }
            shipment.resetMap();
            notifyListeners();
            tv.refresh();
        }
    }

    @SuppressWarnings("nls")
    public void refresh() {
        tv.setInput(i18n.tr("refresh"));
    }

    @SuppressWarnings("nls")
    public void addClickListener() {
        tv.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                DispatchSpecimenWrapper selection = getSelectedSpecimen();
                if (selection != null) {
                    SessionManager.openViewForm(selection.getSpecimen());
                }
            }
        });
        editItem = new MenuItem(getMenu(), SWT.PUSH);
        editItem.setText(
            // menu item label.
            i18n.tr("Edit"));
        editItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DispatchSpecimenWrapper selection = getSelectedSpecimen();
                if (selection != null) {
                    AbstractAdapterBase adapter = AdapterFactory
                        .getAdapter(selection.getSpecimen());
                    adapter.openEntryForm();
                }
            }
        });
    }

    @Override
    public Menu getMenu() {
        return menu;
    }

}
