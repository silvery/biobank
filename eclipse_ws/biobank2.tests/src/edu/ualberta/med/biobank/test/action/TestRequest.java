package edu.ualberta.med.biobank.test.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.ConstraintViolationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetInfoAction;
import edu.ualberta.med.biobank.common.action.collectionEvent.CollectionEventGetInfoAction.CEventInfo;
import edu.ualberta.med.biobank.common.action.info.ResearchGroupReadInfo;
import edu.ualberta.med.biobank.common.action.request.RequestClaimAction;
import edu.ualberta.med.biobank.common.action.request.RequestDeleteAction;
import edu.ualberta.med.biobank.common.action.request.RequestGetSpecimenInfosAction;
import edu.ualberta.med.biobank.common.action.request.RequestStateChangeAction;
import edu.ualberta.med.biobank.common.action.researchGroup.ResearchGroupGetInfoAction;
import edu.ualberta.med.biobank.common.action.researchGroup.SubmitRequestAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenInfo;
import edu.ualberta.med.biobank.common.util.RequestSpecimenState;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.Request;
import edu.ualberta.med.biobank.model.RequestSpecimen;
import edu.ualberta.med.biobank.test.action.helper.CollectionEventHelper;
import edu.ualberta.med.biobank.test.action.helper.PatientHelper;
import edu.ualberta.med.biobank.test.action.helper.RequestHelper;
import edu.ualberta.med.biobank.test.action.helper.ResearchGroupHelper;
import edu.ualberta.med.biobank.test.action.helper.StudyHelper;

public class TestRequest extends TestAction {

    @Rule
    public TestName testname = new TestName();

    private String name;
    private Integer studyId;

    private Integer rgId;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        name = testname.getMethodName() + R.nextInt();
        studyId =
            StudyHelper
                .createStudy(EXECUTOR, name, ActivityStatus.ACTIVE);
        rgId =
            ResearchGroupHelper.createResearchGroup(EXECUTOR,
                name + "rg",
                name + "rg",
                studyId);
    }

    @Test
    public void testUpload() throws Exception {

        ResearchGroupGetInfoAction reader =
            new ResearchGroupGetInfoAction(rgId);
        ResearchGroupReadInfo rg = EXECUTOR.exec(reader);

        // create specs
        Integer p =
            PatientHelper.createPatient(EXECUTOR, name + "_patient",
                rg.rg.getStudy().getId());
        Integer ceId =
            CollectionEventHelper.createCEventWithSourceSpecimens(EXECUTOR,
                p, rgId);

        CollectionEventGetInfoAction ceReader =
            new CollectionEventGetInfoAction(ceId);
        CEventInfo ceInfo = EXECUTOR.exec(ceReader);
        List<String> specs = new ArrayList<String>();
        for (SpecimenInfo specInfo : ceInfo.sourceSpecimenInfos)
            specs.add(specInfo.specimen.getInventoryId());

        Assert.assertTrue(ceInfo.sourceSpecimenInfos.size() >= 2);
        specs.remove(Math.abs(R.nextInt()) % specs.size());
        specs.remove(Math.abs(R.nextInt()) % specs.size());

        // request specs
        SubmitRequestAction action =
            new SubmitRequestAction(rgId, specs);
        Integer rId = EXECUTOR.exec(action).getId();

        // make sure you got what was requested
        RequestGetSpecimenInfosAction specAction =
            new RequestGetSpecimenInfosAction(rId);
        List<Object[]> specInfo = EXECUTOR.exec(specAction).getList();

        for (int i = 0; i < specInfo.size(); i++) {
            RequestSpecimen spec = (RequestSpecimen) specInfo.get(i)[0];
            Assert.assertTrue(specs.contains(spec.getSpecimen()
                .getInventoryId()));
        }
    }

    @Test
    public void testClaim() throws Exception {
        Integer rId = RequestHelper.createRequest(EXECUTOR, rgId);

        RequestGetSpecimenInfosAction specAction =
            new RequestGetSpecimenInfosAction(rId);
        List<Object[]> specInfo = EXECUTOR.exec(specAction).getList();
        List<Integer> ids = new ArrayList<Integer>();

        for (int i = 0; i < specInfo.size(); i++) {
            RequestSpecimen spec = (RequestSpecimen) specInfo.get(i)[0];
            ids.add(spec.getId());
        }

        RequestClaimAction claimAction =
            new RequestClaimAction(ids);
        EXECUTOR.exec(claimAction);

        specInfo = EXECUTOR.exec(specAction).getList();
        for (int i = 0; i < specInfo.size(); i++) {
            RequestSpecimen spec = (RequestSpecimen) specInfo.get(i)[0];
            Assert.assertTrue(spec.getClaimedBy() != null
                && !spec.getClaimedBy().equals(""));
        }

    }

    @Test
    public void testStateChanges() throws Exception {
        Integer rId = RequestHelper.createRequest(EXECUTOR, rgId);

        RequestGetSpecimenInfosAction specAction =
            new RequestGetSpecimenInfosAction(rId);
        List<Object[]> specInfo = EXECUTOR.exec(specAction).getList();
        List<Integer> ids = new ArrayList<Integer>();

        for (int i = 0; i < specInfo.size(); i++) {
            RequestSpecimen spec = (RequestSpecimen) specInfo.get(i)[0];
            ids.add(spec.getId());
        }

        RequestStateChangeAction dispatchAction =
            new RequestStateChangeAction(ids,
                RequestSpecimenState.DISPATCHED_STATE);
        EXECUTOR.exec(dispatchAction);

        specInfo = EXECUTOR.exec(specAction).getList();
        for (int i = 0; i < specInfo.size(); i++) {
            RequestSpecimen spec = (RequestSpecimen) specInfo.get(i)[0];
            Assert.assertTrue(RequestSpecimenState.getState(spec.getState())
                .equals(RequestSpecimenState.DISPATCHED_STATE));
        }
    }

    @Test
    public void testDelete() throws Exception {
        Integer rId = RequestHelper.createRequest(EXECUTOR, rgId);

        RequestDeleteAction delete = new RequestDeleteAction(rId);
        EXECUTOR.exec(delete);

        rId = RequestHelper.createRequest(EXECUTOR, rgId);
        session.beginTransaction();
        Request r = (Request) session.get(Request.class, rId);
        r.setSubmitted(new Date());
        session.saveOrUpdate(r);
        session.getTransaction().commit();
        delete = new RequestDeleteAction(rId);
        try {
            EXECUTOR.exec(delete);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // good
        }
    }

}