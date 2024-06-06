/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class AIContextFactory
    implements IAIContextFactory
{
    private static final String PRE_KEYWORD = "<PRE> "; //$NON-NLS-1$
    private static final String SUF_KEYWORD = " <SUF>"; //$NON-NLS-1$
    private static final String MID_KEYWORD = " <MID>"; //$NON-NLS-1$
    private static final int TEMPLATE_LENGTH = PRE_KEYWORD.length() + SUF_KEYWORD.length() + MID_KEYWORD.length();
    private final IAIContextSplitter contextSplitter;
    private final Provider<AIContextSettings> contextSettingsProvider;

    @Inject
    public AIContextFactory(IAIContextSplitter contextSplitter, Provider<AIContextSettings> contextSettingsProvider)
    {
        Preconditions.checkNotNull(contextSplitter);
        Preconditions.checkNotNull(contextSettingsProvider);
        this.contextSplitter = contextSplitter;
        this.contextSettingsProvider = contextSettingsProvider;
    }

    @Override
    public Optional<AIContext> create(String text, int offset)
    {
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(offset >= 0);
        if (text.isEmpty())
        {
            return Optional.of(new AIContext(0, "", "")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (offset > text.length())
        {
            offset = text.length();
        }

        var settings = contextSettingsProvider.get();
        var parts = contextSplitter.split(text, offset, settings.getMaxLength());
        String context;
        if (settings.isTempleted())
        {
            context = ctreateTemplatedContext(text, parts);
        }
        else
        {
            context = ctreateSimpleContext(text, parts);
        }

        return Optional.of(new AIContext(offset, text, context));
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