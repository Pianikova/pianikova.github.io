/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContentProvider
    implements IContentProvider
{
    private final IUI ui;

    @Inject
    public ContentProvider(IUI ui)
    {
        Preconditions.checkNotNull(ui);
        this.ui = ui;
    }

    @Override
    public Content get(StyledText textWidget)
    {
        return ui.getSourceViewer(textWidget)
            .map(sourceViewer -> get(textWidget, sourceViewer))
            .orElseGet(() -> new Content(textWidget.getText(), textWidget.getCaretOffset(), "", 0)); //$NON-NLS-1$
    }

    private Content get(StyledText textWidget, SourceViewer sourceViewer)
    {
        var text = sourceViewer.getDocument().get();
        var offset = sourceViewer.widgetOffset2ModelOffset(textWidget.getCaretOffset());
        var selection = sourceViewer.getSelection();
        if (selection.isEmpty() || !(selection instanceof ITextSelection))
        {
            return new Content(text, offset, "", 0); //$NON-NLS-1$
        }

        var textSelection = (ITextSelection)selection;
        var selectionStart = textSelection.getOffset();
        var selectionFinish = textSelection.getOffset() + textSelection.getLength();
        var selectionText = text.substring(selectionStart, selectionFinish);
        return new Content(text, offset, selectionText, offset - selectionStart);
    }
}
