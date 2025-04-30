/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerInfoExtension;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.OS;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class VerticalRulerManager
    implements IVerticalRulerManager
{
    private final IDispatcher dispatcher;
    private final IVerticalRulerPainter painterListener;
    private final IEnvironment environment;

    @Inject
    public VerticalRulerManager(IDispatcher dispatcher, IVerticalRulerPainter painterListener, IEnvironment environment)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(painterListener);
        Preconditions.checkNotNull(environment);
        this.dispatcher = dispatcher;
        this.painterListener = painterListener;
        this.environment = environment;
    }

    @Override
    public AutoCloseable activate(SourceViewer viewer, Runnable onReset)
    {
        Preconditions.checkNotNull(onReset);
        if (environment.getOS() != OS.WINDOWS)
        {
            return Closeables.Empty;
        }

        return Optional.ofNullable(viewer).flatMap(v -> getCompositeRuler(v)).map(ruler -> {
            var modelListener = createModelListener(ruler);
            var trackerListener = new RulerMouseTrackListener(onReset);
            var viewportListener = new ViewportListener(viewer);
            addListeners(viewer, ruler, modelListener, trackerListener, viewportListener);
            return Closeables
                .create(() -> removeListeners(viewer, ruler, modelListener, trackerListener, viewportListener));
        }).orElse(Closeables.Empty);
    }

    private AnnotationModelListener createModelListener(CompositeRuler ruler)
    {
        Runnable run = () -> redraw(ruler.getControl());
        return new AnnotationModelListener(() -> dispatcher.dispatchAsync(() -> {
            run.run();
            dispatcher.dispatchAsync(run);
        }));
    }

    @Override
    public void redraw(SourceViewer viewer)
    {
        dispatcher.dispatch(() -> redrawInternal(viewer));
    }

    private void redrawInternal(SourceViewer viewer)
    {
        Optional.ofNullable(viewer)
            .flatMap(v -> getCompositeRuler(v))
            .map(ruler -> ruler.getDecoratorIterator())
            .ifPresent(columns -> {
                while (columns.hasNext())
                {
                    var column = columns.next();
                    redraw(column.getControl());
                }
            });
    }

    private void redraw(Control control)
    {
        if (control.isDisposed())
        {
            return;
        }

        if (control instanceof Composite)
        {
            var composite = (Composite)control;
            control.redraw();
            for (Control child : composite.getChildren())
            {
                redraw(child); // Recurse into children
            }
        }
        else
        {
            control.redraw();
        }
    }

    private void addListeners(SourceViewer viewer, CompositeRuler ruler, AnnotationModelListener modelListener,
        RulerMouseTrackListener trackerListener, ViewportListener viewportListener)
    {
        var decorators = ruler.getDecoratorIterator();
        while (decorators.hasNext())
        {
            var column = decorators.next();
            if (column instanceof IVerticalRulerInfoExtension)
            {
                var info = (IVerticalRulerInfoExtension)column;
                info.getModel().addAnnotationModelListener(modelListener);
            }

            var control = column.getControl();
            control.addPaintListener(painterListener);
            control.addMouseTrackListener(trackerListener);
        }

        viewer.addViewportListener(viewportListener);
    }

    private void removeListeners(SourceViewer viewer, CompositeRuler ruler, AnnotationModelListener modelListener,
        RulerMouseTrackListener tackerListener, ViewportListener viewportListener)
    {
        var decorators = ruler.getDecoratorIterator();
        while (decorators.hasNext())
        {
            var column = decorators.next();
            if (column instanceof IVerticalRulerInfoExtension)
            {
                var info = (IVerticalRulerInfoExtension)column;
                info.getModel().removeAnnotationModelListener(modelListener);
            }

            var control = column.getControl();
            control.removePaintListener(painterListener);
            control.removeMouseTrackListener(tackerListener);
        }

        viewer.removeViewportListener(viewportListener);
    }

    private Optional<CompositeRuler> getCompositeRuler(SourceViewer viewer)
    {
        return getVerticalRuler(viewer).map(ruler -> ruler instanceof CompositeRuler ? (CompositeRuler)ruler : null);
    }

    private Optional<IVerticalRuler> getVerticalRuler(SourceViewer viewer)
    {
        try
        {
            var method = SourceViewer.class.getDeclaredMethod("getVerticalRuler"); //$NON-NLS-1$
            method.setAccessible(true);
            return Optional.ofNullable((IVerticalRuler)method.invoke(viewer));
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }

    private class AnnotationModelListener
        implements IAnnotationModelListener
    {
        private final Runnable onModelChanged;

        public AnnotationModelListener(Runnable onModelChanged)
        {
            Preconditions.checkNotNull(onModelChanged);
            this.onModelChanged = onModelChanged;
        }

        @Override
        public void modelChanged(IAnnotationModel model)
        {
            onModelChanged.run();
        }
    }

    private class RulerMouseTrackListener
        implements MouseTrackListener
    {
        private final Runnable onReset;

        public RulerMouseTrackListener(Runnable onReset)
        {
            Preconditions.checkNotNull(onReset);
            this.onReset = onReset;
        }

        @Override
        public void mouseEnter(MouseEvent e)
        {
            onReset.run();
        }

        @Override
        public void mouseExit(MouseEvent e)
        {
            //
        }

        @Override
        public void mouseHover(MouseEvent e)
        {
            //
        }
    }

    private class ViewportListener implements IViewportListener
    {
        private final SourceViewer viewer;

        public ViewportListener(SourceViewer viewer)
        {
            this.viewer = viewer;
        }

        @Override
        public void viewportChanged(int verticalOffset)
        {
            dispatcher.dispatchAsync(() -> dispatcher.dispatchAsync(() -> {
                painterListener.updateRange();
                redrawInternal(viewer);
            }));
        }
    }
}
