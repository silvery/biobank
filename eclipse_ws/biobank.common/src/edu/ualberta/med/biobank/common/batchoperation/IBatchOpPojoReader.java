package edu.ualberta.med.biobank.common.batchoperation;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import org.supercsv.io.ICsvBeanReader;

import edu.ualberta.med.biobank.common.action.Action;
import edu.ualberta.med.biobank.common.action.IdResult;
import edu.ualberta.med.biobank.common.action.batchoperation.IBatchOpInputPojo;

public interface IBatchOpPojoReader<T extends IBatchOpInputPojo> {

    public Set<T> readPojos(ICsvBeanReader reader)
        throws ClientBatchOpErrorsException, IOException;

    public ClientBatchOpInputErrorList getErrorList();

    public Action<IdResult> getAction() throws NoSuchAlgorithmException,
        IOException, ClassNotFoundException;

}
