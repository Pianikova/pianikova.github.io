/**
 *
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.ServiceState;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class Notifications implements INotifications
{
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final IUINotificationService notificationService;
    private final ISettings settings;
    private final ISettingsSetter settingsSetter;

    @Inject
    public Notifications(IUI ui, IDispatcher dispatcher, IUINotificationService notificationService,
        ISettings settings, ISettingsSetter settingsSetter)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(notificationService);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(settingsSetter);

        this.ui = ui;
        this.dispatcher = dispatcher;
        this.notificationService = notificationService;
        this.settings = settings;
        this.settingsSetter = settingsSetter;
    }

    @Override
    public boolean showMissingTokenInfo()
    {
        return createNotification(Messages.NotActivated, Messages.Activation, settings.getHomePage(),
            UINotificationType.INFO, () -> settingsSetter.setActivationInfoVisible(false));
    }

    @Override
    public boolean showTokenError()
    {
        return createNotification(com.e1c.edt.ai.Messages.StatusTokenFailed, Messages.Support,
            settings.getHomePage() + ServiceState.TOKEN_ERROR.getUrlPath(), UINotificationType.ERROR);
    }

    @Override
    public boolean showSSLError()
    {
        return createNotification(com.e1c.edt.ai.Messages.StatusSSLFailed, Messages.Support,
            settings.getHomePage() + ServiceState.SSL_ERROR.getUrlPath(), UINotificationType.ERROR);
    }

    private boolean createNotification(String title, String buttonText, String url, UINotificationType type)
    {
        return createNotification(title, buttonText, url, type, null);
    }

    private boolean createNotification(String title, String buttonText, String url, UINotificationType type,
        Runnable dontShowAgainAction)
    {
        return dispatcher.dispatch(() -> {
            var shell = ui.getShell().orElse(null);
            if (shell != null)
            {
                notificationService.closeNotificationIfOpen();
                notificationService.createNotification(shell, title, buttonText, url, type, dontShowAgainAction);
                return true;
            }

            return false;
        }).orElse(false);
    }
}
