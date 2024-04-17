/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.quickaccess;

import org.e1c.edt.ai.ui.Activator;
import org.e1c.edt.ai.ui.Composition;
import org.e1c.edt.ai.ui.IChat;
import org.e1c.edt.ai.ui.IModelUIPluginImages;
import org.e1c.edt.ai.ui.views.ChatView;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.quickaccess.QuickAccessElement;


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

    private final IChat chat;
    private String askText;

    public AskAIQuickAccessElement()
    {
        this(Messages.QuickAccessElementAskAI_0);
    }

    public AskAIQuickAccessElement(String input)
    {
        chat = Composition.getChat();
        askText = input;
    }

    @Override
    public void execute()
    {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try
        {
            ChatView view = (ChatView)page.showView(ChatView.ID);
            chat.askQuestion(askText);
            view.setFocus();
        }
        catch (PartInitException e)
        {
            Activator.createErrorStatus(e.getMessage(), e);
        }
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