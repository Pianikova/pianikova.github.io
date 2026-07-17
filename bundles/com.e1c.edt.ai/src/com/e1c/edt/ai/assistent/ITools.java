/**
 *
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IObservable;
import org.eclipse.core.resources.IProject;
import com.e1c.edt.ai.assistent.model.ToolFeedbackFinalTextRequest;
import com.e1c.edt.ai.assistent.model.ToolFeedbackResponse;
import com.e1c.edt.ai.assistent.model.ToolInvokeRequest;
import com.e1c.edt.ai.assistent.model.ToolInvokeResponse;

public interface ITools
{
    IObservable<ToolInvokeResponse> createInvokeSource(IProject project, ToolInvokeRequest toolInvokeRequest,
        ICancellationToken cancellationToken);

    CompletableFuture<Optional<ToolFeedbackResponse>> feedbackAsync(IProject project,
        ToolFeedbackFinalTextRequest feedbackRequest, ICancellationToken cancellationToken);
}
