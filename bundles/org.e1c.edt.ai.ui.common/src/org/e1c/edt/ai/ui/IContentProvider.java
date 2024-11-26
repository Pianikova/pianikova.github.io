/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.swt.custom.StyledText;

public interface IContentProvider
{
    Content get(StyledText textWidget);
}
