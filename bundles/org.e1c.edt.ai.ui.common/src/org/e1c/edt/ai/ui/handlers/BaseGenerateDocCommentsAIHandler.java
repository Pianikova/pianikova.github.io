/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import java.util.HashSet;
import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationTokens;
import org.e1c.edt.ai.CodePart;
import org.e1c.edt.ai.ICodePartsProvider;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.Range;
import org.e1c.edt.ai.assistent.model.CursorLocation;
import org.e1c.edt.ai.ui.AITarget;
import org.e1c.edt.ai.ui.BaseActivator;
import org.e1c.edt.ai.ui.Content;
import org.e1c.edt.ai.ui.IAIContextProvider;
import org.e1c.edt.ai.ui.IChat;
import org.e1c.edt.ai.ui.ICodeParser;
import org.e1c.edt.ai.ui.IContentProvider;
import org.e1c.edt.ai.ui.IUI;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com.google.inject.Inject;

/**
 * Class Handler of the fix the Code command.
 *
 * @author George Suaridze
 */
public class BaseGenerateDocCommentsAIHandler
    extends AbstractHandler
{
    @Inject
    IUISettings uiSettings;
    @Inject
    IAIContextProvider aiContextProvider;
    @Inject
    IChat chat;
    @Inject
    IUI ui;
    @Inject
    IContentProvider contentProvider;
    @Inject
    ICodePartsProvider codePartsProvider;
    @Inject
    ICodeParser codeParser;

    public BaseGenerateDocCommentsAIHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean isEnabled()
    {
        return uiSettings.isCodeCompletion() && getCommentingMethod().isPresent();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        getCommentingMethod().ifPresent(commentingMethod -> {
            commentingMethod.sourceViewer.setSelectedRange(commentingMethod.commentRange.getStart(),
                commentingMethod.commentRange.getLength());
            chat.generateDocComments(commentingMethod.ctx, commentingMethod.methodText);
        });
        return null;
    }

    private Optional<CommentingMethod> getCommentingMethod()
    {
        return ui.getTextWidget()
            .flatMap(textWidget -> ui.getSourceViewer(textWidget))
            .flatMap(
                sourceViewer -> getCommentingMethod(contentProvider.get(sourceViewer.getTextWidget()), sourceViewer));
    }

    private Optional<CommentingMethod> getCommentingMethod(Content content, SourceViewer sourceViewer)
    {
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

        var commentingMethod = new CommentingMethod();
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
        aiContextProvider.create(target, CancellationTokens.NONE).ifPresent(ctx -> commentingMethod.ctx = ctx);
        return Optional.of(commentingMethod);
    }

    private class CommentingMethod
    {
        public AIContext ctx;

        public SourceViewer sourceViewer;

        public String methodText;

        public Range commentRange;
    }
}
