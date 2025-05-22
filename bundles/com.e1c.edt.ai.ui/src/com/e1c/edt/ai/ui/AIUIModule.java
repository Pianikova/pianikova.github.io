/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.xtext.builder.IXtextBuilderParticipant;

import com.e1c.edt.ai.AIModule;
import com.e1c.edt.ai.ICursorInfoProvider;
import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.IProjectProvider;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.context.IModuleProvider;
import com.e1c.edt.ai.context.ModuleProvider;
import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class AIUIModule
    extends AbstractModule
{
    public static final String PARAMETERS = "Parameters"; //$NON-NLS-1$
    public static final String URL = "URL"; //$NON-NLS-1$

    private BaseActivator activator;

    public AIUIModule(BaseActivator activator)
    {
        Preconditions.checkNotNull(activator);
        this.activator = activator;
    }

    @Override
    protected void configure()
    {
        // @formatter:off
        install(new AIModule());
        bind(ILog.class).toInstance(activator);
        bind(IDefaultSettings.class).to(DefaultSettings.class).in(Singleton.class);
        bind(IVersionProvider.class).toInstance(activator);
        bind(IPreferenceStore.class).toInstance(activator.getPreferenceStore());
        bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
        bind(IModuleProvider.class).annotatedWith(Names.named("BaseModuleProvider")).to(ModuleProvider.class).in(Singleton.class); //$NON-NLS-1$
        bind(IModuleProvider.class).to(CurrentEditorModuleProvider.class);
        bind(IProjectIdProvider.class).to(ModuleProvider.class);
        bind(IProjectProvider.class).to(ModuleProvider.class);
        bind(IXtextBuilderParticipant.class).to(BuildTrackingParticipant.class).in(Singleton.class);
        bind(ICodeParser.class).to(CodeParser.class).in(Singleton.class);
        // @formatter:on
    }
}