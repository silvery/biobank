package edu.ualberta.med.biobank.test.model;

import junit.framework.Assert;

import org.hibernate.Query;
import org.hibernate.Transaction;
import org.junit.Test;

import edu.ualberta.med.biobank.model.RequestSpecimen;
import edu.ualberta.med.biobank.model.type.RequestSpecimenState;
import edu.ualberta.med.biobank.test.TestDb;
import edu.ualberta.med.biobank.test.model.util.HibernateHelper;

public class TestRequestSpecimen extends TestDb {
    @Test
    public void stateIds() {
        Transaction tx = session.beginTransaction();

        RequestSpecimen requestSpecimen = factory.createRequestSpecimen();

        Query query = HibernateHelper.getDehydratedPropertyQuery(
            session, requestSpecimen, "state");

        try {
            for (RequestSpecimenState state : RequestSpecimenState.values()) {
                requestSpecimen.setState(state);
                session.update(requestSpecimen);
                session.flush();

                int id = ((Number) query.uniqueResult()).intValue();
                Assert.assertEquals("persisted id does not match enum's id",
                    state.getId(), new Integer(id));
            }
        } finally {
            tx.rollback();
        }
    }
}
