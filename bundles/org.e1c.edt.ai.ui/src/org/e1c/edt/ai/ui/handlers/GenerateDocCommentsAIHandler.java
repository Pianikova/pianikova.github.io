/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import java.util.HashSet;
import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationTokens;
import org.e1c.edt.ai.Range;
import org.e1c.edt.ai.assistent.model.CursorLocation;
import org.e1c.edt.ai.context.CodePart;
import org.e1c.edt.ai.context.ICodePartsProvider;
import org.e1c.edt.ai.ui.AITarget;
import org.e1c.edt.ai.ui.Activator;
import org.e1c.edt.ai.ui.Content;
import org.e1c.edt.ai.ui.IAIContextProvider;
import org.e1c.edt.ai.ui.IChat;
import org.e1c.edt.ai.ui.IContentProvider;
import org.e1c.edt.ai.ui.IUI;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.ui.editor.XtextSourceViewer;

import com.google.inject.Inject;

/**
 * Class Handler of the fix the Code command.
 *
 * @author George Suaridze
 */
public class GenerateDocCommentsAIHandler
    extends AbstractHandler
{
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

    public GenerateDocCommentsAIHandler()
    {
        Activator.injectMembers(this);
    }

    @Override
    public boolean isEnabled()
    {
        return getCommentingMethod().isPresent();
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
        if (!(sourceViewer instanceof XtextSourceViewer))
        {
            return Optional.empty();
        }

        var xtextSourceViewer = (XtextSourceViewer)sourceViewer;
        var document = xtextSourceViewer.getXtextDocument();
        if (document == null)
        {
            return Optional.empty();
        }

        var parseResult = document.readOnly(s -> s.getParseResult());
        if (parseResult == null)
        {
            return Optional.empty();
        }

        var rootNoode = parseResult.getRootNode();
        if (rootNoode == null)
        {
            return Optional.empty();
        }

        var cursorNode = NodeModelUtils.findLeafNodeAtOffset(rootNoode, content.offset);
        if (cursorNode == null)
        {
            return Optional.empty();
        }

        var commentingMethod = new CommentingMethod();
        Integer methodId = null;
        Range range = Range.EMPTY;
        var methods = new HashSet<Integer>();
        CodePart lastMethodPart = null;
        var partsIterator = codePartsProvider.getParts(rootNoode).iterator();
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
