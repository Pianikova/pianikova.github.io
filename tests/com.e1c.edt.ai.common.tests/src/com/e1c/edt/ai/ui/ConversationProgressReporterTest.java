/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

/**
 * Tests for {@link ConversationProgressReporter}.
 */
@SuppressWarnings("nls")
public class ConversationProgressReporterTest
{
    private static final long INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(300);

    private final List<String> taskNames = new ArrayList<>();
    private final NullProgressMonitor monitor = new NullProgressMonitor()
    {
        @Override
        public void setTaskName(String name)
        {
            taskNames.add(name);
        }
    };
    private long now;

    private ConversationProgressReporter reporter()
    {
        return new ConversationProgressReporter(monitor, () -> now);
    }

    @Test
    public void reportsTheFirstUpdateImmediately()
    {
        reporter().onProgress(1, 0);

        assertEquals(1, taskNames.size());
        assertTrue(taskNames.get(0), taskNames.get(0).contains("1"));
    }

    @Test
    public void showsTheRoundAndTheCharacterCount()
    {
        reporter().onProgress(3, 1240);

        assertTrue(taskNames.get(0), taskNames.get(0).contains("3") && taskNames.get(0).contains("1240"));
    }

    @Test
    public void throttlesUpdatesWithinTheInterval()
    {
        var reporter = reporter();

        reporter.onProgress(1, 10);
        now += INTERVAL_NANOS / 3;
        reporter.onProgress(1, 20);
        now += INTERVAL_NANOS / 3;
        reporter.onProgress(1, 30);

        assertEquals(1, taskNames.size());
    }

    @Test
    public void reportsAgainAfterTheInterval()
    {
        var reporter = reporter();

        reporter.onProgress(1, 10);
        now += INTERVAL_NANOS;
        reporter.onProgress(2, 4000);

        assertEquals(2, taskNames.size());
        assertTrue(taskNames.get(1), taskNames.get(1).contains("4000"));
    }

    /** A stream that starts when nanoTime happens to be 0 must still report its first update. */
    @Test
    public void reportsTheFirstUpdateAtTimeZero()
    {
        now = 0;

        reporter().onProgress(1, 0);

        assertEquals(1, taskNames.size());
    }
}
