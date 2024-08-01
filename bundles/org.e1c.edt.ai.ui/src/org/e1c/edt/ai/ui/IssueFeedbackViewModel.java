/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ICodeCompletionStatistics;
import org.e1c.edt.ai.assistent.IFeedbackService;
import org.eclipse.jface.window.Window;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class IssueFeedbackViewModel implements IIssueFeedbackViewModel
{
    private final IFeedbackDialog feedbackDialog;
    private final ICodeCompletionStatistics codeCompletionStatistics;
    private final IFeedbackService feedbackService;

    @Inject
    public IssueFeedbackViewModel(IFeedbackDialog feedbackDialog, ICodeCompletionStatistics codeCompletionStatistics,
        IFeedbackService feedbackService)
    {
        Preconditions.checkNotNull(feedbackDialog);
        Preconditions.checkNotNull(codeCompletionStatistics);
        Preconditions.checkNotNull(feedbackService);
        this.feedbackDialog = feedbackDialog;
        this.codeCompletionStatistics = codeCompletionStatistics;
        this.feedbackService = feedbackService;
    }

    @Override
    public void getFeedback()
    {
        var lastAcceptedSourceId = codeCompletionStatistics.getLastAcceptedSourceId();
        feedbackDialog.setHasCodeCompletion(lastAcceptedSourceId.isPresent());
        if (feedbackDialog.show() != Window.OK)
        {
            return;
        }

        var issueType = feedbackDialog.getIssueType();
        var issueDescription = feedbackDialog.getIssueDescription();
        feedbackService.issueAsync(lastAcceptedSourceId.orElse(null), issueType, issueDescription);
    }
}
