/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.ui.dialogs.PreferencesUtil;

import com.e1c.edt.ai.ILog;
import com.google.inject.Inject;

/**
 * Default implementation of {@link IPreferences} that uses Eclipse's PreferencesUtil.
 *
 * @author Bogdan Sushkov
 */
public class Preferences
    implements IPreferences
{
    private final ILog log;

    @Inject
    public Preferences(ILog log)
    {
        this.log = log;
    }

    @Override
    public void show(String pageId)
    {
        try
        {
            var shell = org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            if (shell != null && !shell.isDisposed())
            {
                var dialog = PreferencesUtil.createPreferenceDialogOn(shell, pageId, null, null);
                if (dialog != null)
                {
                    dialog.open();
                }
            }
        }
        catch (Exception e)
        {
            log.logError(e);
        }
    }
}
