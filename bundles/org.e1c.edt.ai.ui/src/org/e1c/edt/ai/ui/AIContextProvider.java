/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CodeCompletionType;
import org.e1c.edt.ai.IAIContextFactory;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.ui.AIUIModule.SourceCodeSizeReducer;
import org.e1c.edt.ai.ui.AIUIModule.SourceMethodComments;
import org.eclipse.xtext.ui.editor.XtextSourceViewer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AIContextProvider
    implements IAIContextProvider<Void>
{
    private final IUI ui;
    private final IUISettings uiSettings;
    private final IAIContextFactory contextFactory;
    private final ICodeCompletionTypeProvider codeCompletionTypeProvider;
    private final IAIContextProvider<AISourceContext> commentsContextProvider;
    private final IAIContextProvider<AISourceContext> codeSizeReducerContextProvider;

    @Inject
    public AIContextProvider(IUI ui,
        IUISettings uiSettings,
        IAIContextFactory contextFactory,
        ICodeCompletionTypeProvider codeCompletionTypeProvider,
        @SourceMethodComments IAIContextProvider<AISourceContext> commentsContextProvider,
        @SourceCodeSizeReducer IAIContextProvider<AISourceContext> codeSizeReducerContextProvider)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(contextFactory);
        Preconditions.checkNotNull(codeCompletionTypeProvider);
        Preconditions.checkNotNull(commentsContextProvider);
        Preconditions.checkNotNull(codeSizeReducerContextProvider);
        this.ui = ui;
        this.uiSettings = uiSettings;
        this.contextFactory = contextFactory;
        this.codeCompletionTypeProvider = codeCompletionTypeProvider;
        this.commentsContextProvider = commentsContextProvider;
        this.codeSizeReducerContextProvider = codeSizeReducerContextProvider;
    }

    @Override
    public Optional<AIContext> create(AITarget target, Void state, ICancellationToken cancellationToken)
    {
        return ui.getSourceViewer(target.getTextWidget())
            .flatMap(sourceViewer -> {
                var textWidget = sourceViewer.getTextWidget();
                var text = textWidget.getText();
                var offset = textWidget.getCaretOffset();
                var max = target.getMaxLength();
                if (max <= 0)
                {
                    max = uiSettings.getMaxAssistantTextSize();
                }

                switch (target.getComplitionType())
                {
                case ChatComments:
                case ChatExplain:
                case ChatFix:
                case ChatReview:
                    if (textWidget.isTextSelected())
                    {
                        text = textWidget.getSelectionText();
                        offset = 0;
                    }

                    return contextFactory.create(text, offset, CodeCompletionType.CodeLines);

                default:
                    break;
                }

                if (text.length() <= max)
                {
                    return contextFactory.create(text, offset, CodeCompletionType.CodeLines);
                }

                Optional<AIContext> result = null;
                if (sourceViewer instanceof XtextSourceViewer)
                {
                    var xtextSourceViewer = (XtextSourceViewer)sourceViewer;
                    var parseResult = xtextSourceViewer.getXtextDocument().readOnly(s -> s.getParseResult());
                    if (parseResult != null)
                    {
                        var sourceCtx = new AISourceContext(xtextSourceViewer, parseResult, offset,
                            uiSettings.getMaxAssistantTextSize());

                        var codeCompletionType = codeCompletionTypeProvider.getType(sourceCtx);
                        switch (codeCompletionType)
                        {
                        case CodeComments:
                            result = commentsContextProvider.create(target, sourceCtx, cancellationToken);
                            break;

                        case CodeLines:
                        case CodeSingleWord:
                            sourceCtx.SkipMinorMethods = true;
                            sourceCtx.Forcable = true;
                            result = codeSizeReducerContextProvider.create(target, sourceCtx, cancellationToken);
                            break;

                        default:
                            break;
                        }
                    }
                }

                if (result != null && result.isPresent())
                {
                    return result;
                }

                return contextFactory.create(text, offset, CodeCompletionType.CodeLines);
            });
    }
}