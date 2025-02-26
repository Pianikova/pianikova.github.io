/*
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui.quickaccess;

import org.e1c.edt.ai.CancellationTokens;
import org.e1c.edt.ai.ui.AITarget;
import org.e1c.edt.ai.ui.BaseActivator;
import org.e1c.edt.ai.ui.BaseChatView;
import org.e1c.edt.ai.ui.IAIContextProvider;
import org.e1c.edt.ai.ui.IChat;
import org.e1c.edt.ai.ui.IUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.quickaccess.QuickAccessElement;

import com.google.inject.Inject;


/**
 * "Ask the AI" element created in the QuickAccess menu.
 * The default constructor of the class sets the input text as the default.
 * If the user starts typing a message, the input text will be copied and
 * provided to the element's label.
 *
 *
 * @author Bogdan Sushkov
 *
 */
public class AskAIQuickAccessElement
    extends QuickAccessElement
{
    // public static final String ID = BaseActivator.PLUGIN_ID + ".MyQuickAccessElement"; //$NON-NLS-1$

    @Inject
    IAIContextProvider aiContextProvider;
    @Inject
    IChat chat;
    @Inject
    IUI ui;
    private String askText;


    public AskAIQuickAccessElement()
    {
        this(Messages.QuickAccessElementAskAI_0);
    }

    public AskAIQuickAccessElement(String input)
    {
        BaseActivator.injectMembers(this);
        askText = input;
    }

    @Override
    public void execute()
    {
        var ctx = ui.getTextWidget()
            .flatMap(textWidget -> aiContextProvider.create(new AITarget(textWidget, Integer.MAX_VALUE, true),
                CancellationTokens.NONE))
            .orElse(null);

        ui.showView(BaseChatView.ID).ifPresent(view -> {
            chat.askQuestion(ctx, askText);
            view.setFocus();
        });
    }

    @Override
    public String getId()
    {
        return askText;
    }

    @Override
    public String getLabel()
    {
        return Messages.QuickAccessElementAskAI_1 + askText;
    }

    @Override
    public ImageDescriptor getImageDescriptor()
    {
        ImageDescriptor image =
            BaseActivator.getImageDescriptor(BaseActivator.getDefault().getPluginId() + "/obj16/ai.png"); //$NON-NLS-1$
        return image;
    }
}