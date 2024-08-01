/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

import org.e1c.edt.ai.assistent.IFeedbackService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CodeCompletionStatistics
    implements ICodeCompletionContext, ICodeCompletionStatistics
{
    private final IFeedbackService feedbackService;
    private final ICursorInfoProvider cursorInfoProvider;
    private final ArrayList<Text> code = new ArrayList<>();
    private final HashMap<CodeMethod, HashSet<String>> methods = new HashMap<>();
    private String lastAcceptedSourceId = null;
    private Integer startOffset;

    @Inject
    public CodeCompletionStatistics(IFeedbackService feedbackService, ICursorInfoProvider cursorInfoProvider)
    {
        Preconditions.checkNotNull(feedbackService);
        Preconditions.checkNotNull(cursorInfoProvider);
        this.feedbackService = feedbackService;
        this.cursorInfoProvider = cursorInfoProvider;
    }

    @Override
    public synchronized void apply(Text text, int offset)
    {
        Preconditions.checkNotNull(text);
        if (offset < 0)
        {
            return;
        }

        var sourceId = text.getSource().getId();
        if (sourceId.isBlank())
        {
            return;
        }

        if (startOffset == null)
        {
            startOffset = offset;
        }

        code.add(text);
    }

    @Override
    public synchronized void rollback(int offset, int length)
    {
        while (length > 0 && code.size() > 0)
        {
            var index = code.size() - 1;
            var lastText = code.get(index);
            code.remove(index);
            var text = lastText.getText();
            var len = text.length();
            if (len <= length)
            {
                length -= len;
            }
            else
            {
                code.add(new Text(text.substring(0, len - length), lastText.getSource()));
                length = 0;
            }
        }
    }

    @Override
    public void commit()
    {
        try
        {
            if (code.isEmpty())
            {
                return;
            }

            var offset = 0;
            if (startOffset != null)
            {
                offset = startOffset;
            }

            String lastSourceId = null;
            var sb = new StringBuilder();
            for (var text : code)
            {
                var source = text.getSource();
                var sourceId = source.getId();
                attchSourceIdToMethod(sourceId, source.getMethod());
                if (!sourceId.equals(lastSourceId))
                {
                    if (sb.length() > 0)
                    {
                        lastAcceptedSourceId = lastSourceId;
                        feedbackService.acceptedCodeAsync(lastSourceId, sb.toString(),
                            cursorInfoProvider.getCursorInfo(offset),
                            cursorInfoProvider.getCursorInfo(offset + sb.length()));
                        offset += sb.length();
                        sb.setLength(0);
                    }

                    lastSourceId = sourceId;
                }

                sb.append(text.getText());
            }

            if (sb.length() > 0)
            {
                lastAcceptedSourceId = lastSourceId;
                feedbackService.acceptedCodeAsync(lastSourceId, sb.toString(), cursorInfoProvider.getCursorInfo(offset),
                    cursorInfoProvider.getCursorInfo(offset + sb.length()));
            }
        }
        finally
        {
            code.clear();
            startOffset = null;
        }
    }

    @Override
    public synchronized <T> void addMethod(CodeMethod method, T state,
        Function<? super T, ? extends String> methodBodyProvider)
    {
        var sourceIds = methods.remove(method);
        if (sourceIds == null || sourceIds.isEmpty())
        {
            return;
        }

        var body = methodBodyProvider.apply(state);
        if (body == null || body.isBlank())
        {
            return;
        }

        for (var sourceId : sourceIds)
        {
            feedbackService.finalizeCodeAsync(sourceId, body);
        }
    }

    @Override
    public synchronized Optional<String> getLastAcceptedSourceId()
    {
        return Optional.ofNullable(lastAcceptedSourceId);
    }

    private void attchSourceIdToMethod(String sourceId, CodeMethod method)
    {
        methods.computeIfAbsent(method, k -> new HashSet<>()).add(sourceId);
    }
}