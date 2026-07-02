/**
 *
 */
package com.e1c.edt.ai.ui.handlers;

public class AddFilesToChatHandler
    extends BaseAddFilesToChatHandler
{
    @Override
    public void setEnabled(Object evaluationContext)
    {
        // TEMPORARILY DISABLED for the EDT plugin: the "Add file to chat" navigator command is
        // shown greyed out. Remove this override to restore the normal enablement logic.
        setBaseEnabled(false);
    }
}
