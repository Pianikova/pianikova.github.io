package org.e1c.edt.semantic.handlers;

import org.e1c.edt.semantic.Activator;
import org.e1c.edt.semantic.IEndpointDialog;
import org.e1c.edt.semantic.IEndpointViewModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.google.inject.Inject;

public class ActivateHandler extends AbstractHandler {

    @Inject
    IEndpointViewModel endpointViewModel;

    @Inject
    IEndpointDialog endpointDialog;

    public ActivateHandler()
    {
        Activator.injectMembers(this);
    }

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
        if (endpointViewModel.isActive())
        {
            endpointViewModel.deactivate();
            IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
            MessageDialog.openInformation(window.getShell(), "Semantic Endpoint", //$NON-NLS-1$
                "Deactivated on the port " + endpointViewModel.getPort()); //$NON-NLS-1$
            return null;
        }

        if (endpointDialog.show() != Window.OK)
        {
            return null;
        }

        endpointViewModel.activate();
        return null;
	}
}