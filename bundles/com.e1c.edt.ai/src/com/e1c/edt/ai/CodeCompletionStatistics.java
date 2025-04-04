/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

import com.e1c.edt.ai.assistent.IFeedbackService;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CodeCompletionStatistics
    implements ICodeCompletionContext, ICodeCompletionStatistics
{
    private static final int MAX_SIZE = 1024;
    private final ILog log;
    private final IFeedbackService feedbackService;
    private final ICursorInfoProvider cursorInfoProvider;
    private final ArrayList<Text> code = new ArrayList<>();
    private final HashMap<CodeMethod, HashSet<String>> methods = new HashMap<>();
    private String lastAcceptedSourceId = null;
    private Integer startOffset;

    @Inject
    public CodeCompletionStatistics(ILog log, IFeedbackService feedbackService, ICursorInfoProvider cursorInfoProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(feedbackService);
        Preconditions.checkNotNull(cursorInfoProvider);
        this.log = log;
        this.feedbackService = feedbackService;
        this.cursorInfoProvider = cursorInfoProvider;
    }

    @Override
    public synchronized void apply(Text text, int offset)
    {
        Preconditions.checkNotNull(text);
        if (code.size() > MAX_SIZE)
        {
            code.clear();
        }

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
    public void commit(String lastSourceId, int lastOffset)
    {
        try
        {
            if (lastSourceId == null || lastSourceId.equals(lastAcceptedSourceId))
            {
                return;
            }

            if (code.isEmpty() && lastOffset >= 0 && !lastSourceId.isBlank())
            {
                lastAcceptedSourceId = lastSourceId;
                var cursorInfo = cursorInfoProvider.getCursorInfo(lastOffset);
                feedbackService.acceptedCodeAsync(lastSourceId, "", cursorInfo, cursorInfo); //$NON-NLS-1$
                return;
            }

            var offset = 0;
            if (startOffset != null)
            {
                offset = startOffset;
            }

            lastSourceId = null;
            var sb = new StringBuilder();
            for (var text : code)
            {
                var source = text.getSource();
                var sourceId = source.getId();
                attachSourceIdToMethod(sourceId, source.getMethod());
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
            log.debug("Statistics", () -> "Source ids are empty."); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        var body = methodBodyProvider.apply(state);
        if (body == null || body.isBlank())
        {
            log.debug("Statistics", () -> "Method body is empty."); //$NON-NLS-1$ //$NON-NLS-2$
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

    private void attachSourceIdToMethod(String sourceId, CodeMethod method)
    {
        if (methods.size() > MAX_SIZE)
        {
            methods.clear();
        }

        if (methods.computeIfAbsent(method, k -> new HashSet<>()).add(sourceId))
        {
            log.debug("Statistics", () -> "Add " + sourceId + " for " + method.getUniqueName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}