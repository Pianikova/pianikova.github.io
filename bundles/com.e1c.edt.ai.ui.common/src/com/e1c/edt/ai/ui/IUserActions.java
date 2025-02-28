/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CodeCompletionAction;
import org.eclipse.swt.events.VerifyEvent;

interface IUserActions
{
    String getCodeCompletionLabels(char separator);

    CodeCompletionAction getAction(VerifyEvent event);
}
