package edu.ualberta.med.biobank.treeview.admin;

import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;

import edu.ualberta.med.biobank.common.wrappers.ClinicWrapper;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.forms.ClinicEntryForm;
import edu.ualberta.med.biobank.forms.ClinicViewForm;
import edu.ualberta.med.biobank.treeview.AdapterBase;

public class ClinicAdapter extends AdapterBase {

    public ClinicAdapter(AdapterBase parent, ClinicWrapper clinicWrapper) {
        super(parent, clinicWrapper);
        setEditable(parent instanceof ClinicMasterGroup || parent == null);
    }

    public ClinicWrapper getWrapper() {
        return (ClinicWrapper) modelObject;
    }

    @Override
    protected String getLabelInternal() {
        ClinicWrapper wrapper = getWrapper();
        Assert.isNotNull(wrapper, "client is null");
        return wrapper.getNameShort();
    }

    @Override
    public String getTooltipText() {
        return getTooltipText("Clinic");
    }

    @Override
    public void popupMenu(TreeViewer tv, Tree tree, Menu menu) {
        addEditMenu(menu, "Clinic");
        addViewMenu(menu, "Clinic");
        addDeleteMenu(menu, "Clinic");
    }

    @Override
    protected String getConfirmDeleteMessage() {
        return "Are you sure you want to delete this clinic?";
    }

    @Override
    public boolean isDeletable() {
        return internalIsDeletable();
    }

    @Override
    protected AdapterBase createChildNode() {
        return null;
    }

    @Override
    protected AdapterBase createChildNode(ModelWrapper<?> child) {
        return null;
    }

    @Override
    protected Collection<? extends ModelWrapper<?>> getWrapperChildren()
        throws Exception {
        return null;
    }

    @Override
    protected int getWrapperChildCount() throws Exception {
        return 0;
    }

    @Override
    public String getEntryFormId() {
        return ClinicEntryForm.ID;
    }

    @Override
    public String getViewFormId() {
        return ClinicViewForm.ID;
    }

}
