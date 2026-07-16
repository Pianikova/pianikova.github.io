/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IUI;
import com.e1c.edt.ai.ui.Images;
import com.google.inject.Inject;

/**
 * "Fix with 1C:Workmate" quick fix for a standard problem marker (Problems view): opens the
 * marker's file in an editor and delegates to the shared {@link ExternalProblemFixer} core,
 * which selects the problem region and sends one fix request through the "Fix code" flow.
 */
public class ExternalProblemMarkerResolution
    implements IMarkerResolution2
{
    @Inject
    IUI ui;
    @Inject
    ILog log;

    private final ExternalProblemFixer fixer = new ExternalProblemFixer();

    public ExternalProblemMarkerResolution()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public String getLabel()
    {
        return fixer.getLabel();
    }

    @Override
    public String getDescription()
    {
        return fixer.getDescription();
    }

    @Override
    public Image getImage()
    {
        return BaseActivator.getImage(Images.AI);
    }

    @Override
    public void run(IMarker marker)
    {
        try
        {
            var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window == null || window.getActivePage() == null)
            {
                return;
            }
            IDE.openEditor(window.getActivePage(), marker);

            int charStart = marker.getAttribute(IMarker.CHAR_START, -1);
            int charEnd = marker.getAttribute(IMarker.CHAR_END, -1);
            int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
            var message = marker.getAttribute(IMarker.MESSAGE, ""); //$NON-NLS-1$

            // The focus/last-source-viewer bookkeeping settles asynchronously after openEditor —
            // pick up the viewer on the next UI cycle, exactly when the editor is already active.
            Display.getDefault().asyncExec(() -> ui.getLastSourceViewer().ifPresentOrElse(viewer -> {
                int offset = charStart;
                int length = charEnd > charStart ? charEnd - charStart : 0;
                if (offset < 0 && line > 0 && viewer.getDocument() != null)
                {
                    try
                    {
                        offset = viewer.getDocument().getLineInformation(line - 1).getOffset();
                    }
                    catch (Exception e)
                    {
                        log.logError(e);
                    }
                }
                fixer.fix(viewer, offset, length, message);
            }, () -> log.warning("Fix problem marker: no source viewer after opening editor", //$NON-NLS-1$
                () -> String.valueOf(marker))));
        }
        catch (Exception e)
        {
            log.logError(e);
        }
    }
}
