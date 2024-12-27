/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CodeCompletionTokenizer
    implements ICodeCompletionTokenizer
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
            var tokenIterator = new TokenIterator(text, isDelimiter);
            var tokens =
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(tokenIterator, Spliterator.IMMUTABLE), false)
                    .limit(3)
                    .collect(Collectors.toList());

            var tokenBuilder = new StringBuilder();
            for (var i = 0; i < tokens.size(); i++)
            {
                var token = tokens.get(i);
                var tokenText = token.getText();
                var isDelimiterToken = token.isDelimiterToken();
                tokenBuilder.append(tokenText);
                if (!isDelimiterToken && tokenText.length() >= minLength)
                {
                    break;
                }
            }

            var tokenText = tokenBuilder.toString();
            return new CodeCompletionToken(tokenBuilder.toString(), text.substring(tokenText.length()));
        }

        if (text == null)
        {
            text = ""; //$NON-NLS-1$
        }

        return new CodeCompletionToken(text, ""); //$NON-NLS-1$
    }

    private static class TokenIterator
        implements Iterator<Token>
    {
        private final String text;
        private final Predicate<Character> isDelimiter;
        private int index;

        public TokenIterator(String text, Predicate<Character> isDelimiter)
        {
            this.text = text;
            this.isDelimiter = isDelimiter;
        }

        @Override
        public boolean hasNext()
        {
            return index < text.length();
        }

        @Override
        public Token next()
        {
            var len = text.length();
            var start = index;
            var lastIsDelimiterToken = false;
            while (index < len)
            {
                var ch = text.charAt(index);
                var isDelimiterToken = isDelimiter.test(ch);
                if (index == start)
                {
                    lastIsDelimiterToken = isDelimiterToken;
                }
                else
                {
                    if (isDelimiterToken != lastIsDelimiterToken)
                    {
                        return new Token(text.substring(start, index), lastIsDelimiterToken);
                    }
                }

                index++;
            }

            return new Token(text.substring(start, index), lastIsDelimiterToken);
        }
    }

    private static class Token
    {
        private final String text;
        private final boolean isDelimiterToken;

        public Token(String text, boolean isDelimiterToken)
        {
            this.text = text;
            this.isDelimiterToken = isDelimiterToken;
        }

        public String getText()
        {
            return text;
        }

        public boolean isDelimiterToken()
        {
            return isDelimiterToken;
        }
    }
}
