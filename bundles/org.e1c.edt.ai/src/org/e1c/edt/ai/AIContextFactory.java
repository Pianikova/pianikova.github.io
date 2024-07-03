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

    private static final String SYS_OPEN_PREFIX =
        "<s>[INST] <<SYS>>\nТы русскоязычный ассистент разработчика в среде 1C Enterprise, помогаешь вести разработку на языке bsl.\n<</SYS>>\n"; //$NON-NLS-1$
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
    public Optional<AIContext> create(String source, int sourceOffset, String text, int offset, CodeCompletionType codeCompletionType)
    {
        Preconditions.checkNotNull(source);
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

        if (codeCompletionType == CodeCompletionType.CodeComments
            || codeCompletionType == CodeCompletionType.CodeCommentsContinue)
        {
            return Optional.of(createCommentsContext(source, sourceOffset, text, offset, codeCompletionType));
        }

        var parts = contextSplitter.split(text, offset, contextSettings.getMaxLength());
        if (contextSettings.isTempleted())
        {
            return Optional.of(createTemplatedContext(text, offset, parts));
        }

        return Optional.of(createSimpleContext(text, offset, parts));
    }

    @SuppressWarnings("nls")
    private AIContext createCommentsContext(String source, int sourceOffset, String text, int offset,
        CodeCompletionType codeCompletionType)
    {
        var method = stringNormalizer.normalize(text, true);
        var sb = new StringBuilder();
        sb.append(SYS_OPEN_PREFIX);
        if (codeCompletionType == CodeCompletionType.CodeCommentsContinue)
        {
            sb.append("Продолжай писать комментария к методу");
        }
        else
        {
            sb.append("Напиши комментарий к методу");
        }

        sb.append(" ```");
        sb.append(method.stripLeading());
        sb.append("``` ");
        sb.append(INST_CLOSED_KEYWORD);

        if (codeCompletionType == CodeCompletionType.CodeCommentsContinue)
        {
            var pos = sourceOffset - 1;
            while (pos >= 0 && source.charAt(pos) != '\n')
            {
                pos--;
            }

            if (pos + 1 < sourceOffset)
            {
                var comment = source.substring(pos + 1, sourceOffset);
                sb.append(comment);
            }
        }

        return new AIContext(offset, text, sb.toString(), codeCompletionType);
    }

    private AIContext createTemplatedContext(String text, int offset, AIContextParts parts)
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

    private AIContext createSimpleContext(String text, int offset, AIContextParts parts)
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