/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.TracingSources;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Captures audio from the default microphone and streams raw PCM chunks to a
 * {@link IMicrophoneRecorder.Listener}.
 * <p>
 * This class is concerned only with voice recording — it does not know how the audio is delivered.
 */
public class MicrophoneRecorder
    implements IMicrophoneRecorder
{
    private static final String VOICE = "Voice"; //$NON-NLS-1$

    private static final float SAMPLE_RATE = 16000f;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    private static final int CHUNK_DURATION_MS = 1000;
    private static final int BYTES_PER_CHUNK =
        (int)(SAMPLE_RATE * (SAMPLE_SIZE_BITS / 8) * CHANNELS * CHUNK_DURATION_MS / 1000);

    private final ILog log;
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private TargetDataLine microphone;
    private Thread captureThread;

    @Inject
    public MicrophoneRecorder(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @SuppressWarnings("nls")
    @Override
    public void start(Listener listener)
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("listener must not be null");
        }

        if (recording.getAndSet(true))
        {
            log.trace(TracingSources.CHAT, VOICE, () -> "start ignored: already recording");
            return;
        }

        log.trace(TracingSources.CHAT, VOICE,
            () -> "start: sampleRate=" + SAMPLE_RATE + " bits=" + SAMPLE_SIZE_BITS + " channels=" + CHANNELS
                + " bytesPerChunk=" + BYTES_PER_CHUNK);

        try
        {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info))
            {
                recording.set(false);
                log.trace(TracingSources.CHAT, VOICE, () -> "start failed: line not supported for format " + format);
                listener.onError("Microphone not available or format not supported");
                return;
            }

            microphone = (TargetDataLine)AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            log.trace(TracingSources.CHAT, VOICE, () -> "microphone line opened and started");

            listener.onStarted();

            captureThread = new Thread(() -> captureLoop(listener), "voice-capture");
            captureThread.setDaemon(true);
            captureThread.start();
        }
        catch (LineUnavailableException e)
        {
            recording.set(false);
            log.trace(TracingSources.CHAT, VOICE, () -> "start failed: line unavailable: " + e.getMessage());
            log.logError(e);
            listener.onError(e.getMessage());
        }
    }

    @SuppressWarnings("nls")
    @Override
    public void stop()
    {
        if (!recording.getAndSet(false))
        {
            log.trace(TracingSources.CHAT, VOICE, () -> "stop ignored: not recording");
            return;
        }

        log.trace(TracingSources.CHAT, VOICE, () -> "stop requested");

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
                boolean stillAlive = captureThread.isAlive();
                log.trace(TracingSources.CHAT, VOICE, () -> "capture thread joined, stillAlive=" + stillAlive);
            }
            catch (InterruptedException ignored)
            {
                Thread.currentThread().interrupt();
            }
            captureThread = null;
        }
    }

    @Override
    public boolean isRecording()
    {
        return recording.get();
    }

    @SuppressWarnings("nls")
    private void captureLoop(Listener listener)
    {
        log.trace(TracingSources.CHAT, VOICE, () -> "capture loop started");
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();
        long chunkStartTime = System.currentTimeMillis();
        long totalBytes = 0;
        int chunkCount = 0;

        TargetDataLine line;
        while (recording.get() && (line = microphone) != null && line.isOpen())
        {
            int bytesRead = line.read(buffer, 0, buffer.length);
            if (bytesRead <= 0)
            {
                log.trace(TracingSources.CHAT, VOICE, () -> "read returned no data: bytesRead=" + bytesRead);
                continue;
            }

            totalBytes += bytesRead;
            chunkBuffer.write(buffer, 0, bytesRead);

            if (chunkBuffer.size() >= BYTES_PER_CHUNK)
            {
                long durationMs = System.currentTimeMillis() - chunkStartTime;
                byte[] pcm = chunkBuffer.toByteArray();
                final int n = ++chunkCount;
                log.trace(TracingSources.CHAT, VOICE,
                    () -> "emit chunk #" + n + ": " + pcm.length + " bytes, durationMs=" + durationMs);
                listener.onChunk(pcm, durationMs);

                chunkBuffer.reset();
                chunkStartTime = System.currentTimeMillis();
            }
        }

        // Send remaining data as the last chunk
        if (chunkBuffer.size() > 0)
        {
            long durationMs = System.currentTimeMillis() - chunkStartTime;
            byte[] pcm = chunkBuffer.toByteArray();
            final int n = ++chunkCount;
            log.trace(TracingSources.CHAT, VOICE,
                () -> "emit final chunk #" + n + ": " + pcm.length + " bytes, durationMs=" + durationMs);
            listener.onChunk(pcm, durationMs);
        }

        final long total = totalBytes;
        final int chunks = chunkCount;
        log.trace(TracingSources.CHAT, VOICE,
            () -> "capture loop finished: totalBytes=" + total + " chunks=" + chunks);
        listener.onStopped();
    }
}
