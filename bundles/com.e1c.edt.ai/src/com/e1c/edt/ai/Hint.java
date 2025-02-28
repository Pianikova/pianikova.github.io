/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.LinkedList;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class Hint
    implements IHistoricalHint
{
    public static final Predicate<Character> TOKEN_DELIMITER = Delimiters::isTokenDelimiter;
    public static final Predicate<Character> LINE_DELIMITER = Delimiters::isLineDelimiter;
    private final ICodeCompletionTokenizer tokenizer;
    private final LinkedList<TextItem> textItems = new LinkedList<>();
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
    public synchronized void append(Text text)
    {
        Preconditions.checkNotNull(text);
        if (hasEdOfLine)
        {
            return;
        }

        var lastText = getLast(text.getSource());
        if (isSingleWordMode)
        {
            var endOfLine = text.getText().indexOf('\n');
            if (endOfLine >= 0)
            {
                lastText.append(text.getText().substring(0, endOfLine));
                hasEdOfLine = true;
                return;
            }
        }

        lastText.append(text);
    }

    @Override
    public synchronized boolean isEmpty()
    {
        for (var textItem : textItems)
        {
            if (textItem.text.length() != 0)
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public synchronized boolean isBlank()
    {
        for (var textItem : textItems)
        {
            if (textItem.text.chars().anyMatch(ch -> !Character.isWhitespace(ch)))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public synchronized boolean startsWith(char ch)
    {
        if (textItems.size() == 0)
        {
            return false;
        }

        var text = textItems.getFirst().text;
        return text.length() > 0 && text.charAt(0) == ch;
    }

    @Override
    public synchronized Text getText(HintPart part)
    {
        if (isEmpty())
        {
            return Text.EMPTY;
        }

        switch (part)
        {
        case TOKEN:
            var tokenText = getText(HintPart.TEXT);
            var token = tokenizer.getNext(1, tokenText.getText(), TOKEN_DELIMITER);
            return new Text(token.getValue(), tokenText.getSource());

        case LINE:
            var lineText = getText(HintPart.TEXT);
            var line = tokenizer.getNext(2, lineText.getText(), LINE_DELIMITER);
            return new Text(line.getValue(), lineText.getSource());

        case LINES:
            var lines = new StringBuilder();
            var linesText = getText(HintPart.TEXT);
            var str = linesText.getText();
            var count = maxLines;
            while (count-- > 0 && !str.isEmpty())
            {
                var nextLine = tokenizer.getNext(1, str, LINE_DELIMITER);
                var value = nextLine.getValue();
                lines.append(value);
                str = nextLine.getText();
            }

            return new Text(lines.toString(), linesText.getSource());

        case TEXT:
        default:
            if (textItems.size() > 0)
            {
                var firstItem = textItems.getFirst();
                return new Text(firstItem.text.toString(), firstItem.source);
            }

            return Text.EMPTY;
        }
    }

    @Override
    public synchronized Text pull(HintPart part)
    {
        return pull(getText(part));
    }

    @SuppressWarnings("nls")
    @Override
    public synchronized Text pullChar(char ch)
    {
        if (textItems.size() == 0)
        {
            return Text.EMPTY;
        }

        var textItem = textItems.getFirst();
        if (textItem.text.length() == 0)
        {
            return new Text("", textItem.source);
        }

        var text = textItem.text;
        var curChar = text.charAt(0);
        if (curChar != ch)
        {
            return new Text("", textItem.source);
        }

        return pull(new Text(text.substring(0, 1), textItem.source));
    }

    @Override
    public synchronized Text rollback()
    {
        if (history.isEmpty())
        {
            return Text.EMPTY;
        }

        var textToRollback = history.pull();
        var textItem = getFirst(textToRollback.getSource());
        textItem.insert(0, textToRollback);
        return textToRollback;
    }

    @Override
    public synchronized void clear()
    {
        textItems.clear();
    }

    @Override
    public synchronized String toString()
    {
        var sb = new StringBuilder();
        for (var textItem : textItems)
        {
            sb.append(textItem.text);
        }

        return sb.toString();
    }

    private Text pull(Text textPull)
    {
        if (textPull.isEmpty())
        {
            return textPull;
        }

        var text = getFirst(textPull.getSource());
        history.push(textPull);
        text.delete(0, textPull.getText().length());
        if (text.length() == 0)
        {
            textItems.removeFirst();
        }

        return textPull;
    }

    private StringBuilder getFirst(ISource source)
    {
        if (textItems.size() == 0 || textItems.getFirst().source != source)
        {
            var newTextItem = new TextItem(source);
            textItems.addFirst(newTextItem);
            return newTextItem.text;
        }

        return textItems.getFirst().text;
    }

    private StringBuilder getLast(ISource source)
    {
        if (textItems.size() == 0 || textItems.getLast().source != source)
        {
            var newTextItem = new TextItem(source);
            textItems.addLast(newTextItem);
            return newTextItem.text;
        }

        return textItems.getLast().text;
    }

    private static class TextItem
    {
        public final StringBuilder text = new StringBuilder();
        public final ISource source;

        public TextItem(ISource source)
        {
            this.source = source;
        }
    }
}
