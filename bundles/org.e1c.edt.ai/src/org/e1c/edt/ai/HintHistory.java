/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Stack;

public class HintHistory implements IHintHistory
{
    private final Stack<String> history = new Stack<>();

    @Override
    public synchronized boolean isEmpty()
    {
        return history.isEmpty();
    }

    @Override
    public synchronized void clear()
    {
        history.clear();
    }

    @Override
    public synchronized void push(String text)
    {
        history.push(text);
    }

    @Override
    public synchronized String pull()
    {
        if (history.isEmpty())
        {
            return ""; //$NON-NLS-1$
        }

        return history.pop();
    }
}
