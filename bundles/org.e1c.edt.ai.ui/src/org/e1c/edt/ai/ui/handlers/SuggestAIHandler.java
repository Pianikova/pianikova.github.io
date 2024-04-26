/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ICodeCompletionTokenizer;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.assistent.IAICodeAssistant;
import org.e1c.edt.ai.ui.Activator;
import org.e1c.edt.ai.ui.CodeCompletionViewModel;
import org.e1c.edt.ai.ui.Composition;
import org.e1c.edt.ai.ui.HintPainter;
import org.e1c.edt.ai.ui.IAIContextProvider;
import org.e1c.edt.ai.ui.IDispatcher;
import org.e1c.edt.ai.ui.IModelUIPluginImages;
import org.e1c.edt.ai.ui.IUI;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.graphics.Image;

public class SuggestAIHandler
    extends AbstractHandler
{
    private final ILog log;
    private final IUI ui;
    private final IAICodeAssistant codeAssistant;
    private final IAIContextProvider aiContextProvider;
    private final IDispatcher dispatcher;
    private final ICodeCompletionTokenizer tokenizer;
    private CodeCompletionViewModel codeCompletion;
    private Image pinImage;

    public SuggestAIHandler()
    {
        log = Composition.getLog();
        ui = Composition.getUI();
        codeAssistant = Composition.getCodeAssistant();
        aiContextProvider = Composition.getAIContextProvider();
        dispatcher = Composition.getDispatcher();
        tokenizer = Composition.getCodeCompletionTokenizer();
        pinImage = Activator.getImage(IModelUIPluginImages.NAME_ICON_THINKING);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        if (codeCompletion != null)
        {
            codeCompletion.deactivate();
        }

        ui.getTextViewer().ifPresent(textViewer -> {
            var painter = new HintPainter(textViewer);
            painter.setPinImage(pinImage);
            codeCompletion =
                new CodeCompletionViewModel(log, codeAssistant, aiContextProvider, dispatcher, ui, tokenizer, painter);
            codeCompletion.activate();
        });

        return null;
    }

    // CursorLinePainter
}
