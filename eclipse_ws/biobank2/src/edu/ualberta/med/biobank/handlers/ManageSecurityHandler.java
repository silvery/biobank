package edu.ualberta.med.biobank.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.PlatformUI;

import edu.ualberta.med.biobank.dialogs.ManageSecurityDialog;

public class ManageSecurityHandler extends AbstractHandler implements IHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        new ManageSecurityDialog(PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getShell()).open();
        return null;
    }
}
