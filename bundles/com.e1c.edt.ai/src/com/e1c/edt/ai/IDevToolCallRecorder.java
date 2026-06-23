/*
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Captures MCP tool calls executed during a single dev-autopilot turn so the resulting
 * transcript can be inspected by an external agent (see {@link IDevAutopilot}).
 * <p>
 * Recording is a no-op unless a run is active ({@link #beginRun()} called and not yet
 * {@link #endRun() ended}); in normal (non-dev) operation nothing is recorded.
 */
public interface IDevToolCallRecorder
{
    /**
     * Starts capturing tool calls for the current turn. Any previously active capture is discarded.
     */
    void beginRun();

    /**
     * Appends one tool call to the active capture. No-op when no run is active.
     *
     * @param toolName lower-case tool name, may be {@code null}
     * @param argumentsJson serialized tool arguments, may be {@code null}
     * @param resultContent tool response content, may be {@code null}
     * @param errorType error description when the call failed, otherwise {@code null}
     */
    void recordCall(String toolName, String argumentsJson, String resultContent, String errorType);

    /**
     * Ends the active capture and returns the recorded calls (empty list when no run was active).
     *
     * @return recorded calls, never {@code null}
     */
    List<DevToolCall> endRun();

    /**
     * Single recorded tool call (serialized into the autopilot transcript).
     */
    final class DevToolCall
    {
        @SerializedName("tool")
        public String tool;

        @SerializedName("arguments")
        public String arguments;

        @SerializedName("result")
        public String result;

        @SerializedName("error")
        public String error;
    }
}
