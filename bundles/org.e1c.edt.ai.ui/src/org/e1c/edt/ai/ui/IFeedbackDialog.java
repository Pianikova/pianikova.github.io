/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.assistent.model.IssueType;

public interface IFeedbackDialog
{
    int show();

    void setHasCodeCompletion(boolean hasCodeCompletion);

    IssueType getIssueType();

    String getIssueDescription();
}
