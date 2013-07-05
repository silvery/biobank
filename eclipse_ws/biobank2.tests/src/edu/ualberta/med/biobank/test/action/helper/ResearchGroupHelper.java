package edu.ualberta.med.biobank.test.action.helper;

import edu.ualberta.med.biobank.action.IActionExecutor;
import edu.ualberta.med.biobank.common.action.info.AddressSaveInfo;
import edu.ualberta.med.biobank.common.action.info.ResearchGroupSaveInfo;
import edu.ualberta.med.biobank.common.action.researchGroup.ResearchGroupSaveAction;
import edu.ualberta.med.biobank.model.ActivityStatus;

public class ResearchGroupHelper extends Helper {

    public static Integer createResearchGroup(
        IActionExecutor actionExecutor,
        String name, String nameShort, Integer studyId) throws Exception {
        AddressSaveInfo addressSaveInfo =
            new AddressSaveInfo(null, "test", "test", "test", "test", "test",
                "test", "test", "test", "test");
        ResearchGroupSaveInfo save =
            new ResearchGroupSaveInfo(null, name + "rg", nameShort + "rg",
                studyId, "comment", addressSaveInfo,
                ActivityStatus.ACTIVE);
        ResearchGroupSaveAction rgSave = new ResearchGroupSaveAction(save);

        return actionExecutor.exec(rgSave).getId();
    }
}
