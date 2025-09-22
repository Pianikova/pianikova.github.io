/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.quickaccess;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.quickaccess.QuickAccessElement;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ui.AITarget;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.BaseChatView;
import com.e1c.edt.ai.ui.IAIContextProvider;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IUI;
import com.e1c.edt.ai.ui.Images;
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
        var ctx = ui.getLastSourceViewer()
            .flatMap(sourceViewer -> aiContextProvider.create(sourceViewer,
                new AITarget(sourceViewer.getTextWidget(), false, true),
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
        ImageDescriptor image = BaseActivator.getImageDescriptor(Images.AI);
        return image;
    }
}