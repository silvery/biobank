package edu.ualberta.med.biobank.common.wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ualberta.med.biobank.common.exception.BiobankCheckException;
import edu.ualberta.med.biobank.common.peer.UserPeer;
import edu.ualberta.med.biobank.common.permission.GlobalAdminPermission;
import edu.ualberta.med.biobank.common.wrappers.base.UserBaseWrapper;
import edu.ualberta.med.biobank.model.User;
import edu.ualberta.med.biobank.server.applicationservice.BiobankApplicationService;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class UserWrapper extends UserBaseWrapper {

    private String password;

    private transient CenterWrapper<?> currentWorkingCenter;
    private Boolean lockedOut;

    public UserWrapper(WritableApplicationService appService, User wrappedObject) {
        super(appService, wrappedObject);
    }

    public UserWrapper(WritableApplicationService appService) {
        super(appService);
    }

    public void setPassword(String password) {
        String old = this.password;
        this.password = password;
        propertyChangeSupport.firePropertyChange("password", old, password); //$NON-NLS-1$

    }

    public String getPassword() {
        if (password == null && !isNew())
            try {
                password = ((BiobankApplicationService) appService)
                    .getUserPassword(getLogin());
            } catch (ApplicationException e) {
                return null;
            }
        return password;
    }

    @Override
    protected void resetInternalFields() {
        super.resetInternalFields();
        password = null;
        lockedOut = null;
    }

    private static final String GET_USER_QRY = "from " + User.class.getName() //$NON-NLS-1$
        + " where " + UserPeer.LOGIN.getName() + " = ?"; //$NON-NLS-1$ //$NON-NLS-2$

    public static UserWrapper getUser(BiobankApplicationService appService,
        String userName) throws BiobankCheckException, ApplicationException {
        HQLCriteria criteria = new HQLCriteria(GET_USER_QRY,
            Arrays.asList(new Object[] { userName }));
        List<User> users = appService.query(criteria);
        if (users == null || users.size() == 0)
            return null;
        if (users.size() == 1)
            return new UserWrapper(appService, users.get(0));
        throw new BiobankCheckException("Error retrieving users: found " //$NON-NLS-1$
            + users.size() + " results."); //$NON-NLS-1$
    }

    public SiteWrapper getCurrentWorkingSite() {
        if (currentWorkingCenter instanceof SiteWrapper)
            return (SiteWrapper) currentWorkingCenter;
        return null;
    }

    public CenterWrapper<?> getCurrentWorkingCenter() {
        return currentWorkingCenter;
    }

    public void setCurrentWorkingCenter(CenterWrapper<?> currentWorkingCenter) {
        this.currentWorkingCenter = currentWorkingCenter;
    }

    public boolean isSuperAdmin() {
        try {
            return ((BiobankApplicationService) appService).isAllowed(
                new GlobalAdminPermission());
        } catch (ApplicationException e) {
            return false;
        }
    }

    public boolean needChangePassword() {
        if (getNeedPwdChange() == null)
            return false;
        return getNeedPwdChange();
    }

    /**
     * if center is the current center, then current center is reset to be sure
     * it has latest modifications
     * 
     * @throws Exception
     */
    public void updateCurrentCenter(CenterWrapper<?> center) throws Exception {
        if (center != null && center.equals(currentWorkingCenter)) {
            currentWorkingCenter.reset();
        }
    }

    @Override
    public int compareTo(ModelWrapper<User> user2) {
        if (user2 instanceof UserWrapper) {
            String login1 = getLogin();
            String login2 = ((UserWrapper) user2).getLogin();

            if (login1 == null || login2 == null)
                return 0;
            return login1.compareTo(login2);
        }
        return 0;
    }

    public void setLockedOut(boolean lockedOut) {
        this.lockedOut = lockedOut;
    }

    public boolean isLockedOut() {
        if (lockedOut == null && getCsmUserId() != null)
            try {
                lockedOut = ((BiobankApplicationService) appService)
                    .isUserLockedOut(getCsmUserId());
            } catch (ApplicationException e) {
                // TODO log error ?
            lockedOut = false;
        }
        if (lockedOut == null)
            return false;
        return lockedOut;
    }

    private static final String ALL_USERS_QRY = " from " + User.class.getName(); //$NON-NLS-1$

    public static final List<UserWrapper> getAllUsers(
        WritableApplicationService appService) throws ApplicationException {
        HQLCriteria criteria = new HQLCriteria(ALL_USERS_QRY,
            new ArrayList<Object>());

        List<User> users = appService.query(criteria);
        return ModelWrapper.wrapModelCollection(appService, users,
            UserWrapper.class);
    }

    /**
     * This method should be called by the user itself. If another user is
     * connected to the server, the method will fail
     */
    public void modifyPassword(String oldPassword, String newPassword,
        Boolean bulkEmails) throws ApplicationException {
        ((BiobankApplicationService) appService).executeModifyPassword(
            getCsmUserId(), oldPassword, newPassword, bulkEmails);
    }

    @Override
    public String toString() {
        return getLogin();
    }

    @Override
    protected Set<CenterWrapper<?>> getAllCentersInvolved() throws Exception {
        Set<CenterWrapper<?>> centers = new HashSet<CenterWrapper<?>>();
        for (GroupWrapper g : getGroupCollection(false)) {
            centers.addAll(g.getAllCentersInvolved());
        }
        centers.addAll(super.getAllCentersInvolved());
        return centers;
    }
}
