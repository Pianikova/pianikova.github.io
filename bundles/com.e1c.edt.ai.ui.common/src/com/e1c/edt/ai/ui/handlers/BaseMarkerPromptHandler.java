package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.e1c.edt.ai.tools.SetMarkersMcpTool;

public class BaseMarkerPromptHandler
    extends AbstractHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        var selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection)
        {
            var element = ((IStructuredSelection)selection).getFirstElement();
            if (element instanceof IMarker)
            {
                var marker = (IMarker)element;
                try
                {
                    var details = marker.getAttribute(SetMarkersMcpTool.ACTION_DETAILS_ATTRIBUTE);
                    if (details != null && details instanceof SetMarkersMcpTool.MarkerRequest)
                    {
                        // Execute AI prompt
                        executeAiPrompt(marker, (SetMarkersMcpTool.MarkerRequest)details);
                    }
                }
                catch (CoreException e)
                {
                    //
                }
            }
        }

        return null;
    }

    private void executeAiPrompt(IMarker marker, SetMarkersMcpTool.MarkerRequest details)
    {
        // Implementation to send prompt to AI system
    }
}