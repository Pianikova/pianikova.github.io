/**
 * Copyright (C) 2025, 1C-Soft LLC
 */
package com.e1c.edt.ai.ui;

import java.util.Hashtable;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.assistent.AIClientException;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.google.inject.Injector;


/**
 * Данный кла
 * сс представляет собой начальную точку в работе плагина.
 * В нем следует реализовывать логику создания плагина,
 * а так же необходимые действия при завершении работы плагина. <br>
 *
 * Так же данный класс содержит в себе ряд методов для удобного логирования ошибок
 */
public abstract class BaseActivator
    extends AbstractUIPlugin
    implements ILog, IVersionProvider, DebugOptionsListener
{
    private static final String TRACE_SOURCE_PREFIX = "com.e1c.edt.ai/"; //$NON-NLS-1$
    private static final String ICONS_PATH = "icons"; //$NON-NLS-1$
    private static BaseActivator plugin;
    private BundleContext bundleContext;
    private Injector injector;
    private ISettings settings;
    private DebugTrace debugTrace;

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
            if (throwable instanceof CompletionException)
            {
                var completionException = (CompletionException)throwable;
                throwable = completionException.getCause();
            }

            if (throwable instanceof AIClientException)
            {
                log(Status.info(throwable.getMessage()));
                return;
            }

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
        if (error == null || error.isBlank())
        {
            return;
        }

        log(createErrorStatus(error));
    }

    @Override
    public void warning(String topic, Supplier<String> details)
    {
        var messsage = createMesssage(topic, details, Verbosity.WARNING);
        if (messsage != null && messsage.length() > 0)
        {
            log(Status.warning(messsage.toString()));
        }
    }

    @Override
    public void optionsChanged(DebugOptions options)
    {
        debugTrace = options.newDebugTrace(TRACE_SOURCE_PREFIX);
    }

    @Override
    public void trace(String tracingSource, String topic, Supplier<String> details)
    {
        if (!isTracingEnabled(tracingSource))
        {
            return;
        }

        var trace = debugTrace;
        if (trace != null)
        {
            var messsage = createMesssage(topic, details, Verbosity.ERROR);
            if (messsage != null && messsage.length() > 0)
            {
                trace.trace(tracingSource, messsage.toString());
            }
        }
    }

    @Override
    public boolean isTracingEnabled(String tracingSource)
    {
        if (debugTrace == null)
        {
            return false;
        }

        return Platform.isRunning()
            && "true".equalsIgnoreCase(Platform.getDebugOption(TRACE_SOURCE_PREFIX + tracingSource)); //$NON-NLS-1$
    }

    private StringBuilder createMesssage(String topic, Supplier<String> details, Verbosity verbosity)
    {
        if (settings == null || settings.getVerbosity().getLevel() < verbosity.getLevel())
        {
            return null;
        }

        var message = new StringBuilder();
        message.append(topic);
        message.append(System.lineSeparator());
        message.append(details.get());
        return message;
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
        return new Status(IStatus.ERROR, TRACE_SOURCE_PREFIX, 0, message, throwable);
    }

    private IStatus createErrorStatus(String message)
    {
        return new Status(IStatus.ERROR, TRACE_SOURCE_PREFIX, 0, message, null);
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
        var props = new Hashtable<String, String>();
        props.put(DebugOptions.LISTENER_SYMBOLICNAME, TRACE_SOURCE_PREFIX);
        bundleContext.registerService(DebugOptionsListener.class, this, props);
        plugin = this;
        javafx.application.Platform.setImplicitExit(false);
        settings = getInjector().getInstance(ISettings.class);
        Display.getDefault().disposeExec(() -> {
            CancellationTokens.isStopped = true;
        });
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
        var defaultActivator = getDefault();
        if (defaultActivator.injector == null)
        {
            try
            {
                defaultActivator.injector = createInjector();
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

    @SuppressWarnings("nls")
    @Override
    protected void initializeImageRegistry(ImageRegistry registry)
    {
        registry.put(Images.AI, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/ai.png"));
        registry.put(Images.INFO, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/info.png"));
        registry.put(Images.WARNING, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/warning.png"));
        registry.put(Images.ERROR, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/error.png"));
        registry.put(Images.OFFLINE, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/status_offline.png"));
        registry.put(Images.ONLINE, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/status_online.png"));
        registry.put(Images.OFF, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/status_off.png"));
        registry.put(Images.BUSY, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/status_busy.png"));
        registry.put(Images.GIT_MESSAGE, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/gitmessage.png"));
        registry.put(Images.GIT_REVIEW, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/gitreview.png"));
        registry.put(Images.SUGGEST_YOUR_OPTION,
            imageDescriptorFromPlugin(getPluginId(), "icons/obj16/suggest_your_option.png"));
        registry.put(Images.CORRECT_ERRORS, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/correct_errors.png"));
        registry.put(Images.IN_OTHER_WORDS, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/in_other_words.png"));
        registry.put(Images.IMPROVE_STYLE, imageDescriptorFromPlugin(getPluginId(), "icons/obj16/improve_style.png"));
    }

    public abstract String getPluginId();
}
