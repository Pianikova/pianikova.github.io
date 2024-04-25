/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.ISettingsStore;
import org.eclipse.jface.text.ITextSelection;

public class AIContextImpl
    implements IAIContext
{
    private final ISettingsProvider settingsProvider;
    private IUI ui;

    public AIContextImpl(IUI ui, ISettingsProvider settingsProvider)
    {
        this.ui = ui;
        this.settingsProvider = settingsProvider;
    }

    @SuppressWarnings("nls")
    @Override
    public Optional<AIContext> create()
    {
        var viewer = ui.getTextViewer();
        if (viewer.isEmpty())
        {
            return Optional.empty();
        }

        var selectionProvider = viewer.get().getSelectionProvider();
        var selection = selectionProvider.getSelection();
        if (!(selection instanceof ITextSelection))
        {
            return Optional.empty();
        }

        var textSelection = (ITextSelection)selection;
        int cursorOffset = textSelection.getOffset();
        int offset;
        String text;
        if (textSelection.getLength() > 0)
        {
            offset = textSelection.getLength();
            text = textSelection.getText();
            cursorOffset += textSelection.getLength();
        }
        else
        {
            offset = cursorOffset;
            text = viewer.get().getDocument().get();
        }

        var start = offset - settingsProvider.getSettings()
            .map(settings -> settings.getMaxAssistantTextSize())
            .orElse(ISettingsStore.DEFAULTMAXASSISTANTTEXTSIZE);

        if (start < 0)
        {
            start = 0;
        }

        if (start > 0)
        {
            var linePosition = text.indexOf(System.lineSeparator(), start);
            if (linePosition >= 0)
            {
                start = linePosition;
            }
        }

        var prefix = text.substring(start, offset);
        String postfix;
        if (offset < text.length() - 1)
        {
            postfix = text.substring(offset, text.length() - 1);
        }
        else
        {
            postfix = "";
        }

        return Optional.of(new AIContext(cursorOffset, text, prefix, postfix));
    }
}