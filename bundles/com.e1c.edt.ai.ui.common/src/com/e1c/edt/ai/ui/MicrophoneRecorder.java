/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;

/**
 * Captures audio from the microphone and streams Base64 PCM chunks to JavaScript
 * via the WebKit bridge.
 * <p>
 * JS contract:
 *   window.onVoiceChunk(base64, durationMs) — called every ~1s with PCM audio chunk
 *   window.onVoiceStateChange(state) — called with 'recording' or 'stopped'
 *   window.onVoiceError(message) — called on errors
 */
public class MicrophoneRecorder
{
    private static final float SAMPLE_RATE = 16000f;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    private static final int CHUNK_DURATION_MS = 1000;
    private static final int BYTES_PER_CHUNK =
        (int)(SAMPLE_RATE * (SAMPLE_SIZE_BITS / 8) * CHANNELS * CHUNK_DURATION_MS / 1000);

    private final WebEngine webEngine;
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private TargetDataLine microphone;
    private Thread captureThread;

    public MicrophoneRecorder(WebEngine webEngine)
    {
        this.webEngine = webEngine;
    }

    /**
     * Starts recording from the default microphone.
     * If already recording, does nothing.
     */
    public void startVoiceRecording()
    {
        if (recording.getAndSet(true))
        {
            return;
        }

        try
        {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info))
            {
                callJs("onVoiceError", "'Microphone not available or format not supported'"); //$NON-NLS-1$ //$NON-NLS-2$
                recording.set(false);
                return;
            }

            microphone = (TargetDataLine)AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            callJs("onVoiceStateChange", "'recording'"); //$NON-NLS-1$ //$NON-NLS-2$

            captureThread = new Thread(this::captureLoop, "voice-capture"); //$NON-NLS-1$
            captureThread.setDaemon(true);
            captureThread.start();
        }
        catch (LineUnavailableException e)
        {
            callJs("onVoiceError", "'" + escapeJs(e.getMessage()) + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            recording.set(false);
        }
    }

    /**
     * Stops recording, sends last chunk, closes audio line.
     *
     * @return "stopped" for JS compatibility
     */
    public String stopVoiceRecording()
    {
        recording.set(false);

        if (microphone != null)
        {
            microphone.stop();
            microphone.close();
            microphone = null;
        }

        if (captureThread != null)
        {
            try
            {
                captureThread.join(2000);
            }
            catch (InterruptedException ignored)
            {
                Thread.currentThread().interrupt();
            }
            captureThread = null;
        }

        callJs("onVoiceStateChange", "'stopped'"); //$NON-NLS-1$ //$NON-NLS-2$
        return "stopped"; //$NON-NLS-1$
    }

    /**
     * @return {@code true} if currently recording
     */
    public boolean isRecording()
    {
        return recording.get();
    }

    private void captureLoop()
    {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();
        long chunkStartTime = System.currentTimeMillis();

        while (recording.get() && microphone != null && microphone.isOpen())
        {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            if (bytesRead <= 0)
            {
                continue;
            }

            chunkBuffer.write(buffer, 0, bytesRead);

            if (chunkBuffer.size() >= BYTES_PER_CHUNK)
            {
                long durationMs = System.currentTimeMillis() - chunkStartTime;
                byte[] pcmData = chunkBuffer.toByteArray();
                String base64 = Base64.getEncoder().encodeToString(pcmData);
                callJs("onVoiceChunk", "'" + base64 + "', " + durationMs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                chunkBuffer.reset();
                chunkStartTime = System.currentTimeMillis();
            }
        }

        // Send remaining data as the last chunk
        if (chunkBuffer.size() > 0)
        {
            long durationMs = System.currentTimeMillis() - chunkStartTime;
            byte[] pcmData = chunkBuffer.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(pcmData);
            callJs("onVoiceChunk", "'" + base64 + "', " + durationMs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    @SuppressWarnings("nls")
    private void callJs(String functionName, String args)
    {
        Platform.runLater(() -> {
            try
            {
                webEngine.executeScript("if(window." + functionName + ") window." + functionName + "(" + args + ")");
            }
            catch (Exception e)
            {
                System.err.println("[MicrophoneRecorder] JS call failed: " + e.getMessage());
            }
        });
    }

    private static String escapeJs(String s)
    {
        if (s == null)
        {
            return ""; //$NON-NLS-1$
        }
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
    }
}
