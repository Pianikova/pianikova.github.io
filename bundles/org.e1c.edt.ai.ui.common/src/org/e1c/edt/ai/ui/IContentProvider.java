/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;

public interface IContentProvider
{
    Content get(StyledText textWidget, int offset);
}
