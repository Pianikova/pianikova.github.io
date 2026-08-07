/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import java.util.List;

/**
 * Receives liveness updates while a conversation is being answered.
 * <p>
 * A conversation with tools has no known length — the model decides how many tool rounds it needs,
 * and {@code maxToolRounds} is a safety cap rather than an estimate. So there is no total to report
 * against: these updates say what is happening now, not how much is left.
 * <p>
 * Called on the thread that delivers the response stream, once per assistant message start and once
 * per content delta — that is, very often. An implementation must be cheap and must not block; any
 * throttling belongs to the implementation.
 *
 * @author Skill Test
 */
public interface IConversationProgressListener
{
    /**
     * Reports the state of the answer being received.
     *
     * @param round 1-based number of the assistant message currently being received; it grows by one
     * per tool round, so it doubles as the round counter
     * @param charactersReceived number of content characters accumulated for that message so far,
     * reasoning excluded
     */
    void onProgress(int round, int charactersReceived);

    /**
     * Reports that a batch of tool calls for the current round is about to run.
     * <p>
     * Called once per round, right before the batch is dispatched, with every tool name requested in
     * that round (a round can request several tools in parallel). Default no-op so existing listeners
     * that only care about {@link #onProgress} keep compiling.
     *
     * @param toolNames names of the tools about to run, never {@code null}, never empty
     */
    default void onToolCallStart(List<String> toolNames)
    {
        // No-op by default: subclasses that only care about {@link #onProgress} do not have to implement this.
    }

    /**
     * Reports that the batch of tool calls reported by the matching {@link #onToolCallStart} has
     * finished, successfully or not — this is a liveness signal, not a result.
     *
     * @param toolNames same names passed to the matching {@link #onToolCallStart}, never {@code null},
     * never empty
     */
    default void onToolCallEnd(List<String> toolNames)
    {
        // No-op by default: subclasses that only care about {@link #onProgress} do not have to implement this.
    }
}
