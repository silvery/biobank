package edu.ualberta.med.biobank.common.permission.specimen;

import org.hibernate.Session;

import edu.ualberta.med.biobank.common.action.ActionContext;
import edu.ualberta.med.biobank.common.permission.Permission;
import edu.ualberta.med.biobank.common.permission.PermissionEnum;
import edu.ualberta.med.biobank.model.Center;
import edu.ualberta.med.biobank.model.Study;
import edu.ualberta.med.biobank.model.User;

public class SpecimenCreatePermission implements Permission {
    private static final long serialVersionUID = 1L;

    private final Integer centerId;
    private final Integer studyId;

    public SpecimenCreatePermission(Integer centerId, Integer studyId) {
        this.centerId = centerId;
        this.studyId = studyId;
    }

    @Override
    public boolean isAllowed(User user, Session session) {
        ActionContext actionContext = new ActionContext(user, session);
        Study study = actionContext.load(Study.class, studyId);
        Center center = actionContext.load(Center.class, centerId);
        return PermissionEnum.SPECIMEN_CREATE.isAllowed(user, center, study);
    }
}
