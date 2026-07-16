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

import com.e1c.edt.ai.ui.Images;
import com.e1c.edt.ai.ui.handlers.AIMarkerResolution;
import com.e1c.edt.ai.ui.handlers.AIMarkerResolutionGenerator;
import com.e1c.edt.ai.ui.handlers.ExternalProblemFixer;
import com.e1c.edt.ai.ui.handlers.ExternalProblemMarkerResolutionGenerator;

/**
 * Contributes AI quick-fix buttons to the BSL annotation hover toolbar, so the fix is
 * reachable directly from the marker icon on the editor's vertical ruler — both for our own
 * AI markers and for standard BSL validation problems (via the external-problem generator).
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
        // Lazily created: instantiating the generators triggers Guice member injection, which must
        // not run before the plugin's DI is up. First hover happens well after startup.
        private AIMarkerResolutionGenerator generator;
        private ExternalProblemMarkerResolutionGenerator externalGenerator;
        private ExternalProblemFixer annotationFixer;

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

        /**
         * A hovered line often carries several problems at once (an AI marker plus standard
         * validation errors/warnings, both as markers and as live Xtext annotations). Identical
         * icon-only buttons per problem are indistinguishable, so exactly ONE fix button is
         * contributed per line: the AI marker's action when present (its prompt is the most
         * specific; the other messages are attached as extra context), otherwise one aggregated
         * "Fix with 1C:Workmate" over all standard problem messages of the line.
         */
        private void fillToolBar(IToolBarManager manager, Collection<Annotation> annotations)
        {
            IMarker aiMarker = null;
            IMarkerResolution aiResolution = null;
            Annotation primaryProblem = null;
            var problemMessages = new java.util.LinkedHashSet<String>();

            for (Annotation annotation : annotations)
            {
                if (annotation instanceof MarkerAnnotation)
                {
                    IMarker marker = ((MarkerAnnotation)annotation).getMarker();
                    if (marker == null || !marker.exists())
                    {
                        continue;
                    }
                    // getResolutions returns empty for non-AI markers (no action attributes), so
                    // this naturally scopes to our markers without an explicit type check.
                    var aiResolutions = getGenerator().getResolutions(marker);
                    if (aiResolutions.length > 0)
                    {
                        if (aiResolution == null)
                        {
                            aiMarker = marker;
                            aiResolution = aiResolutions[0];
                        }
                        continue;
                    }
                    if (getExternalGenerator().hasResolutions(marker))
                    {
                        var message = marker.getAttribute(IMarker.MESSAGE, ""); //$NON-NLS-1$
                        problemMessages.add(message);
                        if (primaryProblem == null)
                        {
                            primaryProblem = annotation;
                        }
                    }
                }
                else if (getAnnotationFixer().canFix(annotation))
                {
                    // Live Xtext annotation without a backing marker (unsaved/unbuilt problem).
                    problemMessages.add(annotation.getText());
                    if (primaryProblem == null)
                    {
                        primaryProblem = annotation;
                    }
                }
            }

            if (aiResolution != null)
            {
                if (aiResolution instanceof AIMarkerResolution && !problemMessages.isEmpty())
                {
                    ((AIMarkerResolution)aiResolution).setAdditionalProblems(String.join("\n", problemMessages)); //$NON-NLS-1$
                }
                manager.add(createAction(aiMarker, aiResolution));
            }
            else if (primaryProblem != null)
            {
                manager.add(createAnnotationAction(getAnnotationFixer(), primaryProblem,
                    String.join("\n", problemMessages))); //$NON-NLS-1$
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

        private synchronized ExternalProblemMarkerResolutionGenerator getExternalGenerator()
        {
            if (externalGenerator == null)
            {
                externalGenerator = new ExternalProblemMarkerResolutionGenerator();
            }
            return externalGenerator;
        }

        private synchronized ExternalProblemFixer getAnnotationFixer()
        {
            if (annotationFixer == null)
            {
                annotationFixer = new ExternalProblemFixer();
            }
            return annotationFixer;
        }
    }

    private static Action createAnnotationAction(ExternalProblemFixer fixer, Annotation annotation,
        String combinedMessages)
    {
        var action = new Action(fixer.getLabel())
        {
            @Override
            public void run()
            {
                fixer.fix(annotation, combinedMessages);
            }
        };
        // The tooltip lists the actual problems, so the icon-only button is identifiable.
        action.setToolTipText(
            combinedMessages != null && !combinedMessages.isBlank() ? combinedMessages : fixer.getDescription());
        var image = com.e1c.edt.ai.ui.BaseActivator.getImage(Images.AI);
        if (image != null)
        {
            // The image is a shared, plugin-managed instance — wrap without taking ownership.
            action.setImageDescriptor(ImageDescriptor.createFromImage(image));
        }
        return action;
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
