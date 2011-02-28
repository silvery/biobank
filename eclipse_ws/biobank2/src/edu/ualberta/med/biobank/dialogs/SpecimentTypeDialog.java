package edu.ualberta.med.biobank.dialogs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.wrappers.SpecimenTypeWrapper;
import edu.ualberta.med.biobank.validators.NonEmptyStringValidator;
import edu.ualberta.med.biobank.widgets.BiobankText;

public class SpecimentTypeDialog extends BiobankDialog {

    private static final String TITLE = "Source Vessel ";

    private static final String MSG_NO_ST_NAME = "Source vessel must have a name.";

    private SpecimenTypeWrapper origSpecimen;

    // this is the object that is modified via the bound widgets
    private SpecimenTypeWrapper specimenType;

    private String message;

    private SpecimenTypeWrapper oldSpecimen;

    private String currentTitle;

    public SpecimentTypeDialog(Shell parent, SpecimenTypeWrapper specimenType,
        String message) {
        super(parent);
        Assert.isNotNull(specimenType);
        origSpecimen = specimenType;
        this.specimenType = new SpecimenTypeWrapper(null);
        this.specimenType.setName(specimenType.getName());
        this.message = message;
        oldSpecimen = new SpecimenTypeWrapper(SessionManager.getAppService());
        currentTitle = ((origSpecimen.getName() == null) ? "Add " : "Edit ")
            + TITLE;
    }

    @Override
    protected String getDialogShellTitle() {
        return currentTitle;
    }

    @Override
    protected String getTitleAreaMessage() {
        return message;
    }

    @Override
    protected String getTitleAreaTitle() {
        return currentTitle;
    }

    @Override
    protected void createDialogAreaInternal(Composite parent) {
        Composite content = new Composite(parent, SWT.NONE);
        content.setLayout(new GridLayout(2, false));
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createBoundWidgetWithLabel(content, BiobankText.class, SWT.BORDER,
            "Name", null, specimenType, "name", new NonEmptyStringValidator(
                MSG_NO_ST_NAME));

    }

    @Override
    protected void okPressed() {
        oldSpecimen.setName(origSpecimen.getName());
        origSpecimen.setName(specimenType.getName());
        super.okPressed();
    }

    public SpecimenTypeWrapper getOrigSpecimenType() {
        return oldSpecimen;
    }

}