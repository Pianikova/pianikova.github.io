/**
 *
 */
package com.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class Notifications implements INotifications
{
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final IUINotificationService notificationService;

    @Inject
    public Notifications(IUI ui, IDispatcher dispatcher, IUINotificationService notificationService)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(notificationService);

        this.ui = ui;
        this.dispatcher = dispatcher;
        this.notificationService = notificationService;
    }

    @Override
    public boolean showMissingTokenInfo()
    {
        return createNotification(Messages.NotActivated, Messages.Activation, "https://code.1c.ai/", //$NON-NLS-1$
            UINotificationType.INFO);
    }

    @Override
    public boolean showTokenError()
    {
        return createNotification(Messages.StatusTokenFailed, Messages.Support,
            "https://code.1c.ai/troubleshooting/#issue_missing_token", UINotificationType.ERROR); //$NON-NLS-1$
    }

    @Override
    public boolean showSSLError()
    {
        return createNotification(Messages.StatusSSLFailed, Messages.Support,
            "https://code.1c.ai/troubleshooting/#issue_ssl_error", UINotificationType.ERROR); //$NON-NLS-1$
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
