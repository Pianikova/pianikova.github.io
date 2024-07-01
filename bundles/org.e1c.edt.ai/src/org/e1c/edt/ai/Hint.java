/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.function.Predicate;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class Hint
    implements IHistoricalHint
{
    public static final Predicate<Character> TOKEN_DELIMITER = Delimiters::isTokenDelimiter;
    public static final Predicate<Character> LINE_DELIMITER = Delimiters::isLineDelimiter;
    private final ICodeCompletionTokenizer tokenizer;
    private final StringBuilder text = new StringBuilder();
    private IHintHistory history;
    private int maxLines;
    private boolean isSingleWordMode, hasEdOfLine;

    @Inject
    public Hint(ICodeCompletionTokenizer tokenizer, IHintHistory defaultHistory)
    {
        Preconditions.checkNotNull(tokenizer);
        Preconditions.checkNotNull(defaultHistory);
        this.tokenizer = tokenizer;
        this.history = defaultHistory;
    }

    @Override
    public synchronized void initiaize(IHintHistory history, int maxLines, boolean isSingleWordMode)
    {
        Preconditions.checkNotNull(history);
        Preconditions.checkArgument(maxLines > 0);
        this.history = history;
        this.maxLines = maxLines;
        this.isSingleWordMode = isSingleWordMode;
        hasEdOfLine = false;
    }

    @Override
    public synchronized void append(String text)
    {
        Preconditions.checkNotNull(text);
        if (hasEdOfLine)
        {
            return;
        }

        if (isSingleWordMode)
        {
            var endOfLine = text.indexOf('\n');
            if (endOfLine >= 0)
            {
                this.text.append(text.substring(0, endOfLine));
                hasEdOfLine = true;
                return;
            }
        }

        this.text.append(text);
    }

    @Override
    public synchronized boolean isEmpty()
    {
        return text.length() == 0;
    }

    @Override
    public synchronized boolean isBlank()
    {
        return !text.chars().anyMatch(ch -> !Character.isWhitespace(ch));
    }

    @Override
    public synchronized boolean startsWith(char ch)
    {
        return !isEmpty() && text.charAt(0) == ch;
    }

    @Override
    public synchronized String getText(HintPart part)
    {
        if (isEmpty())
        {
            return ""; //$NON-NLS-1$
        }

        switch (part)
        {
        case TOKEN:
            var token = tokenizer.getNext(1, getText(HintPart.TEXT), TOKEN_DELIMITER);
            return token.getValue();

        case LINE:
            var line = tokenizer.getNext(2, getText(HintPart.TEXT), LINE_DELIMITER);
            return line.getValue();

        case LINES:
            var lines = new StringBuilder();
            var str = getText(HintPart.TEXT);
            var count = maxLines;
            while (count-- > 0 && !str.isEmpty())
            {
                var nextLine = tokenizer.getNext(1, str, LINE_DELIMITER);
                var value = nextLine.getValue();
                lines.append(value);
                str = nextLine.getText();
            }

            return lines.toString();

        case TEXT:
        default:
            return text.toString();
        }
    }

    @Override
    public synchronized String pull(HintPart part)
    {
        return pull(getText(part));
    }

    @SuppressWarnings("nls")
    @Override
    public synchronized String pullChar(char ch)
    {
        if (isEmpty())
        {
            return "";
        }

        var curChar = text.charAt(0);
        if (curChar != ch)
        {
            return "";
        }

        return pull(text.substring(0, 1));
    }

    @SuppressWarnings("nls")
    @Override
    public synchronized String rollback()
    {
        if (history.isEmpty())
        {
            return "";
        }

        var textToRollback = history.pull();
        text.insert(0, textToRollback);
        return textToRollback;
    }

    @Override
    public synchronized void clear()
    {
        text.setLength(0);
    }

    @Override
    public synchronized String toString()
    {
        return text.toString();
    }

    private String pull(String textPull)
    {
        if (textPull.isEmpty())
        {
            return textPull;
        }

        history.push(textPull);
        text.delete(0, textPull.length());
        return textPull;
    }
}
