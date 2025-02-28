/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ICodeCompletionStatistics;
import com.e1c.edt.ai.assistent.IFeedbackService;
import org.eclipse.jface.window.Window;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class IssueFeedbackViewModel implements IIssueFeedbackViewModel
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
