/**
 *
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ServiceState;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class Notifications implements INotifications
{
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final IUINotificationService notificationService;
    private final ISettings settings;

    @Inject
    public Notifications(IUI ui, IDispatcher dispatcher, IUINotificationService notificationService, ISettings settings)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(notificationService);
        Preconditions.checkNotNull(settings);

        this.ui = ui;
        this.dispatcher = dispatcher;
        this.notificationService = notificationService;
        this.settings = settings;
    }

    @Override
    public boolean showMissingTokenInfo()
    {
        return createNotification(Messages.NotActivated, Messages.Activation, settings.getHomePage(),
            UINotificationType.INFO);
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
        return dispatcher.dispatch(() -> {
            var shell = ui.getShell().orElse(null);
            if (shell != null)
            {
                notificationService.closeNotificationIfOpen();
                notificationService.createNotification(shell, title, buttonText, url, type);
                return true;
            }

            return false;
        }).orElse(false);
    }
}
