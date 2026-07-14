/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ui.eclipse;

import org.eclipse.jface.action.IAction;
import org.eclipse.mylyn.internal.wikitext.ui.editor.MarkupEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * Adapts the WikiText editor to the Eclipse 2022-03 handler service.
 * <p>
 * WikiText activates every text-editor action directly, while the standard text-editor action bar
 * contributor activates the same global actions. Eclipse then reports equal-priority conflicting
 * handlers. Only WikiText's additional content-assist handler needs the direct activation.
 */
@SuppressWarnings("restriction")
public class EclipseMarkdownEditor
    extends MarkupEditor
{
    @Override
    public void setAction(String actionId, IAction action)
    {
        var definitionId = action == null ? null : action.getActionDefinitionId();
        if (definitionId == null || ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS.equals(definitionId))
        {
            super.setAction(actionId, action);
            return;
        }

        action.setActionDefinitionId(null);
        try
        {
            super.setAction(actionId, action);
        }
        finally
        {
            action.setActionDefinitionId(definitionId);
        }
    }
}
