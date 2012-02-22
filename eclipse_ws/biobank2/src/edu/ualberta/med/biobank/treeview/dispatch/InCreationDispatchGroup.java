package edu.ualberta.med.biobank.treeview.dispatch;

import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.dispatch.DispatchRetrievalAction;
import edu.ualberta.med.biobank.common.util.DispatchState;
import edu.ualberta.med.biobank.common.wrappers.DispatchWrapper;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.treeview.AbstractAdapterBase;
import edu.ualberta.med.biobank.treeview.AdapterBase;

public class InCreationDispatchGroup extends AbstractDispatchGroup {

    public InCreationDispatchGroup(AdapterBase parent, int id) {
        super(parent, id, Messages.InCreationDispatchGroup_creation_node_label);
    }

    @Override
    protected List<? extends ModelWrapper<?>> getWrapperChildren()
        throws Exception {
        return ModelWrapper.wrapModelCollection(SessionManager.getAppService(),
            SessionManager.getAppService().doAction(
                new DispatchRetrievalAction(DispatchState.CREATION,
                    SessionManager.getUser().getCurrentWorkingCenter().getId(),
                    true, true)).getList(), DispatchWrapper.class);
    }

    @Override
    public void popupMenu(TreeViewer tv, Tree tree, Menu menu) {
        if (SessionManager.canCreate(DispatchWrapper.class)) {
            MenuItem mi = new MenuItem(menu, SWT.PUSH);
            mi.setText(Messages.InCreationDispatchGroup_add_label);
            mi.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    addDispatch();
                }
            });
        }
    }

    protected void addDispatch() {
        DispatchWrapper shipment = new DispatchWrapper(
            SessionManager.getAppService());
        DispatchAdapter shipNode = new DispatchAdapter(this, shipment);
        shipNode.openEntryForm();
    }

    @Override
    public int compareTo(AbstractAdapterBase o) {
        return 0;
    }

}
