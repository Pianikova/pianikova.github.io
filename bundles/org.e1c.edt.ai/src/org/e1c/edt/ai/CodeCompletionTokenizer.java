/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.function.Predicate;

public class CodeCompletionTokenizer implements ICodeCompletionTokenizer
{
    @Override
    public CodeCompletionToken getNext(int minLength, String text, Predicate<Character> isDelimiter)
    {
        if (minLength < 1)
        {
            minLength = 1;
        }

        if (text != null && !text.isBlank())
        {
            var index = 0;
            do
            {
                var isDelimiterToken = isDelimiter.test(text.charAt(index));
                index++;

                while (index < text.length() && isDelimiterToken == isDelimiter.test(text.charAt(index)))
                    index++;

                if (index >= minLength)
                {
                    return new CodeCompletionToken(text.substring(0, index), text.substring(index));
                }
            }
            while (index < text.length());
        }

        return new CodeCompletionToken(text, ""); //$NON-NLS-1$
    }
}
