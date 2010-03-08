package edu.ualberta.med.biobank.test;

import edu.ualberta.med.biobank.common.ServiceConnection;
import edu.ualberta.med.biobank.test.internal.DbHelper;
import edu.ualberta.med.biobank.test.wrappers.TestClinic;
import edu.ualberta.med.biobank.test.wrappers.TestContact;
import edu.ualberta.med.biobank.test.wrappers.TestContainer;
import edu.ualberta.med.biobank.test.wrappers.TestContainerLabelingScheme;
import edu.ualberta.med.biobank.test.wrappers.TestContainerType;
import edu.ualberta.med.biobank.test.wrappers.TestPatient;
import edu.ualberta.med.biobank.test.wrappers.TestPatientVisit;
import edu.ualberta.med.biobank.test.wrappers.TestPvSourceVessel;
import edu.ualberta.med.biobank.test.wrappers.TestAliquot;
import edu.ualberta.med.biobank.test.wrappers.TestSourceVessel;
import edu.ualberta.med.biobank.test.wrappers.TestSampleStorage;
import edu.ualberta.med.biobank.test.wrappers.TestSampleType;
import edu.ualberta.med.biobank.test.wrappers.TestShipment;
import edu.ualberta.med.biobank.test.wrappers.TestShippingCompany;
import edu.ualberta.med.biobank.test.wrappers.TestSite;
import edu.ualberta.med.biobank.test.wrappers.TestStudy;
import gov.nih.nci.system.applicationservice.WritableApplicationService;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { TestSite.class, TestPatient.class, TestPatientVisit.class,
    TestStudy.class, TestContainerLabelingScheme.class,
    TestPvSourceVessel.class, TestAliquot.class, TestClinic.class,
    TestSampleStorage.class, TestSourceVessel.class, TestSampleType.class,
    TestContainer.class, TestContainerType.class, TestShipment.class,
    TestContact.class, TestShippingCompany.class })
public class AllTests {
    public static WritableApplicationService appService = null;

    @BeforeClass
    public static void setUp() throws Exception {
        appService = ServiceConnection.getAppService(System.getProperty(
            "server", "http://localhost:8080")
            + "/biobank2", "testuser", "test");
        DbHelper.setAppService(appService);
    }

    @AfterClass
    public static void tearDown() {
    }

}
