package edu.ualberta.med.biobank.treeview.admin;

import java.util.Map;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.common.action.info.ResearchGroupAdapterInfo;
import edu.ualberta.med.biobank.common.permission.researchGroup.ResearchGroupDeletePermission;
import edu.ualberta.med.biobank.common.permission.researchGroup.ResearchGroupReadPermission;
import edu.ualberta.med.biobank.common.permission.researchGroup.ResearchGroupUpdatePermission;
import edu.ualberta.med.biobank.forms.ResearchGroupEntryForm;
import edu.ualberta.med.biobank.forms.ResearchGroupViewForm;
import edu.ualberta.med.biobank.model.ResearchGroup;
import edu.ualberta.med.biobank.treeview.AbstractAdapterBase;
import edu.ualberta.med.biobank.treeview.AbstractNewAdapterBase;
import edu.ualberta.med.biobank.treeview.AdapterBase;

public class ResearchGroupAdapter extends AbstractNewAdapterBase {
    private static final I18n i18n = I18nFactory
        .getI18n(ResearchGroupAdapter.class);

    ResearchGroupAdapterInfo rg;

    public ResearchGroupAdapter(AbstractNewAdapterBase parent,
        ResearchGroupAdapterInfo rg) {
        super(parent, rg.id, rg.nameShort, null, false);
        this.rg = rg;
        if (rg.id != null) {
            init();
        }
    }

    @Override
    public void setValue(Object value) {
        this.rg = (ResearchGroupAdapterInfo) value;
        setId(rg.id);
        if (rg.id != null) {
            init();
        }
    }

    @Override
    public void init() {
        this.isDeletable = isAllowed(new ResearchGroupDeletePermission(rg.id));
        this.isReadable = isAllowed(new ResearchGroupReadPermission(rg.id));
        this.isEditable = isAllowed(new ResearchGroupUpdatePermission(rg.id));
    }

    @Override
    protected String getLabelInternal() {
        return rg.nameShort;
    }

    @Override
    public Integer getId() {
        return rg.id;
    }

    @Override
    public String getTooltipTextInternal() {
        return getTooltipText(ResearchGroup.NAME.singular().toString());
    }

    @Override
    public void popupMenu(TreeViewer tv, Tree tree, Menu menu) {
        addEditMenu(menu, ResearchGroup.NAME.singular().toString());
        addViewMenu(menu, ResearchGroup.NAME.singular().toString());
        addDeleteMenu(menu, ResearchGroup.NAME.singular().toString());
    }

    @SuppressWarnings("nls")
    @Override
    protected String getConfirmDeleteMessage() {
        return i18n.tr("Are you sure you want to delete this research group?");
    }

    @Override
    protected AdapterBase createChildNode() {
        return null;
    }

    @Override
    protected AbstractNewAdapterBase createChildNode(Object child) {
        return null;
    }

    @Override
    public String getEntryFormId() {
        return ResearchGroupEntryForm.ID;
    }

    @Override
    public String getViewFormId() {
        return ResearchGroupViewForm.ID;
    }

    @Override
    public int compareTo(AbstractAdapterBase o) {
        if (o instanceof ResearchGroupAdapter)
            return rg.id.compareTo(
                ((ResearchGroupAdapter) o).rg.id);
        return 0;
    }

    @Override
    protected void runDelete() throws Exception {
        // TODO: implement delete
    }

    @Override
    protected Map<Integer, ?> getChildrenObjects() throws Exception {
        return null;
    }
}
