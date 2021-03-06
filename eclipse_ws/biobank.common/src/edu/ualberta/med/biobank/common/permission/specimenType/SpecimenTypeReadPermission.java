package edu.ualberta.med.biobank.common.permission.specimenType;

import edu.ualberta.med.biobank.common.action.ActionContext;
import edu.ualberta.med.biobank.common.permission.Permission;
import edu.ualberta.med.biobank.model.PermissionEnum;

public class SpecimenTypeReadPermission implements Permission {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean isAllowed(ActionContext context) {
        return PermissionEnum.SPECIMEN_TYPE_READ.isAllowed(context.getUser());
    }
}
