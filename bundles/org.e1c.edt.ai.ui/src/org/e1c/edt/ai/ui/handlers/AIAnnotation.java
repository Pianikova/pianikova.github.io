/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import java.util.function.Consumer;

import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ui.Composition;
import org.e1c.edt.ai.ui.IDispatcher;
import org.e1c.edt.ai.ui.IUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.inlined.LineContentAnnotation;
import org.eclipse.swt.events.MouseEvent;
/**
 * @author Bogdan Sushkov
 *
 */
public class AIAnnotation
    extends LineContentAnnotation
{
    private String text;
    private Position position;
    private ISourceViewer viewer;
    private final ILog log;
    private final IUI ui;
    private final IDispatcher dispatcher;

    /**
     * @param position
     * @param viewer
     */
    public AIAnnotation(Position position, ISourceViewer viewer)
    {
        super(position, viewer);
        this.position = position;
        this.viewer = viewer;
        log = Composition.getLog();
        ui = Composition.getUI();
        dispatcher = Composition.getDispatcher();
    }

    @Override
    public void setText(String text)
    {
        super.setText(text);
        this.text = text;
    }

    @Override
    public Consumer<MouseEvent> getAction(MouseEvent e)
    {
        return (Consumer<MouseEvent>)action -> {
            dispatcher.dispatch(() -> {
                try
                {
                    viewer.getDocument().replace(position.getOffset(), 0, text);
                    setCursorOffset(position.getOffset());
                }
                catch (BadLocationException ex)
                {
                    log.logError(ex);
                }
            });
        };
    }

    private void setCursorOffset(int offset)
    {
        ui.select(new TextSelection(offset, 0));
    }
}
