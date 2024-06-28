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

    private static final String SYS_OPEN_KEYWORD = "<<SYS>>"; //$NON-NLS-1$
    private static final String SYS_CLOSED_KEYWORD = "<</SYS>>"; //$NON-NLS-1$
    private static final String INST_OPEN_KEYWORD = "[INST]"; //$NON-NLS-1$
    private static final String INST_CLOSED_KEYWORD = "[/INST]"; //$NON-NLS-1$

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
    public Optional<AIContext> create(String text, int offset, CodeCompletionType codeCompletionType)
    {
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(offset >= 0);
        if (text.isEmpty())
        {
            return Optional.of(new AIContext(0, "", "", codeCompletionType)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (offset > text.length())
        {
            offset = text.length();
        }

        if (codeCompletionType == CodeCompletionType.CodeComments)
        {
            return Optional.of(createDocContext(offset, text));
        }

        var parts = contextSplitter.split(text, offset, contextSettings.getMaxLength());
        if (contextSettings.isTempleted())
        {
            return Optional.of(createTemplatedContext(offset, text, parts));
        }

        return Optional.of(createSimpleContext(offset, text, parts));
    }

    private AIContext createDocContext(int offset, String text)
    {
        var prompt =
            "Ты русскоязычный ассистент разработчика в среде 1C Enterprise,помогаешь вести разработку на языке bsl."; //$NON-NLS-1$
        var query = "Напиши комментарий к методу:\n"; //$NON-NLS-1$
        var method = stringNormalizer.normalize(text, true);
        var sb = new StringBuilder();
        sb.append(INST_OPEN_KEYWORD);
        sb.append(SYS_OPEN_KEYWORD);
        sb.append(prompt);
        sb.append(SYS_CLOSED_KEYWORD);
        sb.append(query);
        sb.append(method);
        sb.append(INST_CLOSED_KEYWORD);
        return new AIContext(offset, text, sb.toString(), CodeCompletionType.CodeComments);
    }

    private AIContext createTemplatedContext(int offset, String text, AIContextParts parts)
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
        return new AIContext(offset, text, sb.toString(), getComplitionType(sufix));
    }

    private AIContext createSimpleContext(int offset, String text, AIContextParts parts)
    {
        var prefix = stringNormalizer.normalize(parts.getPrefix().apply(text), true);
        var sufix = stringNormalizer.normalize(parts.getSufix().apply(text), true);
        var middle = stringNormalizer.normalize(parts.getMiddle().apply(text), false);
        var sb = new StringBuffer(prefix.length() + middle.length());
        sb.append(prefix);
        sb.append(middle);
        return new AIContext(offset, text, sb.toString(), getComplitionType(sufix));
    }

    private CodeCompletionType getComplitionType(String sufix)
    {
        var lineFinish = sufix.indexOf('\n');
        if (lineFinish > 0 && !sufix.substring(0, lineFinish).isBlank())
        {
            return CodeCompletionType.CodeSingleWord;
        }

        return CodeCompletionType.CodeLines;
    }
}