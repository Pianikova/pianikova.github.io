/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * A class-holder view to dialog with the AI.
 *
 * @author Bogdan Sushkov
 */
public class ChatView extends ViewPart {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "org.e1c.edt.ai.ui.views.ChatView"; //$NON-NLS-1$

    /**
     *   Заглушка - поисковая система гугл.
     *   В дальнейшем исправить на url сервиса
     */
    private String message = ""; //$NON-NLS-1$
    private String url = "https://google.com"; //$NON-NLS-1$
    private Browser browser;

    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new GridLayout());

        browser = new Browser(parent, SWT.NONE);
        browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }
    @Override
    public void setFocus()
    {
        String request = url + "/search?q=" + message.replace(' ', '+'); //$NON-NLS-1$
        browser.setUrl(request);
    }

    public void setMessage(String input)
    {
        message = input;
    }
}
