/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Stack;

public class HintHistory
    implements IHintHistory
{
    private final Stack<Text> history = new Stack<>();

    @Override
    public int getCount()
    {
        return history.size();
    }

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
    public synchronized void push(Text text)
    {
        history.push(text);
    }

    @Override
    public synchronized Text pull()
    {
        if (history.isEmpty())
        {
            return Text.EMPTY;
        }

        return history.pop();
    }
}
