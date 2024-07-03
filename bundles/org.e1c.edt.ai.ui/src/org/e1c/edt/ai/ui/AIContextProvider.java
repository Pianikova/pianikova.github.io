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
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.ui.editor.XtextSourceViewer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AIContextProvider
    implements IAIContextProvider<Void>
{
    private final IUI ui;
    private final IUISettings uiSettings;
    private final IAIContextFactory contextFactory;
    private final ICodePartsProvider codePartsProvider;
    private final IAIContextProvider<AISourceContext> commentsContextProvider;
    private final IAIContextProvider<AISourceContext> codeSizeReducerContextProvider;

    @Inject
    public AIContextProvider(IUI ui,
        IUISettings uiSettings,
        IAIContextFactory contextFactory,
        ICodePartsProvider codePartsProvider,
        @SourceMethodComments IAIContextProvider<AISourceContext> commentsContextProvider,
        @SourceCodeSizeReducer IAIContextProvider<AISourceContext> codeSizeReducerContextProvider)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(contextFactory);
        Preconditions.checkNotNull(codePartsProvider);
        Preconditions.checkNotNull(commentsContextProvider);
        Preconditions.checkNotNull(codeSizeReducerContextProvider);
        this.ui = ui;
        this.uiSettings = uiSettings;
        this.contextFactory = contextFactory;
        this.codePartsProvider = codePartsProvider;
        this.commentsContextProvider = commentsContextProvider;
        this.codeSizeReducerContextProvider = codeSizeReducerContextProvider;
    }

    @Override
    public Optional<AIContext> create(AITarget target, Void state, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(cancellationToken);
        return ui.getSourceViewer(target.getTextWidget())
            .flatMap(sourceViewer -> create(sourceViewer, target, state, cancellationToken));
    }

    private Optional<AIContext> create(SourceViewer sourceViewer, AITarget target, Void state,
        ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(sourceViewer);
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(cancellationToken);
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

            return contextFactory.create(text, offset, text, offset, target.getComplitionType());

        default:
            break;
        }

        Optional<AIContext> result = Optional.empty();
        if (sourceViewer instanceof XtextSourceViewer)
        {
            var xtextSourceViewer = (XtextSourceViewer)sourceViewer;
            var parseResult = xtextSourceViewer.getXtextDocument().readOnly(s -> s.getParseResult());
            if (parseResult != null)
            {
                var parts = codePartsProvider.getParts(parseResult.getRootNode());
                var sourceCtx = new AISourceContext(xtextSourceViewer, parseResult, offset,
                    uiSettings.getMaxAssistantTextSize(), parts);

                AITarget actualTarget = target;
                for (var part : parts)
                {
                    if (part.getRange().contains(offset))
                    {
                        switch (part.getType())
                        {
                        case Comment:
                            actualTarget = new AITarget(target.getTextWidget(), target.getMaxLength(),
                                CodeCompletionType.CodeCommentsContinue);
                            break;

                        case MethodPrefix:
                            actualTarget = new AITarget(target.getTextWidget(), target.getMaxLength(),
                                CodeCompletionType.CodeComments);
                            break;

                        default:
                            break;
                        }

                        break;
                    }
                }

                switch (actualTarget.getComplitionType())
                {
                case CodeComments:
                case CodeCommentsContinue:
                    result = commentsContextProvider.create(actualTarget, sourceCtx, cancellationToken);
                    break;

                case CodeLines:
                case CodeSingleWord:
                    if (text.length() <= max)
                    {
                        return contextFactory.create(text, offset, text, offset,
                            actualTarget.getComplitionType());
                    }

                    sourceCtx.SkipMinorMethods = true;
                    sourceCtx.Forcable = true;
                    result = codeSizeReducerContextProvider.create(actualTarget, sourceCtx, cancellationToken);
                    break;

                default:
                    break;
                }
            }
        }

        if (result.isPresent())
        {
            return result;
        }

        return contextFactory.create(text, offset, text, offset, target.getComplitionType());
    }
}