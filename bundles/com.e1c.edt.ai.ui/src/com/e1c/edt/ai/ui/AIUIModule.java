/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.xtext.builder.IXtextBuilderParticipant;

import com.e1c.edt.ai.AIModule;
import com.e1c.edt.ai.ICursorInfoProvider;
import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.IEditRollback;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.IProjectProvider;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.context.IModuleProvider;
import com.e1c.edt.ai.context.ModuleProvider;
import com.e1c.edt.ai.tools.EditRollback;
import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
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
        bind(IModuleNameProvider.class).to(ModuleNameProvider.class).in(Singleton.class);
        bind(ISpecializedEditorOpener.class).to(EdtSpecializedEditorOpener.class).in(Singleton.class);
        bind(IEditRollback.class).to(EditRollback.class).in(Singleton.class);

        // Global-context tracking is EDT-only: scans + hashes every workspace file and syncs it
        // to the server. The plain-Eclipse plugin must NOT do this, so these initializables live
        // here rather than in the shared AIUICommonModule. (Multibinder contributions are additive
        // across modules, so they join the IInitializable set declared in AIUICommonModule.)
        var initializableBinder = Multibinder.newSetBinder(binder(), IInitializable.class);
        initializableBinder.addBinding().to(ActiveProjectTracker.class);
        initializableBinder.addBinding().to(ResourceListener.class);
        bind(ActiveProjectTracker.class).in(Singleton.class);
        bind(ResourceListener.class).in(Singleton.class);
        // @formatter:on
    }
}