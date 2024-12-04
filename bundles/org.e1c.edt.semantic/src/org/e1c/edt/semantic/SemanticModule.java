/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IProgramingLanguage;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.Json;
import org.e1c.edt.ai.context.IModuleProvider;
import org.e1c.edt.ai.context.ModuleProvider;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jface.preference.IPreferenceStore;

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
        bind(ILog.class).toInstance(activator);
        bind(IPreferenceStore.class).toInstance(activator.getPreferenceStore());
        bind(IWebServer.class).to(WebServer.class).in(Singleton.class);
        bind(Handler.class).to(WebHandler.class).in(Singleton.class);
        bind(IEndpointDialog.class).to(EndpointDialog.class).in(Singleton.class);
        bind(IModuleProvider.class).to(ModuleProvider.class).in(Singleton.class);
        bind(IJson.class).to(Json.class).in(Singleton.class);
        bind(IProgramingLanguage.class).to(ProgramingLanguage.class).in(Singleton.class);
        bind(IUISettings.class).to(UISettings.class).in(Singleton.class);
        bind(IEndpointViewModel.class).to(EndpointViewModel.class).in(Singleton.class);
        bind(IIDE.class).to(IDE.class).in(Singleton.class);
        // @formatter:on
    }
}
