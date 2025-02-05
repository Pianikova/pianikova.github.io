/**
 * Copyright (C) 2023, 1C-Soft LLC
 */
package org.e1c.edt.ai.ui;

import java.util.function.Supplier;

import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.IVersionProvider;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import com.google.inject.Injector;

/**
 * Данный класс представляет собой начальную точку в работе плагина.
 * В нем следует реализовывать логику создания плагина,
 * а так же необходимые действия при завершении работы плагина. <br>
 *
 * Так же данный класс содержит в себе ряд методов для удобного логирования ошибок
 */
public abstract class BaseActivator
    extends AbstractUIPlugin
    implements ILog, IVersionProvider
{
    /**
    * Путь к картинкам плагина
    */
    private static final String ICONS_PATH = "icons"; //$NON-NLS-1$
    private static BaseActivator plugin;
    private BundleContext bundleContext;
    private Injector injector;
    private IUISettings settings;

    /**
     * Получить экземпляр плагина. Через экземпляр плагина можно получать доступ к разнообразным механизмам Eclipse,
     * таким как:
     * <ul>
     * <li>Журнал логирования ошибок плагина</li>
     * <li>Механизм настройки свойств плагина</li>
     * <li>Механизм дескрипторов</li>
     * </ul>
     *
     * @return экземпляр плагина, никогда не должен возвращать <code>null</code>
     */
    public static BaseActivator getDefault()
    {
        return plugin;
    }

    public static void injectMembers(Object instance)
    {
        getDefault().getInjector().injectMembers(instance);
    }

    /**
    * Получение картинки по идентификатору
    *
    * @param id символьный идентификатор картинки
    * @return картинка или <b>null</b>, если для указанного идентификатора картинка
    * не обнаружена
    */

    public static Image getImage(String id)
    {
        return plugin.getImageRegistry().get(id);
    }

    /**
    * Получение дескриптора картинки по идентификатору
    *
    * @param id символьный идентификатор картинки
    * @return дескриптор картинки или <b>null</b>, если для указанного идентификатора картинка
    * не обнаружена
    */
    public static ImageDescriptor getImageDescriptor(String id)
    {
        return plugin.getImageRegistry().getDescriptor(id);
    }

    /**
    * Создает дескриптор картинки по символическому имени
    * Используется соглашение по формированию символических имен:
    * идентификатор плагина + относительный путь к картинке
    *
    * @param key символьный идентификатор картинки
    * @retrun созданный дескриптор картинки
    */
    public ImageDescriptor createImageDescriptorFromKey(String key)
    {
        String path = ICONS_PATH + key.substring(getPluginId().length());
        ImageDescriptor descriptor = ResourceLocator.imageDescriptorFromBundle(getPluginId(), path).get();
        return descriptor;
    }

    /**
     * Запись статуса события в лог журнал плагина.
     *
     * @param status статус события для логирования, не может быть <code>null</code>.
     * Данный статус содержит в себе информацию о произошедшем событии (ошибка выполнения,
     * разнообразные предупреждения), которые были зафиксированы в логике работы плагина.
     */
    private static void log(IStatus status)
    {
        if (plugin == null)
        {
            return;
        }

        var logger = plugin.getLog();
        if (logger == null)
        {
            return;
        }

        logger.log(status);
    }

    /**
     * Запись исключения в лог журнал плагина
     *
     * @param throwable выкинутое исключение, не может быть <code>null</code>
     */
    @Override
    public void logError(Throwable throwable)
    {
        if (throwable != null)
        {
            log(createErrorStatus(throwable.getMessage(), throwable));
        }
    }

    /**
     * Запись исключения в лог журнал плагина
     *
     * @param throwable выкинутое исключение, не может быть <code>null</code>
     */
    @Override
    public void logError(String error)
    {
        if (error != null && !error.isBlank())
        {
            log(createErrorStatus(error));
        }
    }

    /**
     * Запись сообщения трасировки в лог журнал плагина
     *
     * @param topic тема трасировки
     * @param traceMessage детали
     */
    @Override
    public void trace(String topic, Supplier<String> details)
    {
        if (settings == null || !settings.traceMode())
        {
            return;
        }

        var sb = new StringBuilder();
        sb.append(topic);
        sb.append(System.lineSeparator());
        sb.append(details.get());
        if (sb.length() > 10000)
        {
            sb.setLength(10000);
        }

        log(Status.info(sb.toString()));
    }

    /**
     * Создание записи с описанием ошибки в лог журнале плагина по выкинотому исключению и сообщению, его описывающим
     *
     * @param message описание выкинутого исключения, не может быть <code>null</code>
     * @param throwable выкинутое исключение, может быть <code>null</code>
     * @return созданное статус событие, не может быть <code>null</code>
     */
    private IStatus createErrorStatus(String message, Throwable throwable)
    {
        return new Status(IStatus.ERROR, getPluginId(), 0, message, throwable);
    }

    private IStatus createErrorStatus(String message)
    {
        return new Status(IStatus.ERROR, getPluginId(), 0, message, null);
    }

    /**
     * Данный метод является начальной точкой работы плагина
     *
     * @param bundleContext объект, создаваемый OSGi Framework, для доступа к разнообразным сервисам, например,
     * по работе с файловыми ресурсами внутри проекта
     */
    @Override
    public void start(BundleContext bundleContext) throws Exception
    {
        super.start(bundleContext);
        this.bundleContext = bundleContext;
        plugin = this;
        settings = getInjector().getInstance(IUISettings.class);
    }

    /**
     * Данный метод вызывается при завершении работы плагина
     *
     * @param bundleContext объект, создаваемый OSGi Framework, для доступа к разнообразным сервисам, например,
     * по работе с файловыми ресурсами внутри проекта
     */
    @Override
    public void stop(BundleContext bundleContext) throws Exception
    {
        var globalContextTracker = getInjector().getInstance(IGlobalContextTracker.class);
        if (globalContextTracker instanceof AutoCloseable)
        {
            ((AutoCloseable)globalContextTracker).close();
        }

        plugin = null;
        super.stop(bundleContext);
    }

    /**
     * Получить объект, создаваемый OSGi Framework, для доступа к разнообразным сервисам, например, по работе с
     * файловыми ресурсами внутри проекта
     *
     * @return объект, создаваемый OSGi Framework, для доступа к разнообразным сервисам, например, по работе с
     * файловыми ресурсами внутри проекта
     */
    protected BundleContext getContext()
    {
        return bundleContext;
    }

    /**
    * Инициализация реестра картинок плагина
    */
    /*@Override
    protected void initializeImageRegistry(ImageRegistry reg)
    {
        reg.put(IModelUIPluginImages.OBJS_AI_ICON,
            createImageDescriptorFromKey(IModelUIPluginImages.OBJS_AI_ICON));
    }*/

    private synchronized Injector getInjector()
    {
        if (injector == null)
        {
            try
            {
                injector = createInjector();
            }
            catch (Exception e)
            {
                log(createErrorStatus("Failed to create injector for " //$NON-NLS-1$
                    + getBundle().getSymbolicName(), e));
                throw new RuntimeException("Failed to create injector for " //$NON-NLS-1$
                    + getBundle().getSymbolicName(), e);
            }
        }

        return injector;
    }

    protected abstract Injector createInjector();

    @Override
    public Version getPluginVersion()
    {
        Bundle bundle = getDefault().getBundle();
        return bundle.getVersion();
    }

    @Override
    public String getPlatformVersion()
    {
        return System.getProperty("eclipse.buildId"); //$NON-NLS-1$
    }

    public abstract String getPluginId();
}
