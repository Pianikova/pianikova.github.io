/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import com.e1c.edt.ai.ui.UINotificationService.UINotificationActionType;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class UINotification
    extends PopupDialog
{
    @Inject
    private IDispatcher dispatcher;

    @Inject
    private IWeb web;

    private final String message;
    private final String linkText;
    private final String url;
    private final UINotificationType type;
    private Runnable action;
    private UINotificationActionType actionType;
    private Runnable dontShowAgainAction;

    public UINotification(Shell parentShell, String message, UINotificationType type, String linkText,
        String url)
    {
        super(parentShell, SWT.NO_TRIM | SWT.ON_TOP, false, false, false, false, false, null, null);
        BaseActivator.injectMembers(this);
        this.message = message;
        this.type = type;
        this.linkText = linkText;
        this.url = url;
    }

    public UINotification(Shell parentShell, String message, UINotificationType type, String linkText,
        String url, Runnable dontShowAgainAction)
    {
        this(parentShell, message, type, linkText, url);
        this.dontShowAgainAction = dontShowAgainAction;
    }

    public UINotification(Shell parentShell, String message, UINotificationType type, String linkText,
        String url, Runnable action, UINotificationActionType actionType)
    {
        this(parentShell, message, type, linkText, url);
        this.action = action;
        this.actionType = actionType;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Color bg = parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
        Color fg = parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND);

        parent.setBackground(bg);
        parent.setForeground(fg);
        parent.setBackgroundMode(SWT.INHERIT_FORCE);

        Composite canvas = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 10;
        layout.marginHeight = 10;
        layout.verticalSpacing = 5;
        canvas.setLayout(layout);

        Label iconLabel = new Label(canvas, SWT.NONE);
        iconLabel.setImage(BaseActivator.getImage(type.getImageId()));
        iconLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
        iconLabel.setBackground(bg);

        Composite textContainer = new Composite(canvas, SWT.NONE);
        textContainer.setBackground(bg);
        GridLayout textLayout = new GridLayout(1, false);
        textLayout.marginWidth = 0;
        textLayout.marginHeight = 0;
        textContainer.setLayout(textLayout);
        textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        Label textLabel = new Label(textContainer, SWT.SINGLE);
        textLabel.setText(message);
        textLabel.setBackground(bg);
        textLabel.setForeground(fg);
        GridData textData = new GridData(SWT.FILL, SWT.TOP, true, false);
        textData.heightHint = textLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        textLabel.setLayoutData(textData);

        // Ссылка
        if (linkText != null && !linkText.isEmpty())
        {
            Link linkLabel = new Link(textContainer, SWT.SINGLE);
            linkLabel.setText("<a href=\"" + url + "\">" + linkText + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            linkLabel.setBackground(bg);
            linkLabel.setForeground(fg);
            linkLabel.setToolTipText(url);
            linkLabel.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    web.browse(url);
                }
            });
            GridData linkData = new GridData(SWT.FILL, SWT.TOP, true, false);
            linkData.heightHint = linkLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
            linkLabel.setLayoutData(linkData);
        }

        Composite buttonContainer = new Composite(textContainer, SWT.NONE);
        GridLayout buttonLayout = new GridLayout(3, false);
        buttonLayout.marginWidth = 0;
        buttonContainer.setLayout(buttonLayout);
        GridData buttonContainerData = new GridData(SWT.RIGHT, SWT.TOP, true, false);
        buttonContainerData.horizontalSpan = 2;
        buttonContainer.setLayoutData(buttonContainerData);
        buttonContainer.setBackground(bg);
        buttonContainer.setForeground(fg);

        if (dontShowAgainAction != null)
        {
            Link dontShowAgainLink = new Link(buttonContainer, SWT.NONE);
            dontShowAgainLink.setText("<a>" + Messages.DontShowAgain + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
            dontShowAgainLink.setBackground(bg);
            dontShowAgainLink.setForeground(fg);
            dontShowAgainLink.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
            dontShowAgainLink.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    dispatcher.dispatchAsync(dontShowAgainAction);
                    close();
                }
            });
        }

        if (action != null)
        {
            Button actionButton = new Button(buttonContainer, SWT.PUSH);
            actionButton.setText(actionType.getActionText());
            actionButton.setBackground(bg);
            actionButton.setForeground(fg);
            actionButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
            actionButton.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    dispatcher.dispatchAsync(action);
                    close();
                }
            });
        }

        Button closeButton = new Button(buttonContainer, SWT.PUSH);
        closeButton.setText(Messages.CloseButton);
        closeButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
        closeButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                close();
            }
        });

        return canvas;
    }

    @Override
    protected void initializeBounds()
    {
        super.initializeBounds();

        int heightDelta = 20;
        int margin = 20;
        int minWidth = 150;
        int minHeight = 80;

        Shell shell = getShell();
        Point preferred = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        Rectangle ideBounds = getParentShell().getMonitor().getClientArea();

        int ideWidth = ideBounds.width;
        int ideHeight = ideBounds.height;
        int targetWidth = Math.max(preferred.x + heightDelta, minWidth);
        if (targetWidth > ideWidth - 2 * margin)
        {
            targetWidth = ideWidth - 2 * margin;
        }

        Point recomputed = shell.computeSize(targetWidth, SWT.DEFAULT);

        int targetHeight = Math.max(recomputed.y, minHeight);
        if (targetHeight > ideHeight - 2 * margin)
        {
            targetHeight = ideHeight - 2 * margin;
        }

        shell.setSize(targetWidth, targetHeight);

        int x = ideBounds.x + ideWidth - targetWidth - margin;
        int y = ideBounds.y + ideHeight - targetHeight - margin;

        int minX = ideBounds.x + margin;
        int maxX = ideBounds.x + ideWidth - targetWidth - margin;
        x = Math.min(Math.max(x, minX), maxX);

        int minY = ideBounds.y + margin;
        int maxY = ideBounds.y + ideHeight - targetHeight - margin;
        y = Math.min(Math.max(y, minY), maxY);

        shell.setLocation(x, y);
    }

    @Override
    public int open()
    {
        int result = super.open();
        return result;
    }
}
