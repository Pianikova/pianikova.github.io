/*
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.List;
import java.util.function.LongSupplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

import com.e1c.edt.ai.IConversationProgressListener;
import com.google.common.base.Preconditions;

/**
 * Shows what a conversation is doing right now in the task name of an Eclipse job.
 * <p>
 * There is no total to report against — the model decides how many tool rounds it needs — so this
 * reports liveness, not completion: the current round and how much of the answer has arrived. Both
 * grow visibly while the request is alive, which is what tells the user it has not hung.
 * <p>
 * The text goes into the task name rather than a sub-task: the EDT progress view renders the job
 * name line and the progress bar, but not sub-task lines, so a sub-task would stay invisible. The
 * bar itself appears only after {@code beginTask}, which the job body calls before the first report.
 * <p>
 * Updates are throttled. The listener is called once per streamed delta, that is per token, while
 * {@link IProgressMonitor#setTaskName} formats a string and notifies the progress UI — reporting
 * every delta would spend more time drawing than working.
 *
 * @author Skill Test
 */
public class ConversationProgressReporter
    implements IConversationProgressListener
{
    /** Fast enough to look live, rare enough not to flood the progress UI. */
    private static final long MIN_INTERVAL_NANOS = 300_000_000L;

    private final IProgressMonitor monitor;
    private final LongSupplier nanoTime;
    private long lastReportNanos;
    private boolean reported;

    /**
     * @param monitor monitor of the job the conversation runs in, cannot be {@code null}
     */
    public ConversationProgressReporter(IProgressMonitor monitor)
    {
        this(monitor, System::nanoTime);
    }

    /**
     * @param monitor monitor of the job the conversation runs in, cannot be {@code null}
     * @param nanoTime source of the monotonic clock the throttling is based on, cannot be
     * {@code null}; exists to make the throttling testable
     */
    public ConversationProgressReporter(IProgressMonitor monitor, LongSupplier nanoTime)
    {
        Preconditions.checkNotNull(monitor);
        Preconditions.checkNotNull(nanoTime);
        this.monitor = monitor;
        this.nanoTime = nanoTime;
    }

    @Override
    public void onProgress(int round, int charactersReceived)
    {
        var now = nanoTime.getAsLong();
        if (reported && now - lastReportNanos < MIN_INTERVAL_NANOS)
        {
            return;
        }

        lastReportNanos = now;
        reported = true;
        monitor.setTaskName(NLS.bind(Messages.ConversationProgressTaskName, Integer.valueOf(round),
            Integer.valueOf(charactersReceived)));
    }

    /**
     * Shows which tool(s) are about to run, unthrottled — this fires once per round rather than once
     * per streamed token, so there is no flood to guard against, and the name should switch the moment
     * the round moves from "receiving text" to "waiting on a tool".
     */
    @Override
    public void onToolCallStart(List<String> toolNames)
    {
        monitor.setTaskName(NLS.bind(Messages.ConversationProgressToolCall, String.join(", ", toolNames))); //$NON-NLS-1$
    }
}
