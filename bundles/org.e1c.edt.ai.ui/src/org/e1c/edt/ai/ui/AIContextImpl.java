/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.ISettingsStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class AIContextImpl
    implements IAIContext
{
    private final ILog log;
    private final IUI ui;
    private final ISettingsProvider settingsProvider;

    public AIContextImpl(ILog log, IUI ui, ISettingsProvider settingsProvider)
    {
        this.log = log;
        this.ui = ui;
        this.settingsProvider = settingsProvider;
    }

    @Override
    public Optional<AIContext> create()
    {
        return ui.getSelection().map(selection -> new AIContext(selection.getText(), selection.getOffset()));
    }

    @Override
    public Optional<AIContext> create(IDocument document, int cursorOffset)
    {
        var start = cursorOffset - settingsProvider.getSettings()
            .map(settings -> settings.getMaxAssistantTextSize())
            .orElse(ISettingsStore.DEFAULTMAXASSISTANTTEXTSIZE);

        if (start < 0)
        {
            start = 0;
        }

        try
        {
            var text = document.get(start, cursorOffset - start);
            if (start > 0)
            {
                var linePosition = text.indexOf(System.lineSeparator());
                if (linePosition >= 0 && linePosition < text.length() - 1)
                {
                    var textPart = text.substring(linePosition);
                    if (!textPart.isBlank())
                    {
                        text = textPart;
                    }
                }
            }

            return Optional.of(new AIContext(text, cursorOffset));
        }
        catch (BadLocationException e)
        {
            log.logError(e);
        }

        return Optional.empty();
    }

    @Override
    public void apply(IDocument document, AIContext aiContext)
    {
        try
        {
            document.replace(aiContext.getCursorOffset(), 0, aiContext.getInput());
        }
        catch (BadLocationException e)
        {
            log.logError(e);
        }
    }
}
