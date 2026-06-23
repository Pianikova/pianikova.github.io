/*
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.google.inject.Singleton;

/**
 * Thread-safe {@link IDevToolCallRecorder}. The dev-autopilot processes one turn at a time,
 * so a single active capture list is sufficient; tool calls running on worker threads append
 * into the synchronized list while a run is active.
 */
@Singleton
public class DevToolCallRecorder
    implements IDevToolCallRecorder
{
    private final AtomicReference<List<DevToolCall>> active = new AtomicReference<>();

    @Override
    public void beginRun()
    {
        active.set(Collections.synchronizedList(new ArrayList<>()));
    }

    @Override
    public void recordCall(String toolName, String argumentsJson, String resultContent, String errorType)
    {
        var list = active.get();
        if (list == null)
        {
            return;
        }

        var call = new DevToolCall();
        call.tool = toolName;
        call.arguments = argumentsJson;
        call.result = resultContent;
        call.error = errorType;
        list.add(call);
    }

    @Override
    public List<DevToolCall> endRun()
    {
        var list = active.getAndSet(null);
        if (list == null)
        {
            return List.of();
        }

        synchronized (list)
        {
            return new ArrayList<>(list);
        }
    }
}
