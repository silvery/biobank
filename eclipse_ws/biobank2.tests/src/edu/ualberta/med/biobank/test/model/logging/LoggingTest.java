package edu.ualberta.med.biobank.test.model.logging;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.After;
import org.junit.Before;

import edu.ualberta.med.biobank.test.TestDb;

public class LoggingTest extends TestDb {
    protected AuditReader auditReader;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        auditReader = AuditReaderFactory.get(session);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        auditReader = null;
        super.tearDown();
    }
}
