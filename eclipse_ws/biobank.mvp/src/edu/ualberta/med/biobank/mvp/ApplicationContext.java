package edu.ualberta.med.biobank.mvp;

import edu.ualberta.med.biobank.model.Center;
import edu.ualberta.med.biobank.model.User;

public interface ApplicationContext {
    User getUser();

    Integer getUserId();

    Center getWorkingCenter();

    Integer getWorkingCenterId();
}
