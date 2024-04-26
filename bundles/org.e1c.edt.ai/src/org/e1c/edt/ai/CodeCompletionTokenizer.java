/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;

public class CodeCompletionTokenizer implements ICodeCompletionTokenizer
{
    private int minLength;

    public CodeCompletionTokenizer()
    {
        this(2);
    }

    public CodeCompletionTokenizer(int minLength)
    {
        Preconditions.checkArgument(minLength > 0);
        this.minLength = minLength;
    }

    @Override
    public CodeCompletionToken getNext(String text)
    {
        var nextToken = getNextToken(text);
        if (!nextToken.isEmpty())
        {
            var nextText = text.substring(nextToken.length());
            if (!getNextToken(nextText).isEmpty())
            {
                return new CodeCompletionToken(nextToken, nextText);
            }
        }

        return new CodeCompletionToken("", text); //$NON-NLS-1$
    }

    private String getNextToken(String text)
    {
        var chars = text.toCharArray();
        var length = chars.length;
        var cursor = 0;

        while (cursor < length && isWhiteSpace(chars[cursor++]))
            ;

        var size = 0;
        while (size < minLength && cursor < length)
        {
            while (cursor < length && !isWhiteSpace(chars[cursor++]))
            {
                size++;
            }
        }

        if (size >= minLength)
        {
            return text.substring(0, cursor - 1);
        }

        return ""; //$NON-NLS-1$
    }

    private Boolean isWhiteSpace(char ch)
    {
        return (ch == ' ') || (ch == '\t') || (ch == '\n') || (ch == '\r');
    }
}
