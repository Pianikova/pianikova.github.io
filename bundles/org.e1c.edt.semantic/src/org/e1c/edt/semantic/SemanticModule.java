/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.Json;
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
        // bind(IQualifiedNameProvider.class).toService();
        bind(ILog.class).toInstance(activator);
        bind(IJson.class).to(Json.class).in(Singleton.class);
        bind(IWebServer.class).to(WebServer.class).in(Singleton.class);
        bind(Handler.class).to(WebHandler.class).in(Singleton.class);
        bind(IEntitiesWalker.class).to(EntitiesWalker.class).in(Singleton.class);
        bind(IRelatedEntities.class).to(RelatedEntities.class).in(Singleton.class);
        bind(IEntityInfo.class).to(EntityInfo.class).in(Singleton.class);
        bind(IV8Model.class).to(V8Model.class).in(Singleton.class);
        bind(IIdFactory.class).to(IdFactory.class).in(Singleton.class);
        bind(IEndpointDialog.class).to(EndpointDialog.class).in(Singleton.class);
        // @formatter:on
    }
}
