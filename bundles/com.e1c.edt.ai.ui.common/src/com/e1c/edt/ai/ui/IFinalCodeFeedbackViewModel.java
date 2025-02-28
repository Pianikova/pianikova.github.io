/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;

interface IFinalCodeFeedbackViewModel
{
    AutoCloseable activate(StyledText textWidget);
}
