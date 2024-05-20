/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.quickaccess;

import org.e1c.edt.ai.ui.Activator;
import org.e1c.edt.ai.ui.ChatView;
import org.e1c.edt.ai.ui.IChat;
import org.e1c.edt.ai.ui.IModelUIPluginImages;
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
    public static final String ID = Activator.PLUGIN_ID + ".MyQuickAccessElement"; //$NON-NLS-1$

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
        Activator.injectMembers(this);
        askText = input;
    }

    @Override
    public void execute()
    {
        ui.showView(ChatView.ID).ifPresent(view -> {
            chat.askQuestion(askText);
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
        ImageDescriptor image = Activator.getImageDescriptor(IModelUIPluginImages.OBJS_AI_ICON);
        return image;
    }
}