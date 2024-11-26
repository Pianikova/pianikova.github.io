/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.assistent.ITextPreprocessor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class IdeApiHandler
{
    private static final String AI_CHAT = "AI Chat"; //$NON-NLS-1$
    private final ILog log;
    private final IUI ui;
    private final ITextPreprocessor textPreprocessor;

    @Inject
    public IdeApiHandler(ILog log, IUI ui, ITextPreprocessor textPreprocessor)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(textPreprocessor);
        this.log = log;
        this.ui = ui;
        this.textPreprocessor = textPreprocessor;
    }

    public void wink(String parameter)
    {
        Preconditions.checkNotNull(parameter);
        log.trace(AI_CHAT, "winked: " + parameter); //$NON-NLS-1$
    }

    public void paste_code(String code)
    {
        if (code == null)
        {
            return;
        }

        final var processedCode = textPreprocessor.process(code);
        ui.getTextWidget().ifPresent(textWidget -> {
            var contet = textWidget.getContent();
            ui.getSourceViewer(textWidget).ifPresent(sourceViewer -> {
                var selection = sourceViewer.getSelection();
                if (selection instanceof TextSelection)
                {
                    var textSelection = (TextSelection)selection;
                    if (textSelection.getLength() > 0)
                    {
                        var shellOptional = ui.getShell();
                        if (shellOptional.isPresent())
                        {
                            if (!MessageDialog.openQuestion(shellOptional.get(), Messages.AIName,
                                Messages.ReplaceCode))
                            {
                                return;
                            }
                        }
                    }

                    contet.replaceTextRange(sourceViewer.modelOffset2WidgetOffset(textSelection.getOffset()),
                        textSelection.getLength(), processedCode);
                    return;
                }

                contet.replaceTextRange(textWidget.getCaretOffset(), 0, processedCode);
            });
        });
    }

    public void trace(String message)
    {
        // Chat tracing
        log.trace(AI_CHAT, message);
    }
}
