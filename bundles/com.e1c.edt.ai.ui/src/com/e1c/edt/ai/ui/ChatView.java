/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.widgets.Composite;

public class ChatView
    extends BaseChatView
{
    @Override
    protected void createFooterControls(Composite parent)
    {
        // EDT-only: the Navigator drags 1C metadata objects via LocalSelectionTransfer, which the
        // JavaFX chat canvas cannot receive, so a dedicated SWT drop strip is added below the chat.
        createNavigatorDropZone(parent);
    }
}
