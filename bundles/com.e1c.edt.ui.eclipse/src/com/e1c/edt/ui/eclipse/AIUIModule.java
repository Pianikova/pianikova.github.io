/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ui.eclipse;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.jface.preference.IPreferenceStore;

import com.e1c.edt.ai.AIModule;
import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.ICodeProvider;
import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.IContextEntities;
import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.IFiles;
import com.e1c.edt.ai.IGlobalContextManager;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectBuilder;
import com.e1c.edt.ai.IProjectDetailsProvider;
import com.e1c.edt.ai.IProjectParametersProvider;
import com.e1c.edt.ai.IProjectProvider;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.IVisualContextProvider;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.ICodeParser;
import com.e1c.edt.ai.ui.IModuleNameProvider;
import com.e1c.edt.ai.ui.ISpecializedEditorOpener;
import com.e1c.edt.ai.ui.NoOpSpecializedEditorOpener;
import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

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
        bind(ICodePartsProvider.class).to(CodePartsProvider.class).in(Singleton.class);
        bind(ICodeParser.class).to(CodeParser.class).in(Singleton.class);
        bind(ICodeProvider.class).to(CodeProvider.class).in(Singleton.class);
        bind(IContextEntities.class).to(ContextEntities.class).in(Singleton.class);
        bind(IProjectProvider.class).to(ProjectProvider.class).in(Singleton.class);
        bind(IProjectBuilder.class).to(ProjectBuilder.class).in(Singleton.class);
        bind(IEditingSupport.class).to(EditingSupport.class).in(Singleton.class);
        bind(MessageDigest.class).toProvider(() -> {
            try
            {
                return MessageDigest.getInstance("MD5");//$NON-NLS-1$
            }
            catch (NoSuchAlgorithmException e)
            {
                return null;
            }
        });
        bind(IGlobalContextManager.class).to(GlobalContextManager.class).in(Singleton.class);
        bind(IModuleNameProvider.class).to(ModuleNameProvider.class).in(Singleton.class);
        bind(IConfigurationParametersProvider.class).to(ConfigurationParametersProvider.class).in(Singleton.class);
        bind(IProjectParametersProvider.class).to(ConfigurationParametersProvider.class).in(Singleton.class);
        bind(IVisualContextProvider.class).to(com.e1c.edt.ai.ui.SwtVisualContextProvider.class).in(Singleton.class);
        bind(IFiles.class).to(Files.class).in(Singleton.class);
        bind(ISpecializedEditorOpener.class).to(NoOpSpecializedEditorOpener.class).in(Singleton.class);
        @SuppressWarnings("unused")
        var projectDetailsProviderBinder = Multibinder.newSetBinder(binder(), IProjectDetailsProvider.class);
        // @formatter:on
    }
}
