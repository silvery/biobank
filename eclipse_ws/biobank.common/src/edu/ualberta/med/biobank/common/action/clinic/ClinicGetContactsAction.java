package edu.ualberta.med.biobank.common.action.clinic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import edu.ualberta.med.biobank.common.action.Action;
import edu.ualberta.med.biobank.common.action.clinic.ClinicGetContactsAction.Response;
import edu.ualberta.med.biobank.common.action.exception.ActionException;
import edu.ualberta.med.biobank.common.util.NotAProxy;
import edu.ualberta.med.biobank.model.Contact;
import edu.ualberta.med.biobank.model.User;

public class ClinicGetContactsAction implements Action<Response> {

    private static final long serialVersionUID = 1L;

    // @formatter:off
    @SuppressWarnings("nls")
    private static final String HQL =
        "SELECT contact " 
        + " FROM " + Contact.class.getName() + " contact"
        + " INNER JOIN contact.clinic clinic"
        + " WHERE clinic.id=?";
    // @formatter:on

    private final Integer clinicId;

    public ClinicGetContactsAction(Integer clinicId) {
        this.clinicId = clinicId;
    }

    @Override
    public boolean isAllowed(User user, Session session) {
        return true;
    }

    @Override
    public Response run(User user, Session session)
        throws ActionException {
        Response response = new Response();

        Query query = session.createQuery(HQL);
        query.setParameter(0, clinicId);

        @SuppressWarnings("unchecked")
        List<Contact> rs = query.list();
        if (rs != null) {
            response.contacts.addAll(rs);
        }
        return response;
    }

    public static class Response implements Serializable, NotAProxy {
        private static final long serialVersionUID = 1L;

        private ArrayList<Contact> contacts;

        public Response() {
            contacts = new ArrayList<Contact>();
        }

        public ArrayList<Contact> getContacts() {
            return contacts;
        }
    }
}