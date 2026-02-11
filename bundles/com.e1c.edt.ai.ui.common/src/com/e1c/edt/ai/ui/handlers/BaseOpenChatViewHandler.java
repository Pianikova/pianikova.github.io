package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.BaseChatView;
import com.e1c.edt.ai.ui.IUI;
import com.google.inject.Inject;

public class BaseOpenChatViewHandler
    extends AbstractHandler
{
    @Inject
    IUI ui;

    @Inject
    ISettings settings;

    public BaseOpenChatViewHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean isEnabled()
    {
        return settings.isEnabled();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        ui.showView(BaseChatView.ID).ifPresent(view -> view.setFocus());
        return null;
    }

}
