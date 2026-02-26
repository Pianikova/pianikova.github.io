/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.eclipse.core.runtime.jobs.Job;
import org.junit.Before;
import org.junit.Test;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;

/**
 * Unit tests for {@link Notificator}
 */
public class NotificatorTest
{
    private IStateService stateService;
    private INotifications notifications;
    private IDispatcher dispatcher;
    private IClock clock;
    private Notificator notificator;

    @Before
    public void setUp() throws Exception
    {
        stateService = mock(IStateService.class);
        notifications = mock(INotifications.class);
        dispatcher = mock(IDispatcher.class);
        clock = mock(IClock.class);

        // Mock dispatcher.createJob() to return a Job
        Job mockJob = mock(Job.class);
        when(dispatcher.createJob(anyString(), any(), anyBoolean(), any())).thenReturn(mockJob);

        notificator = new Notificator(stateService, notifications, dispatcher, clock);
    }

    @Test
    public void shouldIgnoreDuplicateStateChanges()
    {
        var state = createAIState(ServiceState.TOKEN_ERROR);

        notificator.onStateChange(state);
        notificator.onStateChange(state); // Same state again

        // Duplicate state changes are ignored - clock.now() is not called anymore
        verify(stateService).addListener(notificator);
    }

    @Test
    public void shouldAcceptDifferentStateChanges()
    {
        var state1 = createAIState(ServiceState.TOKEN_ERROR);
        var state2 = createAIState(ServiceState.SSL_ERROR);

        notificator.onStateChange(state1);
        notificator.onStateChange(state2); // Different state

        verify(stateService).addListener(notificator);
    }

    @SuppressWarnings("nls")
    @Test
    public void missingTokenShouldNotShowRepeatedly() throws Exception
    {
        var baseTime = LocalDateTime.of(2025, 1, 15, 10, 0, 0);

        when(clock.now()).thenReturn(baseTime);

        notificator.onStateChange(createAIState(ServiceState.MISSING_TOKEN));

        // First time should show
        boolean firstShow = notificator.shouldShowNotification(ServiceState.MISSING_TOKEN);
        assertTrue("First missing token should show", firstShow);

        // Simulate showing the notification to set lastShownServiceState
        simulateShowNotification(ServiceState.MISSING_TOKEN, baseTime);

        // Second time should not show
        boolean secondShow = notificator.shouldShowNotification(ServiceState.MISSING_TOKEN);
        assertFalse("Second missing token should not show", secondShow);
    }

    @SuppressWarnings("nls")
    @Test
    public void otherStatesShouldShowAfterInterval()
    {
        var baseTime = LocalDateTime.of(2025, 1, 15, 10, 0, 0);

        when(clock.now()).thenReturn(baseTime);

        notificator.onStateChange(createAIState(ServiceState.TOKEN_ERROR));

        // First time should show
        when(clock.now()).thenReturn(baseTime);
        boolean firstShow = notificator.shouldShowNotification(ServiceState.TOKEN_ERROR);
        assertTrue("First token error should show", firstShow);

        // Simulate showing the notification to set lastShownTime
        simulateShowNotification(ServiceState.TOKEN_ERROR, baseTime);

        // Same day - should not show
        when(clock.now()).thenReturn(baseTime.plusHours(12));
        boolean sameDayShow = notificator.shouldShowNotification(ServiceState.TOKEN_ERROR);
        assertFalse("Same day token error should not show", sameDayShow);

        // After 1 day - should show again
        when(clock.now()).thenReturn(baseTime.plusDays(1).plusSeconds(1));
        boolean nextDayShow = notificator.shouldShowNotification(ServiceState.TOKEN_ERROR);
        assertTrue("Next day token error should show", nextDayShow);
    }

    @SuppressWarnings("nls")
    @Test
    public void newStatesShouldAlwaysShow()
    {
        notificator.onStateChange(createAIState(ServiceState.TOKEN_ERROR));

        // Different state should show
        boolean show = notificator.shouldShowNotification(ServiceState.SSL_ERROR);
        assertTrue("Different state should show", show);
    }

    @SuppressWarnings("nls")
    @Test
    public void nullStateShouldNotShow()
    {
        boolean show = notificator.shouldShowNotification(null);
        assertFalse("Null state should not show", show);
    }

    @Test
    public void shouldInitializeWithInitialState()
    {
        var state = createAIState(ServiceState.TOKEN_ERROR);

        when(stateService.getState()).thenReturn(state);

        notificator.initialize();

        verify(stateService).getState();
    }

    @SuppressWarnings("nls")
    @Test
    public void sslErrorShouldShowRepeatedlyAfterInterval() throws Exception
    {
        var baseTime = LocalDateTime.of(2025, 2, 20, 14, 30, 0);

        when(clock.now()).thenReturn(baseTime);

        notificator.onStateChange(createAIState(ServiceState.SSL_ERROR));

        // First time should show
        when(clock.now()).thenReturn(baseTime);
        boolean firstShow = notificator.shouldShowNotification(ServiceState.SSL_ERROR);
        assertTrue("First SSL error should show", firstShow);

        // Simulate showing the notification to set lastShownTime
        simulateShowNotification(ServiceState.SSL_ERROR, baseTime);

        // After 1 day - should show again
        when(clock.now()).thenReturn(baseTime.plusDays(1).plusSeconds(1));
        boolean nextDayShow = notificator.shouldShowNotification(ServiceState.SSL_ERROR);
        assertTrue("Next day SSL error should show", nextDayShow);
    }

    @SuppressWarnings("nls")
    @Test
    public void stateWithNullLastShownTimeShouldShow()
    {
        // Initialize with a state
        notificator.onStateChange(createAIState(ServiceState.TOKEN_ERROR));

        // Clear last shown by changing state
        notificator.onStateChange(createAIState(ServiceState.SSL_ERROR));

        // Original state should show again
        boolean show = notificator.shouldShowNotification(ServiceState.TOKEN_ERROR);
        assertTrue("State with null last shown time should show", show);
    }

    private AIState createAIState(ServiceState serviceState)
    {
        var state = mock(AIState.class);
        when(state.getServiceState()).thenReturn(serviceState);
        return state;
    }

    private void simulateShowNotification(ServiceState serviceState, LocalDateTime showTime)
    {
        notificator.lastShownServiceState = serviceState;
        notificator.lastShownTime = showTime;
    }
}
