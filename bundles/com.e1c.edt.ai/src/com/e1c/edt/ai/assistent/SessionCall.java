/**
 *
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.Session;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

public class SessionCall
    implements ISessionCall
{
    private static final AIState STATE_CHANGED = new AIState(ServiceState.SETTINGS_CHANGED, ActionState.INACTIVE);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_SECONDS = 5;
    private final ILog log;
    private final IHttpLog httpLog;
    private final IStateListener stateListener;
    private final ISessionService sessionService;
    private final IStateService stateService;

    @Inject
    public SessionCall(ILog log, IHttpLog httpLog, IStateListener stateListener,
        ISessionService sessionService,
        IStateService stateService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(httpLog);
        Preconditions.checkNotNull(stateListener);
        Preconditions.checkNotNull(sessionService);
        Preconditions.checkNotNull(stateService);
        this.log = log;
        this.httpLog = httpLog;
        this.stateListener = stateListener;
        this.sessionService = sessionService;
        this.stateService = stateService;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> call(ProjectId projectId, ICancellationToken cancellationToken,
        Function<Optional<Session>, CompletableFuture<HttpResponse<T>>> taskSupplier)
    {
        Preconditions.checkNotNull(taskSupplier);
        var result = new CompletableFuture<HttpResponse<T>>();
        callInternal(projectId, cancellationToken, taskSupplier, result);
        return result;
    }

    private <T> void callInternal(ProjectId projectId, ICancellationToken cancellationToken,
        Function<Optional<Session>, CompletableFuture<HttpResponse<T>>> taskSupplier,
        CompletableFuture<HttpResponse<T>> result)
    {
        var attachToken = CancellationTokenSource.attach(cancellationToken, () -> result.cancel(true));
        var stopwatch = Stopwatch.createStarted();
        var busyToken = stateService.busy();
        executeWithRetry(projectId, cancellationToken, taskSupplier, stopwatch, result, 0)
            .whenComplete((r, error) -> {
                try
                {
                    try
                    {
                        busyToken.close();
                    }
                    catch (Exception e)
                    {
                        //
                    }

                    if (error != null)
                    {
                        if (!isCancellationException(error))
                        {
                            httpLog.error(error, cancellationToken.toString());
                        }

                        if (!result.isDone())
                        {
                            result.completeExceptionally(error);
                        }
                    }
                }
                finally
                {
                    try
                    {
                        attachToken.close();
                    }
                    catch (Exception ex)
                    {
                        //
                    }
                }
            });
    }

    @SuppressWarnings("nls")
    private <T> CompletableFuture<HttpResponse<T>> executeWithRetry(ProjectId projectId,
        ICancellationToken cancellationToken,
        Function<Optional<Session>, CompletableFuture<HttpResponse<T>>> taskSupplier, Stopwatch stopwatch,
        CompletableFuture<HttpResponse<T>> result, int attemptCount)
    {
        return sessionService.getSessionAsync(projectId).thenCompose(session -> {
            return taskSupplier.apply(session).whenComplete((response, throwable) -> {
                if (throwable == null)
                {
                    var statusCode = response.statusCode();
                    if (statusCode >= 400 && statusCode < 500 && attemptCount < MAX_RETRY_ATTEMPTS)
                    {
                        int nextAttempt = attemptCount + 1;
                        long delaySeconds = calculateRetryDelay(attemptCount);

                        log.trace(TracingSources.API_CALLS, "ApiCallRepeater",
                            () -> "Retrying request due to unexpected response status code. Attempt: " + nextAttempt
                                + "/" + MAX_RETRY_ATTEMPTS + ", Delay: " + delaySeconds + "s");

                        CompletableFuture.delayedExecutor(delaySeconds, TimeUnit.SECONDS).execute(() -> {
                            executeWithRetry(projectId, cancellationToken, taskSupplier, stopwatch, result,
                                nextAttempt);
                        });
                    }
                    else
                    {
                        if (statusCode >= 400 && statusCode < 500)
                        {
                            stateListener.onStateChange(STATE_CHANGED);
                        }
                        httpLog.response(response, cancellationToken.toString(), stopwatch, true, attemptCount > 0);
                        result.complete(response);
                    }
                }
                else
                {
                    result.completeExceptionally(
                        throwable instanceof CompletionException ? throwable.getCause() : throwable);
                }
            });
        });
    }

    private long calculateRetryDelay(int attemptCount)
    {
        return INITIAL_RETRY_DELAY_SECONDS * (1L << attemptCount);
    }

    private boolean isCancellationException(Throwable error)
    {
        return error instanceof CompletionException
            && ((CompletionException)error).getCause() instanceof CancellationException;
    }
}
