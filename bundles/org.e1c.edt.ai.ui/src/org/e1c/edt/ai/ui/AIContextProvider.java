/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContextParts;
import org.e1c.edt.ai.IAIContextSplitter;
import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.ISettingsStore;
import org.eclipse.jface.text.ITextSelection;

public class AIContextProvider
    implements IAIContextProvider
{
    private static final String PRE_KEYWORD = "<PRE> "; //$NON-NLS-1$
    private static final String SUF_KEYWORD = " <SUF>"; //$NON-NLS-1$
    private static final String MID_KEYWORD = " <MID>"; //$NON-NLS-1$
    private static final int TEMPLATE_LENGTH = PRE_KEYWORD.length() + SUF_KEYWORD.length() + MID_KEYWORD.length();
    private final ISettingsProvider settingsProvider;
    private IUI ui;
    private IAIContextSplitter contextSplitter;

    public AIContextProvider(IUI ui, ISettingsProvider settingsProvider, IAIContextSplitter contextSplitter)
    {
        this.ui = ui;
        this.settingsProvider = settingsProvider;
        this.contextSplitter = contextSplitter;
    }

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

        var maxLength = getMaxLength();
        var parts = contextSplitter.split(text, offset, maxLength);
        var context = ctreateContextByTemplate(text, parts);
        return Optional.of(new AIContext(cursorOffset, text, context));
    }

    private String ctreateContextByTemplate(String text, AIContextParts parts)
    {
        var prefix = normalize(parts.getPrefix().apply(text));
        var sufix = normalize(parts.getSufix().apply(text));
        var sb = new StringBuffer(prefix.length() + sufix.length() + TEMPLATE_LENGTH);
        sb.append(PRE_KEYWORD);
        sb.append(prefix);
        sb.append(SUF_KEYWORD);
        sb.append(sufix);
        sb.append(MID_KEYWORD);
        return sb.toString();
    }

    private String normalize(String text)
    {
        return text.replace(System.lineSeparator(), "\n"); //$NON-NLS-1$
    }

    private int getMaxLength()
    {
        var length = settingsProvider.getSettings()
            .map(settings -> settings.getMaxAssistantTextSize())
            .orElse(ISettingsStore.DEFAULTMAXASSISTANTTEXTSIZE) - TEMPLATE_LENGTH;

        if (length < ISettingsStore.MINASSISTANTTEXTSIZE)
        {
            return length;
        }

        return length;
    }
}