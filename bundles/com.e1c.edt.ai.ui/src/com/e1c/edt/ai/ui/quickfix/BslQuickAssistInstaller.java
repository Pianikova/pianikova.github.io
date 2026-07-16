/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.quickfix;

import java.lang.reflect.Field;

import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewerExtension3;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.xtext.ui.editor.hover.AnnotationWithQuickFixesHover;
import org.eclipse.xtext.ui.editor.quickfix.XtextQuickAssistProcessor;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.ui.IDispatcher;
import com.e1c.edt.ai.ui.IInitializable;
import com.e1c.edt.ai.ui.IUI;
import com.google.inject.Inject;

/** Installs the composite Workmate/native quick-assist processor into active BSL editors. */
public class BslQuickAssistInstaller
    implements IInitializable, Listener
{
    private static final String QUICK_ASSIST_PROCESSOR_FIELD = "quickAssistProcessor"; //$NON-NLS-1$

    private final IUI ui;
    private final IDispatcher dispatcher;
    private final ILog log;

    @Inject
    public BslQuickAssistInstaller(IUI ui, IDispatcher dispatcher, ILog log)
    {
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.log = log;
    }

    @Override
    public void initialize()
    {
        dispatcher.dispatchAsync(() -> {
            Display display = Display.getDefault();
            display.addFilter(SWT.FocusIn, this);
            display.addFilter(SWT.MouseHover, this);
            // FocusIn may arrive while the source viewer is still being configured. Retrying on
            // KeyDown guarantees that the processor is installed before Ctrl+1 is dispatched.
            display.addFilter(SWT.KeyDown, this);
            if (display.getFocusControl() instanceof StyledText)
            {
                install((StyledText)display.getFocusControl());
            }
        });
    }

    @Override
    public void handleEvent(Event event)
    {
        if (event.widget instanceof StyledText)
        {
            install((StyledText)event.widget);
        }
    }

    private void install(StyledText textWidget)
    {
        ui.getSourceViewer(textWidget).ifPresent(this::install);
    }

    private void install(SourceViewer viewer)
    {
        if (!(viewer instanceof ISourceViewerExtension3))
        {
            return;
        }
        ISourceViewerExtension3 extension = (ISourceViewerExtension3)viewer;
        IQuickAssistAssistant assistant = extension.getQuickAssistAssistant();
        if (assistant == null)
        {
            return;
        }
        IQuickAssistProcessor current = assistant.getQuickAssistProcessor();
        AICompositeBslQuickAssistProcessor composite;
        if (current instanceof AICompositeBslQuickAssistProcessor)
        {
            composite = (AICompositeBslQuickAssistProcessor)current;
        }
        else
        {
            // Do not depend on the concrete BSL processor package: EDT versions may wrap or move
            // it. The EDT plugin only installs this listener in its own product, and every BSL
            // processor is an XtextQuickAssistProcessor.
            if (!(current instanceof XtextQuickAssistProcessor))
            {
                return;
            }
            composite = new AICompositeBslQuickAssistProcessor(current, log);
            assistant.setQuickAssistProcessor(composite);
            String processorName = current.getClass().getName();
            log.trace(TracingSources.COMMON, "Quick assist", //$NON-NLS-1$
                () -> "Installed Workmate composite over " + processorName); //$NON-NLS-1$
        }
        installIntoHover(viewer, extension, composite);
    }

    private void installIntoHover(SourceViewer viewer, ISourceViewerExtension3 extension,
        AICompositeBslQuickAssistProcessor composite)
    {
        Object hover = extension.getCurrentAnnotationHover();
        if (!(hover instanceof AnnotationWithQuickFixesHover))
        {
            try
            {
                Field annotationHoverField = SourceViewer.class.getDeclaredField("fAnnotationHover"); //$NON-NLS-1$
                annotationHoverField.setAccessible(true);
                hover = annotationHoverField.get(viewer);
            }
            catch (ReflectiveOperationException | RuntimeException e)
            {
                log.logError(e);
                return;
            }
        }
        if (!(hover instanceof AnnotationWithQuickFixesHover))
        {
            return;
        }
        try
        {
            Field field = AnnotationWithQuickFixesHover.class.getDeclaredField(QUICK_ASSIST_PROCESSOR_FIELD);
            field.setAccessible(true);
            if (field.get(hover) != composite)
            {
                field.set(hover, composite);
            }
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            log.logError(e);
        }
    }
}
