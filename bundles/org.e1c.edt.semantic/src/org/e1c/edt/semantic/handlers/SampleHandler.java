package org.e1c.edt.semantic.handlers;

import org.e1c.edt.ai.Closeables;
import org.e1c.edt.semantic.Activator;
import org.e1c.edt.semantic.IV8Model;
import org.e1c.edt.semantic.IWebServer;
import org.e1c.edt.semantic.WebServerSettings;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.google.inject.Inject;

public class SampleHandler extends AbstractHandler {

    @Inject
    IWebServer server;

    @Inject
    IV8Model model;

    private AutoCloseable serverStartToken = Closeables.Empty;


    public SampleHandler()
    {
        Activator.injectMembers(this);
    }

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
        try
        {
            serverStartToken.close();
        }
        catch (Exception e)
        {
            // ignored
        }

	    var settings = new WebServerSettings();
	    settings.Port = 9000;
        serverStartToken = server.start(settings);
        // IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        // MessageDialog.openInformation(window.getShell(), "1С Semantic Endpoint", "Hello, Eclipse world");
		return null;
	}
}