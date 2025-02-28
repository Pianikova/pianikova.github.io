/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.assistent.model.IssueType;

interface IFeedbackDialog
{
    int show();

    void setHasCodeCompletion(boolean hasCodeCompletion);

    IssueType getIssueType();

    String getIssueDescription();
}
