/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.ILog;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class WebServer
    implements IWebServer
{
    private final ILog log;
    private final Provider<Handler> handlerProider;

    @Inject
    public WebServer(ILog log, Provider<Handler> handlerProider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(handlerProider);
        this.log = log;
        this.handlerProider = handlerProider;
    }

    @Override
    public AutoCloseable start(WebServerSettings settings)
    {
        var server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(settings.Port);
        server.setConnectors(new Connector[] { connector });
        {
            server.setHandler(handlerProider.get());
            try
            {
                server.start();
                log.trace("start", settings.toString()); //$NON-NLS-1$
            }
            catch (Exception e)
            {
                log.logError(e);
            }
        }

        return Closeables.create(() -> stop(settings, server));
    }

    private void stop(WebServerSettings settings, Server server)
    {
        try
        {
            server.stop();
            log.trace("stop", settings.toString()); //$NON-NLS-1$
        }
        catch (Exception e)
        {
            log.logError(e);
        }
    }
}