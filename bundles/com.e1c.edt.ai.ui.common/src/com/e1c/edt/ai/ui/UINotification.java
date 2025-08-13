/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.net.URL;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.e1c.edt.ai.ILog;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class UINotification
    extends PopupDialog
{

    @Inject
    private static ILog log;
    private final String message;
    private final String linkText;
    private final String url;
    private final UINotificationType type;
    private boolean isMouseOver = false;
    private Runnable timerRunnable;
    private Runnable action;

    public UINotification(Shell parentShell, String message, UINotificationType type, String linkText,
        String url)
    {
        super(parentShell, SWT.NO_TRIM | SWT.ON_TOP, false, false, false, false, false, null, null);
        this.message = message;
        this.type = type;
        this.linkText = linkText;
        this.url = url;
    }

    public UINotification(Shell parentShell, String message, UINotificationType type, String linkText,
        String url,
        Runnable action)
    {
        this(parentShell, message, type, linkText, url);
        this.action = action;
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
                    try
                    {
                        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
            });
            GridData linkData = new GridData(SWT.FILL, SWT.TOP, true, false);
            linkData.heightHint = linkLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
            linkLabel.setLayoutData(linkData);
        }

        Composite buttonContainer = new Composite(textContainer, SWT.NONE);
        GridLayout buttonLayout = new GridLayout(1, false);
        buttonLayout.marginWidth = 0;
        buttonContainer.setLayout(buttonLayout);
        GridData buttonContainerData = new GridData(SWT.RIGHT, SWT.TOP, true, false);
        buttonContainerData.horizontalSpan = 2;
        buttonContainer.setLayoutData(buttonContainerData);
        buttonContainer.setBackground(bg);
        buttonContainer.setForeground(fg);

        if (action != null)
        {
            Button actionButton = new Button(buttonContainer, SWT.PUSH);
            actionButton.setText(Messages.UpdateButton);
            actionButton.setBackground(bg);
            actionButton.setForeground(fg);
            actionButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
            actionButton.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    action.run();
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

        canvas.addMouseTrackListener(new MouseTrackAdapter()
        {
            @Override
            public void mouseEnter(MouseEvent e)
            {
                isMouseOver = true;
                if (getShell() != null && timerRunnable != null)
                {
                    Display.getDefault().timerExec(-1, timerRunnable);
                }
            }

            @Override
            public void mouseExit(MouseEvent e)
            {
                isMouseOver = false;
                startCloseTimer();
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

    private void startCloseTimer()
    {
        timerRunnable = () -> {
            if (!isMouseOver && getShell() != null && !getShell().isDisposed())
            {
                close();
            }
        };
        if (getShell() != null)
        {
            Display.getDefault().timerExec(5000, timerRunnable);
        }
    }

    @Override
    public int open()
    {
        int result = super.open();
        startCloseTimer();
        return result;
    }
}
