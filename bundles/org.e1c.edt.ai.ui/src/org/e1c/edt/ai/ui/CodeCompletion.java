/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.Closeables;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class CodeCompletion implements ICodeCompletion
{
    private final Provider<ICodeCompletionViewModel> codeCompletionViewModelProvider;
    private AutoCloseable query = Closeables.Empty;

    @Inject
    public CodeCompletion(Provider<ICodeCompletionViewModel> codeCompletionViewModelProvider)
    {
        this.codeCompletionViewModelProvider = codeCompletionViewModelProvider;
    }

    @Override
    public synchronized void show(boolean askImmediately)
    {
        try
        {
            query.close();
        }
        catch (Exception e)
        {
            // ignored
        }

        query = codeCompletionViewModelProvider.get().activate(askImmediately);
    }
}