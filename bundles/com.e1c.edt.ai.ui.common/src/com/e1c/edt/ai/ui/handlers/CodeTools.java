/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import java.util.HashSet;
import java.util.Optional;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.AIContextKind;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.CodePart;
import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.Range;
import com.e1c.edt.ai.assistent.model.CursorLocation;
import com.e1c.edt.ai.ui.AITarget;
import com.e1c.edt.ai.ui.Content;
import com.e1c.edt.ai.ui.IAIContextProvider;
import com.e1c.edt.ai.ui.ICodeParser;
import com.e1c.edt.ai.ui.IContentProvider;
import com.e1c.edt.ai.ui.ITextWidgetInfoProvider;
import com.e1c.edt.ai.ui.IUI;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CodeTools
    implements ICodeTools
{
    private final ILog log;
    private final IUI ui;
    private final ITextWidgetInfoProvider textWidgetInfoProvider;
    private final IContentProvider contentProvider;
    private final ICodeParser codeParser;
    private final ICodePartsProvider codePartsProvider;
    private final IAIContextProvider aiContextProvider;

    @Inject
    public CodeTools(ILog log, IUI ui, ITextWidgetInfoProvider textWidgetInfoProvider, IContentProvider contentProvider,
        ICodeParser codeParser, ICodePartsProvider codePartsProvider, IAIContextProvider aiContextProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(textWidgetInfoProvider);
        Preconditions.checkNotNull(contentProvider);
        Preconditions.checkNotNull(codeParser);
        Preconditions.checkNotNull(codePartsProvider);
        Preconditions.checkNotNull(aiContextProvider);
        this.log = log;
        this.ui = ui;
        this.textWidgetInfoProvider = textWidgetInfoProvider;
        this.contentProvider = contentProvider;
        this.codeParser = codeParser;
        this.codePartsProvider = codePartsProvider;
        this.aiContextProvider = aiContextProvider;
    }

    @Override
    public boolean hasTarget()
    {
        return ui.getLastTextWidget()
            .map(textWidget -> !textWidget.getSelectionText().isBlank() || getTargetMethod().isPresent())
            .orElse(false);
    }

    @Override
    public Optional<AIContext> createContextForTarget()
    {
        return ui.getLastTextWidget()
            .flatMap(textWidget -> {
                if (!textWidget.getSelectionText().isBlank())
                {
                    return aiContextProvider.create(new AITarget(textWidget, Integer.MAX_VALUE, true),
                        CancellationTokens.NONE);
                }

                var optionalTargetMethod = getTargetMethod();
                if (optionalTargetMethod.isPresent())
                {
                    return Optional.of(optionalTargetMethod.get().ctx);
                }

                return Optional.empty();
            });
    }

    @Override
    public Optional<TargetMethod> getTargetMethod()
    {
        return ui.getLastTextWidget()
            .flatMap(textWidget -> ui.getSourceViewer(textWidget))
            .flatMap(sourceViewer -> getTargetMethod(sourceViewer));
    }

    private Optional<Content> getContent(SourceViewer sourceViewer)
    {
        return textWidgetInfoProvider.getLastMouseOffset(sourceViewer.getTextWidget())
            .map(offset -> contentProvider.get(sourceViewer.getTextWidget(), offset));
    }

    private Optional<TargetMethod> getTargetMethod(SourceViewer sourceViewer)
    {
        var optionalContent = getContent(sourceViewer);
        if (optionalContent.isEmpty())
        {
            return Optional.empty();
        }

        var content = optionalContent.get();
        var optionalRootNode = codeParser.parse(sourceViewer).map(parseResult -> parseResult.getRootNode());
        if (optionalRootNode.isEmpty())
        {
            return Optional.empty();
        }

        var rootNode = optionalRootNode.get();
        var cursorNode = NodeModelUtils.findLeafNodeAtOffset(rootNode, content.offset);
        if (cursorNode == null)
        {
            return Optional.empty();
        }

        if (!codePartsProvider.isMethod(cursorNode))
        {
            return Optional.empty();
        }

        var commentingMethod = new TargetMethod();
        Integer methodId = null;
        Range range = Range.EMPTY;
        var methods = new HashSet<Integer>();
        CodePart lastMethodPart = null;
        var partsIterator = codePartsProvider.getParts(rootNode).iterator();
        while (partsIterator.hasNext())
        {
            var part = partsIterator.next();
            var curMethodId = part.getMethodId();
            if (curMethodId == null)
            {
                continue;
            }

            lastMethodPart = part;

            if (methods.add(curMethodId))
            {
                if (methodId != null)
                {
                    break;
                }

                range = part.getRange();
                if (part.getLocation() == CursorLocation.Comment)
                {
                    commentingMethod.commentRange = range;
                }
                else
                {
                    commentingMethod.commentRange = new Range(range.getStart(), 0);
                }
            }
            else
            {
                range = range.merge(part.getRange());
            }

            if (part.getLocation() == CursorLocation.Comment)
            {
                commentingMethod.commentRange = range;
            }

            if (range.contains(content.offset))
            {
                methodId = curMethodId;
            }
        }

        if (methodId == null && lastMethodPart != null)
        {
            methodId = lastMethodPart.getMethodId();
        }

        if (methodId == null || commentingMethod.commentRange == null)
        {
            return Optional.empty();
        }

        commentingMethod.methodText =
            content.text.substring(range.getStart(), range.getStart() + range.getLength() - 1);
        if (commentingMethod.methodText.isBlank())
        {
            return Optional.empty();
        }

        commentingMethod.sourceViewer = sourceViewer;
        var target = new AITarget(sourceViewer.getTextWidget(), Integer.MAX_VALUE, true);
        var lastRange = range;
        aiContextProvider.create(target, CancellationTokens.NONE).ifPresent(ctx -> {
            var methodCtx = new AIContext(ctx.getProjectId(), AIContextKind.ActiveEditor, lastRange.getStart(),
                ctx.getSource(), lastRange.getStart(), ctx.getPath(), commentingMethod.methodText, 0, "", //$NON-NLS-1$
                commentingMethod.methodText, lastRange.getStart(), lastRange.getStart() + lastRange.getLength());
            commentingMethod.ctx = methodCtx;
        });
        return Optional.of(commentingMethod);
    }

    @Override
    public void selectMethodComment(TargetMethod targetMethod)
    {
        getRange(targetMethod.sourceViewer.getTextWidget(), targetMethod.commentRange)
            .ifPresent(range -> targetMethod.sourceViewer.setSelectedRange(range.getStart(), range.getLength()));
    }

    private Optional<Range> getRange(StyledText widget, Range range)
    {
        try
        {
            var start = range.getStart();
            var length = range.getLength();
            var fullText = widget.getText();
            int dif = 0, newLength = 0;
            if (length == 0)
            {
                if (start > 0)
                {
                    var text = fullText.substring(start);
                    dif = text.length() - text.stripLeading().length();
                }
            }
            else
            {
                var text = fullText.substring(start, start + length).stripLeading();
                newLength = text.length();
                dif = length - newLength;
            }

            if (dif < 0)
            {
                dif = 0;
            }

            return Optional.of(new Range(start + dif, newLength));
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        return Optional.empty();
    }
}
