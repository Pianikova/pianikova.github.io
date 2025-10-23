package com.e1c.edt.semantic;

import java.util.Hashtable;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.context.ContextModuleFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator
    extends AbstractUIPlugin
    implements ILog, DebugOptionsListener
{
    private static final String TRACE_SOURCE_PREFIX = "com.e1c.edt.ai/"; //$NON-NLS-1$
    private Injector injector;
    private DebugTrace debugTrace;

	// The plug-in ID
	public static final String PLUGIN_ID = "com.e1c.edt.semantic"; //$NON-NLS-1$

	private static Activator plugin;

	public Activator() {
	}

    public static void injectMembers(Object instance)
    {
        getDefault().getInjector().injectMembers(instance);
    }

	@Override
    public void start(BundleContext bundleContext) throws Exception
    {
        super.start(bundleContext);
        var props = new Hashtable<String, String>();
        props.put(DebugOptions.LISTENER_SYMBOLICNAME, TRACE_SOURCE_PREFIX);
        bundleContext.registerService(DebugOptionsListener.class, this, props);
		plugin = this;
	}

	@Override
    public void stop(BundleContext bundleContext) throws Exception
    {
        plugin = null;
        super.stop(bundleContext);
	}

    private static void log(IStatus status)
    {
        plugin.getLog().log(status);
    }

    @Override
    public void logError(Throwable throwable)
    {
        if (throwable != null)
        {
            log(createErrorStatus(throwable.getMessage(), throwable));
        }
    }

    @Override
    public void logError(String error)
    {
        if (error != null && !error.isBlank())
        {
            log(createErrorStatus(error));
        }
    }

    @Override
    public void warning(String topic, Supplier<String> details)
    {
        if (topic == null || topic.isBlank())
        {
            topic = ""; //$NON-NLS-1$
        }

        var sb = new StringBuilder();
        sb.append("Semantic server "); //$NON-NLS-1$
        sb.append(topic);
        sb.append(System.lineSeparator());
        sb.append(details.get());
        log(Status.warning(sb.toString()));
    }

    @Override
    public void optionsChanged(DebugOptions options)
    {
        debugTrace = options.newDebugTrace(TRACE_SOURCE_PREFIX);
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
            var messsage = createMesssage(topic, details);
            if (messsage != null && messsage.length() > 0)
            {
                trace.trace(tracingSource, messsage.toString());
            }
        }
    }

    private StringBuilder createMesssage(String topic, Supplier<String> details)
    {
        if (topic == null || topic.isBlank())
        {
            topic = ""; //$NON-NLS-1$
        }

        var message = new StringBuilder();
        message.append("Semantic server "); //$NON-NLS-1$
        message.append(topic);
        message.append(System.lineSeparator());
        message.append(details.get());
        return message;
    }

    private static IStatus createErrorStatus(String message, Throwable throwable)
    {
        return new Status(IStatus.ERROR, PLUGIN_ID, 0, message, throwable);
    }

    private static IStatus createErrorStatus(String message)
    {
        return new Status(IStatus.ERROR, PLUGIN_ID, 0, message, null);
    }

	public static Activator getDefault() {
		return plugin;
	}

    private synchronized Injector getInjector()
    {
        if (injector == null)
        {
            try
            {
                var mergedModule =
                    ContextModuleFactory.create(this).with(new SemanticModule(this));
                injector = Guice.createInjector(mergedModule);
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
}
