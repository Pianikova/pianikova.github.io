package org.e1c.edt.semantic;

import java.util.function.Supplier;

import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.context.ContextModuleFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator
    extends AbstractUIPlugin
    implements ILog
{
    private Injector injector;

	// The plug-in ID
	public static final String PLUGIN_ID = "org.e1c.edt.semantic"; //$NON-NLS-1$

	private static Activator plugin;

	public Activator() {
	}

    public static void injectMembers(Object instance)
    {
        getDefault().getInjector().injectMembers(instance);
    }

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
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
    public void trace(String topic, Supplier<String> details)
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
        log(Status.info(sb.toString()));
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
