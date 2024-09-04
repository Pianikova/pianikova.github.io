/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.context.ContextModule;
import org.e1c.edt.ai.context.IResourceSetProvider;
import org.e1c.edt.ai.context.ResourceSetProvider;
import org.eclipse.jetty.server.Handler;

import com._1c.g5.wiring.AbstractServiceAwareModule;
import com.google.common.base.Preconditions;
import com.google.inject.Singleton;

public class SemanticModule
    extends AbstractServiceAwareModule
{
    private Activator activator;

    public SemanticModule(Activator activator)
    {
        super(activator);
        Preconditions.checkNotNull(activator);
        this.activator = activator;
    }

    @Override
    protected void doConfigure()
    {
        // @formatter:off
        install(new ContextModule());
        bind(ILog.class).toInstance(activator);
        bind(IWebServer.class).to(WebServer.class).in(Singleton.class);
        bind(Handler.class).to(WebHandler.class).in(Singleton.class);
        bind(IEndpointDialog.class).to(EndpointDialog.class).in(Singleton.class);
        bind(IResourceSetProvider.class).to(ResourceSetProvider.class).in(Singleton.class);
        // @formatter:on
    }
}
