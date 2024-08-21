package org.e1c.edt.semantic.handlers;

import org.e1c.edt.ai.Closeables;
import org.e1c.edt.semantic.Activator;
import org.e1c.edt.semantic.IEndpointDialog;
import org.e1c.edt.semantic.IV8Model;
import org.e1c.edt.semantic.IWebServer;
import org.e1c.edt.semantic.WebServerSettings;
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
    IWebServer server;

    @Inject
    IV8Model model;

    @Inject
    IEndpointDialog endpointDialog;

    private AutoCloseable serverStartToken = Closeables.Empty;

    public ActivateHandler()
    {
        Activator.injectMembers(this);
    }

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
        if (serverStartToken != Closeables.Empty)
        {
            try
            {
                serverStartToken.close();
            }
            catch (Exception e)
            {
                // ignored
            }
            finally
            {
                serverStartToken = Closeables.Empty;
                IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
                MessageDialog.openInformation(window.getShell(), "Semantic Endpoint", "Deactivated"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            return null;
        }

        if (endpointDialog.show() != Window.OK)
        {
            return null;
        }

	    var settings = new WebServerSettings();
        settings.Port = endpointDialog.getPort();
        serverStartToken = server.start(settings);
		return null;
	}
}