/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator class.
 * This class contains methods that provide convenient error logging and versions of platform and plugin.
 * @author Bogdan Sushkov
 *
 */
public class Activator
    extends Plugin
    implements BundleActivator
{
    public static final String PLUGIN_ID = "com.e1c.edt.ai"; //$NON-NLS-1$

    private static BundleContext bundleContext;
    private static Activator plugin;

    /**
     * Создание записи с описанием ошибки в лог журнале плагина по выкинотому исключению и сообщению, его описывающим
     *
     * @param message описание выкинутого исключения, не может быть <code>null</code>
     * @param throwable выкинутое исключение, может быть <code>null</code>
     * @return созданное статус событие, не может быть <code>null</code>
     */
    public static IStatus createErrorStatus(String message, Throwable throwable)
    {
        return new Status(IStatus.ERROR, PLUGIN_ID, 0, message, throwable);
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        setBundleContext(context);
        setDefault(this);
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        setDefault(null);
        setBundleContext(null);
    }

    /**
     * @return the bundleContext
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * @param bundleContext the bundleContext to set
     */
    public static void setBundleContext(BundleContext bundleContext)
    {
        Activator.bundleContext = bundleContext;
    }

    /**
     * @return the singleton, shouldn't be <code>null</code>
     */
    public static Activator getDefault()
    {
        return plugin;
    }

    /**
     * @param singleton the singleton to set
     */
    public static void setDefault(Activator plugin)
    {
        Activator.plugin = plugin;
    }
}
