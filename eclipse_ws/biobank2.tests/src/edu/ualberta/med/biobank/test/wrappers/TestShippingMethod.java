package edu.ualberta.med.biobank.test.wrappers;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import edu.ualberta.med.biobank.common.exception.BiobankCheckException;
import edu.ualberta.med.biobank.common.wrappers.ClinicWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContactWrapper;
import edu.ualberta.med.biobank.common.wrappers.PatientWrapper;
import edu.ualberta.med.biobank.common.wrappers.ShipmentWrapper;
import edu.ualberta.med.biobank.common.wrappers.ShippingMethodWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.common.wrappers.StudyWrapper;
import edu.ualberta.med.biobank.model.ShippingMethod;
import edu.ualberta.med.biobank.test.TestDatabase;
import edu.ualberta.med.biobank.test.internal.ClinicHelper;
import edu.ualberta.med.biobank.test.internal.ContactHelper;
import edu.ualberta.med.biobank.test.internal.PatientHelper;
import edu.ualberta.med.biobank.test.internal.ShipmentHelper;
import edu.ualberta.med.biobank.test.internal.ShippingMethodHelper;
import edu.ualberta.med.biobank.test.internal.SiteHelper;
import edu.ualberta.med.biobank.test.internal.StudyHelper;

public class TestShippingMethod extends TestDatabase {
    @Test
    public void testGettersAndSetters() throws Exception {
        String name = "testGettersAndSetters" + r.nextInt();

        ShippingMethodWrapper company = ShippingMethodHelper
            .addShippingMethod(name);
        testGettersAndSetters(company);
    }

    @Test
    public void testGetShipmentCollection() throws Exception {
        String name = "testGetShipmentCollection" + r.nextInt();
        SiteWrapper site = SiteHelper.addSite(name);
        ClinicWrapper clinic = ClinicHelper.addClinic(site, name);
        StudyWrapper study = StudyHelper.addStudy(name);
        ContactWrapper contact = ContactHelper.addContact(clinic, name);
        study.addContacts(Arrays.asList(contact));
        study.persist();
        PatientWrapper patient1 = PatientHelper.addPatient(name, study);

        ShippingMethodWrapper company1 = ShippingMethodHelper
            .addShippingMethod(name);
        ShippingMethodWrapper company2 = ShippingMethodHelper
            .addShippingMethod(name + "_2");

        ShipmentWrapper shipment1 = ShipmentHelper
            .addShipment(site, clinic, patient1);
        shipment1.setShippingMethod(company1);
        shipment1.persist();
        ShipmentWrapper shipment2 = ShipmentHelper
            .addShipment(site, clinic, patient1);
        shipment2.setShippingMethod(company2);
        shipment2.persist();
        ShipmentWrapper shipment3 = ShipmentHelper
            .addShipment(site, clinic, patient1);
        shipment3.setShippingMethod(company2);
        shipment3.persist();

        company1.reload();
        company2.reload();
        Assert.assertEquals(1, company1.getShipmentCollection().size());
        Assert.assertEquals(2, company2.getShipmentCollection().size());
    }

    @Test
    public void testGetShipmentCollectionBoolean() throws Exception {
        String name = "testGetShipmentCollectionBoolean" + r.nextInt();
        SiteWrapper site = SiteHelper.addSite(name);
        ClinicWrapper clinic = ClinicHelper.addClinic(site, name);
        StudyWrapper study = StudyHelper.addStudy(name);
        ContactWrapper contact = ContactHelper.addContact(clinic, name);
        study.addContacts(Arrays.asList(contact));
        study.persist();
        PatientWrapper patient1 = PatientHelper.addPatient(name, study);

        ShippingMethodWrapper company = ShippingMethodHelper
            .addShippingMethod(name);

        ShipmentWrapper shipment1 = ShipmentHelper
            .addShipment(site, clinic, patient1);
        shipment1.setShippingMethod(company);
        shipment1.setWaybill("QWERTY" + name);
        shipment1.persist();
        ShipmentWrapper shipment2 = ShipmentHelper
            .addShipment(site, clinic, patient1);
        shipment2.setShippingMethod(company);
        shipment1.setWaybill("ASDFG" + name);
        shipment2.persist();
        ShipmentWrapper shipment3 = ShipmentHelper
            .addShipment(site, clinic, patient1);
        shipment3.setShippingMethod(company);
        shipment1.setWaybill("ghrtghd" + name);
        shipment3.persist();

        company.reload();
        List<ShipmentWrapper> shipments = company.getShipmentCollection(true);
        if (shipments.size() > 1) {
            for (int i = 0; i < shipments.size() - 1; i++) {
                ShipmentWrapper s1 = shipments.get(i);
                ShipmentWrapper s2 = shipments.get(i + 1);
                Assert.assertTrue(s1.compareTo(s2) <= 0);
            }
        }
    }

    @Test
    public void testGetShippingCompanies() throws Exception {
        String name = "testGetShippingCompanies" + r.nextInt();
        int sizeBefore = ShippingMethodWrapper.getShippingMethods(appService)
            .size();

        ShippingMethodHelper.addShippingMethod(name);
        ShippingMethodHelper.addShippingMethod(name + "_2");

        int sizeAfter = ShippingMethodWrapper.getShippingMethods(appService)
            .size();

        Assert.assertEquals(sizeBefore + 2, sizeAfter);
    }

    @Test
    public void testPersist() throws Exception {
        String name = "testPersist" + r.nextInt();
        ShippingMethodWrapper company = ShippingMethodHelper
            .newShippingMethod(name);
        company.persist();
        ShippingMethodHelper.createdCompanies.add(company);

        ShippingMethod shipComp = new ShippingMethod();
        shipComp.setId(company.getId());
        Assert.assertEquals(1, appService
            .search(ShippingMethod.class, shipComp).size());
    }

    @Test
    public void testDelete() throws Exception {
        String name = "testDelete" + r.nextInt();
        ShippingMethodWrapper company = ShippingMethodHelper.addShippingMethod(
            name, false);

        ShippingMethod shipComp = new ShippingMethod();
        shipComp.setId(company.getId());
        Assert.assertEquals(1, appService
            .search(ShippingMethod.class, shipComp).size());

        company.delete();

        Assert.assertEquals(0, appService
            .search(ShippingMethod.class, shipComp).size());
    }

    @Test
    public void testDeleteFailNoShipments() throws Exception {
        String name = "testDeleteFailNoShipments" + r.nextInt();
        ShippingMethodWrapper company = ShippingMethodHelper.addShippingMethod(
            name, false);

        SiteWrapper site = SiteHelper.addSite(name);
        ClinicWrapper clinic = ClinicHelper.addClinic(site, name);
        StudyWrapper study = StudyHelper.addStudy(name);
        ContactWrapper contact = ContactHelper.addContact(clinic, name);
        study.addContacts(Arrays.asList(contact));
        study.persist();
        PatientWrapper patient1 = PatientHelper.addPatient(name, study);
        ShipmentWrapper shipment1 = ShipmentHelper
            .addShipment(site, clinic, patient1);
        shipment1.setShippingMethod(company);
        shipment1.persist();
        company.reload();

        try {
            company.delete();
            Assert.fail("one shipment in the collection");
        } catch (BiobankCheckException bce) {
            Assert.assertTrue(true);
        }

        shipment1.setShippingMethod(null);
        shipment1.persist();
        company.reload();
        company.delete();
    }

    @Test
    public void testResetAlreadyInDatabase() throws Exception {
        String name = "testResetAlreadyInDatabase" + r.nextInt();
        ShippingMethodWrapper company = ShippingMethodHelper
            .addShippingMethod(name);
        company.setName("QQQQ");
        company.reset();
        Assert.assertEquals(name, company.getName());
    }

    @Test
    public void testResetNew() throws Exception {
        String name = "testResetNew" + r.nextInt();
        ShippingMethodWrapper company = ShippingMethodHelper
            .newShippingMethod(name);
        company.setName("QQQQ");
        company.reset();
        Assert.assertEquals(null, company.getName());
    }

    @Test
    public void testCompareTo() throws Exception {
        String name = "testCompareTo" + r.nextInt();
        ShippingMethodWrapper company1 = ShippingMethodHelper
            .addShippingMethod("QWERTY" + name);
        ShippingMethodWrapper company2 = ShippingMethodHelper
            .addShippingMethod("ASDFG" + name);
        Assert.assertTrue(company1.compareTo(company2) > 0);
        Assert.assertTrue(company2.compareTo(company1) < 0);
    }

}
