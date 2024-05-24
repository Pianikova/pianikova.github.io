/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContextParts;
import org.e1c.edt.ai.IAIContextSplitter;
import org.eclipse.jface.text.ITextSelection;

import com.google.inject.Inject;

public class AIContextProvider
    implements IAIContextProvider
{
    private static final String PRE_KEYWORD = "<PRE> "; //$NON-NLS-1$
    private static final String SUF_KEYWORD = " <SUF>"; //$NON-NLS-1$
    private static final String MID_KEYWORD = " <MID>"; //$NON-NLS-1$
    private static final int TEMPLATE_LENGTH = PRE_KEYWORD.length() + SUF_KEYWORD.length() + MID_KEYWORD.length();
    private final IUI ui;
    private final IUISettings uiSettings;
    private final IAIContextSplitter contextSplitter;

    @Inject
    public AIContextProvider(IUI ui, IUISettings uiSettings, IAIContextSplitter contextSplitter)
    {
        this.ui = ui;
        this.uiSettings = uiSettings;
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

        if (offset >= text.length())
        {
            offset = text.length() - 1;
        }

        if (text == null || text.isEmpty() || offset < 0)
        {
            return Optional.empty();
        }

        var parts = contextSplitter.split(text, offset, uiSettings.getMaxAssistantTextSize());
        String context;
        if (uiSettings.isTemplatedContext())
        {
            context = ctreateTemplatedContext(text, parts);
        }
        else
        {
            context = ctreateSimpleContext(text, parts);
        }

        return Optional.of(new AIContext(offset, text, context, parts.isTrimmedMiddle()));
    }

    private String ctreateTemplatedContext(String text, AIContextParts parts)
    {
        var prefix = normalize(parts.getPrefix().apply(text));
        var sufix = normalize(parts.getSufix().apply(text));
        var middle = normalize(parts.getMiddle().apply(text));
        var sb = new StringBuffer(prefix.length() + sufix.length() + middle.length() + TEMPLATE_LENGTH);
        sb.append(PRE_KEYWORD);
        sb.append(prefix);
        sb.append(SUF_KEYWORD);
        sb.append(sufix);
        sb.append(MID_KEYWORD);
        sb.append(middle);
        return sb.toString();
    }

    private String ctreateSimpleContext(String text, AIContextParts parts)
    {
        var prefix = normalize(parts.getPrefix().apply(text));
        var middle = normalize(parts.getMiddle().apply(text));
        var sb = new StringBuffer(prefix.length() + middle.length());
        sb.append(prefix);
        sb.append(middle);
        return sb.toString();
    }

    private String normalize(String text)
    {
        return text.replace(System.lineSeparator(), "\n"); //$NON-NLS-1$
    }
}