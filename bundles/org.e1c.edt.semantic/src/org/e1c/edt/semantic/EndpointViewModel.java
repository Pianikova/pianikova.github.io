/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.ILog;
import org.eclipse.jface.preference.IPreferenceStore;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class EndpointViewModel implements IEndpointViewModel
{
    public final static String PORT_STORE_KEY = "EndpointPort"; //$NON-NLS-1$
    private final ILog log;
    private final IWebServer server;
    private final IPreferenceStore preferenceStore;
    private AutoCloseable serverStartToken = Closeables.Empty;
    private WebServerSettings settings = new WebServerSettings();

    @Inject
    public EndpointViewModel(ILog log, IWebServer server, IPreferenceStore preferenceStore)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(server);
        Preconditions.checkNotNull(preferenceStore);
        this.log = log;
        this.server = server;
        this.preferenceStore = preferenceStore;
    }

    @Override
    public boolean isActive()
    {
        return serverStartToken != Closeables.Empty;
    }

    @Override
    public void activate()
    {
        if (isActive())
        {
            return;
        }

        preferenceStore.setValue(PORT_STORE_KEY, getPort());
        serverStartToken = server.start(settings);
    }

    @Override
    public void deactivate()
    {
        if (!isActive())
        {
            return;
        }

        try
        {
            serverStartToken.close();
            preferenceStore.setValue(PORT_STORE_KEY, 0);
        }
        catch (Exception e)
        {
            // ignored
        }
        finally
        {
            serverStartToken = Closeables.Empty;
        }
    }

    @SuppressWarnings("nls")
    @Override
    public void restore()
    {
        try
        {
            var port = 0;
            try
            {
                var porpProperty = System.getProperties().getProperty("semantic.port", "");
                port = Integer.parseInt(porpProperty);
            }
            catch (Exception ex)
            {
                //
            }

            if (port == 0)
            {
                port = preferenceStore.getInt(PORT_STORE_KEY);
            }

            if (port > 0)
            {
                settings.Port = port;
                activate();
            }
        }
        catch (Exception ex)
        {
            log.logError(ex);
        }
    }

    @Override
    public int getPort()
    {
        return settings.Port;
    }

    @Override
    public void setPort(int port)
    {
        settings.Port = port;
    }
}
