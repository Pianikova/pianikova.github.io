/*
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai;

/**
 * File-driven dev harness for feedback-loop testing of metadata/form/template scenarios.
 * <p>
 * When started (only in experimental mode), it watches an {@code inbox} directory for request
 * files, runs each prompt through the real assistant pipeline ({@link IConversationFacade}), and
 * writes a structured transcript into an {@code outbox} directory. An external agent drives the
 * loop purely through files — no UI, no recompilation, no EDT restart. See the dev-autopilot
 * README for the channel layout and JSON schema.
 */
public interface IDevAutopilot
{
    /**
     * Resolves the channel directory, creates {@code inbox}/{@code processing}/{@code outbox},
     * and starts the watcher thread. Idempotent.
     */
    void start();

    /**
     * Stops the watcher thread. Idempotent.
     */
    void stop();
}
