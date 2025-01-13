/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.e1c.edt.ai.AIModule;
import org.e1c.edt.ai.ICursorInfoProvider;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IProjectIdProvider;
import org.e1c.edt.ai.IVersionProvider;
import org.e1c.edt.ai.context.IModuleProvider;
import org.e1c.edt.ai.context.ModuleProvider;
import org.eclipse.jface.preference.IPreferenceStore;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Singleton;

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
        bind(IVersionProvider.class).toInstance(activator);
        bind(IPreferenceStore.class).toInstance(activator.getPreferenceStore());
        bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
        bind(IModuleProvider.class).annotatedWith(BaseModuleProvider.class).to(ModuleProvider.class).in(Singleton.class);
        bind(IModuleProvider.class).to(CurrentEditorModuleProvider.class);
        bind(IProjectIdProvider.class).to(ModuleProvider.class);
        // @formatter:on
    }


    @BindingAnnotation
    @Qualifier
    @Target({ FIELD, PARAMETER, METHOD })
    @Retention(RUNTIME)
    public @interface BaseModuleProvider
    {
        //
    }
}