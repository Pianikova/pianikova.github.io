/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.ui.hover;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collection;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import com.e1c.edt.ai.ui.handlers.AIMarkerResolutionGenerator;

/**
 * Contributes AI-marker quick-fix buttons to the BSL annotation hover toolbar, so the fix is
 * reachable directly from the marker icon on the editor's vertical ruler.
 * <p>
 * The BSL hover contributor SPI ({@code com._1c.g5.v8.dt.bsl.ui.hover.IBslHoverContributor}) exists
 * only in newer 1C:EDT runtimes, but this plugin is compiled against an older target platform that
 * lacks it. To stay buildable while still working on a newer runtime, this factory is registered
 * for the {@code com._1c.g5.v8.dt.bsl.ui.bslHoverContributor} extension point and returns a
 * {@link Proxy} that implements the SPI interface, resolved reflectively at runtime.
 * <p>
 * On an older runtime the extension point is simply absent, so this factory is never invoked — a
 * clean no-op with no error.
 */
public class AIMarkerHoverContributorFactory
    implements IExecutableExtensionFactory, IExecutableExtension
{
    private static final String CONTRIBUTOR_INTERFACE = "com._1c.g5.v8.dt.bsl.ui.hover.IBslHoverContributor"; //$NON-NLS-1$
    private static final String FILL_TOOL_BAR = "fillToolBar"; //$NON-NLS-1$

    @Override
    public void setInitializationData(org.eclipse.core.runtime.IConfigurationElement config, String propertyName,
        Object data)
    {
        // Nothing to initialize; the factory is created by the platform via createExecutableExtension.
    }

    @Override
    public Object create() throws CoreException
    {
        Class<?> contributorInterface;
        try
        {
            contributorInterface = Class.forName(CONTRIBUTOR_INTERFACE);
        }
        catch (ClassNotFoundException e)
        {
            // Runtime is too old to have the hover SPI: contribute nothing.
            return null;
        }

        return Proxy.newProxyInstance(contributorInterface.getClassLoader(),
            new Class<?>[] { contributorInterface }, new HoverInvocationHandler());
    }

    /**
     * Dispatches the reflective {@code IBslHoverContributor.fillToolBar(IToolBarManager,
     * Collection&lt;Annotation&gt;)} call to the real logic, matching by method name so the code
     * never references the version-specific SPI type.
     */
    private static final class HoverInvocationHandler
        implements InvocationHandler
    {
        // Lazily created: instantiating the generator triggers Guice member injection, which must
        // not run before the plugin's DI is up. First hover happens well after startup.
        private AIMarkerResolutionGenerator generator;

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args)
        {
            if (FILL_TOOL_BAR.equals(method.getName()) && args != null && args.length == 2
                && args[0] instanceof IToolBarManager && args[1] instanceof Collection)
            {
                @SuppressWarnings("unchecked")
                var annotations = (Collection<Annotation>)args[1];
                fillToolBar((IToolBarManager)args[0], annotations);
                return null;
            }
            // Object methods (toString/hashCode/equals) and anything unexpected.
            switch (method.getName())
            {
            case "hashCode": //$NON-NLS-1$
                return System.identityHashCode(proxy);
            case "equals": //$NON-NLS-1$
                return proxy == (args != null ? args[0] : null);
            case "toString": //$NON-NLS-1$
                return "AIMarkerHoverContributor"; //$NON-NLS-1$
            default:
                return null;
            }
        }

        private void fillToolBar(IToolBarManager manager, Collection<Annotation> annotations)
        {
            for (Annotation annotation : annotations)
            {
                if (!(annotation instanceof MarkerAnnotation))
                {
                    continue;
                }
                IMarker marker = ((MarkerAnnotation)annotation).getMarker();
                if (marker == null || !marker.exists())
                {
                    continue;
                }
                // getResolutions returns empty for non-AI markers (no action attributes), so this
                // naturally scopes to our markers without an explicit type check.
                for (IMarkerResolution resolution : getGenerator().getResolutions(marker))
                {
                    manager.add(createAction(marker, resolution));
                }
            }
        }

        private synchronized AIMarkerResolutionGenerator getGenerator()
        {
            if (generator == null)
            {
                generator = new AIMarkerResolutionGenerator();
            }
            return generator;
        }
    }

    private static Action createAction(IMarker marker, IMarkerResolution resolution)
    {
        var action = new Action(resolution.getLabel())
        {
            @Override
            public void run()
            {
                resolution.run(marker);
            }
        };
        if (resolution instanceof IMarkerResolution2)
        {
            var resolution2 = (IMarkerResolution2)resolution;
            action.setToolTipText(resolution2.getDescription());
            var image = resolution2.getImage();
            if (image != null)
            {
                // The image is a shared, plugin-managed instance — wrap without taking ownership.
                action.setImageDescriptor(ImageDescriptor.createFromImage(image));
            }
        }
        return action;
    }
}
