/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.assistent.model.Completion;
import org.eclipse.core.resources.IProject;

public interface ICodeAssistant
{
    public IObservable<Completion> createSource(IProject project,
        ICompletionRequestProvider completionRequestProvider,
        ICancellationToken cancellationToken);
}
