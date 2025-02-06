/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ui.eclipse;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.inject.Qualifier;

import org.e1c.edt.ai.AIModule;
import org.e1c.edt.ai.ICodePartsProvider;
import org.e1c.edt.ai.ICodeProvider;
import org.e1c.edt.ai.IContextEntities;
import org.e1c.edt.ai.IGlobalContextManager;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IProjectIdProvider;
import org.e1c.edt.ai.IProjectProvider;
import org.e1c.edt.ai.IVersionProvider;
import org.e1c.edt.ai.ui.BaseActivator;
import org.e1c.edt.ai.ui.ICodeParser;
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
        bind(ICodePartsProvider.class).to(CodePartsProvider.class).in(Singleton.class);
        bind(ICodeParser.class).to(CodeParser.class).in(Singleton.class);
        bind(ICodeProvider.class).to(CodeProvider.class).in(Singleton.class);
        bind(IContextEntities.class).to(ContextEntities.class).in(Singleton.class);
        bind(IProjectProvider.class).to(ProjectProvider.class).in(Singleton.class);
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
        bind(IProjectIdProvider.class).to(ProjectIdProvider.class).in(Singleton.class);
        bind(IGlobalContextManager.class).to(GlobalContextManager.class).in(Singleton.class);
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