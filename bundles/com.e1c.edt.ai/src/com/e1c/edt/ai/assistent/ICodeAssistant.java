/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.assistent.model.Completion;
import com.e1c.edt.ai.assistent.model.ProjectId;

public interface ICodeAssistant
{
    public IObservable<Completion> createSource(ProjectId projectId,
        ICompletionRequestProvider completionRequestProvider,
        ICancellationToken cancellationToken);
}
