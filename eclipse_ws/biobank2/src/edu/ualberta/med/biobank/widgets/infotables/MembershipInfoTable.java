package edu.ualberta.med.biobank.widgets.infotables;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import edu.ualberta.med.biobank.common.wrappers.MembershipWrapper;
import edu.ualberta.med.biobank.common.wrappers.PrincipalWrapper;
import edu.ualberta.med.biobank.common.wrappers.RoleWrapper;
import edu.ualberta.med.biobank.dialogs.user.MembershipEditDialog;
import edu.ualberta.med.biobank.gui.common.widgets.BgcLabelProvider;
import edu.ualberta.med.biobank.gui.common.widgets.IInfoTableDeleteItemListener;
import edu.ualberta.med.biobank.gui.common.widgets.IInfoTableEditItemListener;
import edu.ualberta.med.biobank.gui.common.widgets.InfoTableEvent;
import edu.ualberta.med.biobank.model.PermissionEnum;

public class MembershipInfoTable extends InfoTableWidget<MembershipWrapper> {
    public static final int ROWS_PER_PAGE = 7;
    private static final String[] HEADINGS = new String[] {
        Messages.MembershipInfoTable_center_label,
        Messages.MembershipInfoTable_study_label,
        Messages.MembershipInfoTable_role_label,
        Messages.MembershipInfoTable_permissions_label };

    protected static class TableRowData {
        MembershipWrapper ms;
        String center;
        String study;
        String roles;
        String permissions;

        @Override
        public String toString() {
            return StringUtils.join(new String[] { center, study, roles,
                permissions }, "\t"); //$NON-NLS-1$
        }
    }

    public MembershipInfoTable(Composite parent,
        final PrincipalWrapper<?> principal) {
        super(parent, principal.getMembershipCollection(true), HEADINGS,
            ROWS_PER_PAGE, MembershipWrapper.class);

        addEditItemListener(new IInfoTableEditItemListener<MembershipWrapper>() {
            @Override
            public void editItem(InfoTableEvent<MembershipWrapper> event) {
                MembershipWrapper user = ((TableRowData) getSelection()).ms;
                editMembership(user);
            }
        });

        addDeleteItemListener(new IInfoTableDeleteItemListener<MembershipWrapper>() {
            @Override
            public void deleteItem(InfoTableEvent<MembershipWrapper> event) {
                MembershipWrapper ms = ((TableRowData) getSelection()).ms;
                principal.removeFromMembershipCollection(Arrays.asList(ms));
                getList().remove(ms);
                reloadCollection(principal.getMembershipCollection(true));
            }
        });
    }

    protected void editMembership(MembershipWrapper ms) {
        MembershipEditDialog dlg = new MembershipEditDialog(PlatformUI
            .getWorkbench().getActiveWorkbenchWindow().getShell(), ms);
        int res = dlg.open();
        if (res == Dialog.OK) {
            reloadCollection(getList(), ms);
            notifyListeners();
        }
    }

    @SuppressWarnings("serial")
    @Override
    protected BiobankTableSorter getComparator() {
        return new BiobankTableSorter() {
            @Override
            public int compare(Object o1, Object o2) {
                if (o1 instanceof MembershipWrapper
                    && o2 instanceof MembershipWrapper) {
                    MembershipWrapper rp1 = (MembershipWrapper) o1;
                    MembershipWrapper rp2 = (MembershipWrapper) o2;
                    return rp1.compareTo(rp2);
                }
                return 0;
            }
        };
    }

    @Override
    public Object getCollectionModelObject(Object o) throws Exception {
        TableRowData info = new TableRowData();
        info.ms = (MembershipWrapper) o;
        info.center = info.ms.getCenter() == null ? Messages.MembershipInfoTable_all_label
            : info.ms.getCenter().getNameShort();
        info.study = info.ms.getStudy() == null ? Messages.MembershipInfoTable_all_label
            : info.ms.getStudy().getNameShort();
        info.roles = getRolesString(info.ms);
        info.permissions = getPermissionsString(info.ms);
        return info;
    }

    public String getRolesString(MembershipWrapper ms) {
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (RoleWrapper r : ms.getRoleCollection(true)) {
            if (sb.length() > 25) {
                sb.setLength(25);
                sb.append("..."); //$NON-NLS-1$
                break;
            }
            if (first)
                first = false;
            else
                sb.append(";"); //$NON-NLS-1$
            sb.append(r.getName());

        }
        return sb.toString();
    }

    public String getPermissionsString(MembershipWrapper ms) {
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (PermissionEnum perm : ms.getPermissionCollection()) {
            if (sb.length() > 25) {
                sb.setLength(25);
                sb.append("..."); //$NON-NLS-1$
                break;
            }
            if (first)
                first = false;
            else
                sb.append(";"); //$NON-NLS-1$
            sb.append(perm.name()); // TODO: localize
        }
        return sb.toString();
    }

    @Override
    protected String getCollectionModelObjectToString(Object o) {
        if (o == null)
            return null;
        return ((TableRowData) o).toString();
    }

    @Override
    protected BgcLabelProvider getLabelProvider() {
        return new BgcLabelProvider() {
            @Override
            public String getColumnText(Object element, int columnIndex) {
                TableRowData info = (TableRowData) ((BiobankCollectionModel) element).o;
                if (info == null) {
                    if (columnIndex == 0) {
                        return Messages.infotable_loading_msg;
                    }
                    return ""; //$NON-NLS-1$
                }
                switch (columnIndex) {
                case 0:
                    return info.center;
                case 1:
                    return info.study;
                case 2:
                    return info.roles;
                case 3:
                    return info.permissions;
                default:
                    return ""; //$NON-NLS-1$
                }
            }
        };
    }
}