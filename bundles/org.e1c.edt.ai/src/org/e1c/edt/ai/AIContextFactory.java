/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AIContextFactory
    implements IAIContextFactory
{
    private static final String PRE_KEYWORD = "<PRE> "; //$NON-NLS-1$
    private static final String SUF_KEYWORD = " <SUF>"; //$NON-NLS-1$
    private static final String MID_KEYWORD = " <MID>"; //$NON-NLS-1$
    private static final int TEMPLATE_LENGTH = PRE_KEYWORD.length() + SUF_KEYWORD.length() + MID_KEYWORD.length();
    private final IAIContextSplitter contextSplitter;
    private final IAIContextSettings contextSettings;
    private final IStringNormalizer stringNormalizer;

    @Inject
    public AIContextFactory(IAIContextSplitter contextSplitter, IAIContextSettings contextSettings,
        IStringNormalizer stringNormalizer)
    {
        Preconditions.checkNotNull(contextSplitter);
        Preconditions.checkNotNull(contextSettings);
        Preconditions.checkNotNull(stringNormalizer);
        this.contextSplitter = contextSplitter;
        this.contextSettings = contextSettings;
        this.stringNormalizer = stringNormalizer;
    }

    @Override
    public Optional<AIContext> create(String text, int offset)
    {
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(offset >= 0);
        if (text.isEmpty())
        {
            return Optional.of(new AIContext(0, "", "", true)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (offset > text.length())
        {
            offset = text.length();
        }

        var parts = contextSplitter.split(text, offset, contextSettings.getMaxLength());
        if (contextSettings.isTempleted())
        {
            return Optional.of(ctreateTemplatedContext(offset, text, parts));
        }

        return Optional.of(ctreateSimpleContext(offset, text, parts));
    }

    private AIContext ctreateTemplatedContext(int offset, String text, AIContextParts parts)
    {
        var prefix = stringNormalizer.normalize(parts.getPrefix().apply(text), true);
        var sufix = stringNormalizer.normalize(parts.getSufix().apply(text), true);
        var middle = stringNormalizer.normalize(parts.getMiddle().apply(text), false);
        var sb = new StringBuffer(prefix.length() + sufix.length() + middle.length() + TEMPLATE_LENGTH);
        sb.append(PRE_KEYWORD);
        sb.append(prefix);
        sb.append(SUF_KEYWORD);
        sb.append(sufix);
        sb.append(MID_KEYWORD);
        sb.append(middle);
        return new AIContext(offset, text, sb.toString(), isSingleWord(sufix));
    }

    private AIContext ctreateSimpleContext(int offset, String text, AIContextParts parts)
    {
        var prefix = stringNormalizer.normalize(parts.getPrefix().apply(text), true);
        var sufix = stringNormalizer.normalize(parts.getSufix().apply(text), true);
        var middle = stringNormalizer.normalize(parts.getMiddle().apply(text), false);
        var sb = new StringBuffer(prefix.length() + middle.length());
        sb.append(prefix);
        sb.append(middle);
        return new AIContext(offset, text, sb.toString(), isSingleWord(sufix));
    }

    private boolean isSingleWord(String sufix)
    {
        var lineFinish = sufix.indexOf('\n');
        if (lineFinish > 0)
        {
            return !sufix.substring(0, lineFinish).isBlank();
        }

        return false;
    }
}