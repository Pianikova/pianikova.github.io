/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.ICodeCompletionTokenizer;
import org.e1c.edt.ai.IHintTextBuilder;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.assistent.IAICodeAssistant;

public class CodeCompletion implements ICodeCompletion
{
    private final ILog log;
    private final ISettingsStore settingsStore;
    private final IUI ui;
    private final IAICodeAssistant codeAssistant;
    private final IAIContextProvider aiContextProvider;
    private final IDispatcher dispatcher;
    private final ICodeCompletionTokenizer tokenizer;
    private ICodeCompletionViewModel codeCompletion;
    private AutoCloseable query = Closeables.Empty;
    private IHintTextBuilder hintTextBuilder;

    public CodeCompletion(ILog log, ISettingsStore settingsStore, IUI ui, IAICodeAssistant codeAssistant,
        IAIContextProvider aiContextProvider,
        IDispatcher dispatcher, ICodeCompletionTokenizer tokenizer, IHintTextBuilder hintTextBuilder)
    {
        this.log = log;
        this.settingsStore = settingsStore;
        this.ui = ui;
        this.codeAssistant = codeAssistant;
        this.aiContextProvider = aiContextProvider;
        this.dispatcher = dispatcher;
        this.tokenizer = tokenizer;
        this.hintTextBuilder = hintTextBuilder;
    }

    @Override
    public void show(boolean ask)
    {
        try
        {
            query.close();
        }
        catch (Exception e)
        {
            // ignored
        }

        ui.getTextViewer().ifPresent(textViewer -> {
            var hintPainter = new HintPainter(textViewer, hintTextBuilder);
            hintPainter.setLabel("Tab → ← Esc"); //$NON-NLS-1$
            codeCompletion =
                new CodeCompletionViewModel(log, settingsStore, codeAssistant, aiContextProvider, dispatcher, ui,
                    tokenizer, hintPainter);

            query = codeCompletion.activate(ask);
        });
    }
}