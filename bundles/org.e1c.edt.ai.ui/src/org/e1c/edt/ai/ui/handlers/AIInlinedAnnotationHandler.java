/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ui.Activator;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.ui.editor.XtextEditor;

/**
 * @author Bogdan Sushkov
 *
 */
public class AIInlinedAnnotationHandler
    extends AbstractHandler
{


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        boolean activated =
            Activator.getDefault().getPreferenceStore().getBoolean(Activator.PREF_CODE_COMPLITION_ENABLED);
        Activator.getDefault().getPreferenceStore().setValue(Activator.PREF_CODE_COMPLITION_ENABLED, !activated);
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        XtextEditor editor = activePart.getAdapter(XtextEditor.class);
        if (editor != null)
        {
            ((ISourceViewerExtension5)editor.getInternalSourceViewer()).updateCodeMinings();
        }
        return null;
    }

}
