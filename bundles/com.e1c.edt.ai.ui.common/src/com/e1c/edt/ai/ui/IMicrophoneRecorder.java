/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

/**
 * Captures audio from the microphone and delivers raw PCM chunks to a {@link Listener}.
 * <p>
 * The recorder knows nothing about how the audio is consumed (web engine, files, etc.) — its sole
 * responsibility is to record the voice and emit chunks/state/error events.
 */
public interface IMicrophoneRecorder
{
    /**
     * Starts recording from the default microphone. If already recording, does nothing.
     *
     * @param listener receives audio chunks and state/error notifications, not {@code null}
     */
    void start(Listener listener);

    /**
     * Stops recording, delivers the last chunk and the {@link Listener#onStopped()} notification,
     * and releases the audio line. If not recording, does nothing.
     */
    void stop();

    /**
     * @return {@code true} if currently recording
     */
    boolean isRecording();

    /**
     * Receives recording events. Callbacks may be invoked on an internal capture thread.
     */
    interface Listener
    {
        /**
         * @param pcm raw PCM audio chunk (16 kHz, 16-bit, mono, little-endian), not {@code null}
         * @param durationMs duration of the chunk in milliseconds
         */
        void onChunk(byte[] pcm, long durationMs);

        /**
         * Called once when recording has started.
         */
        void onStarted();

        /**
         * Called once when recording has stopped.
         */
        void onStopped();

        /**
         * @param message human-readable error description, not {@code null}
         */
        void onError(String message);
    }
}
