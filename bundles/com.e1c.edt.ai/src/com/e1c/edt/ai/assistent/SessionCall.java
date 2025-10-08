/**
 *
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.Session;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

public class SessionCall
    implements ISessionCall
{
    private static final AIState STATE_CHANGED = new AIState(ServiceState.SETTINGS_CHANGED, ActionState.INACTIVE);
    private final ILog log;
    private final IHttpLog httpLog;
    private final IAIStateListener stateListener;
    private final ISessionService sessionService;
    private final IStateService stateService;

    @Inject
    public SessionCall(ILog log, IHttpLog httpLog, IAIStateListener stateListener,
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

    @SuppressWarnings("nls")
    private <T> void callInternal(ProjectId projectId, ICancellationToken cancellationToken,
        Function<Optional<Session>, CompletableFuture<HttpResponse<T>>> taskSupplier,
        CompletableFuture<HttpResponse<T>> result)
    {
        var attachToken = CancellationTokenSource.attach(cancellationToken, () -> result.cancel(true));
        var stopwatch = Stopwatch.createStarted();
        stateService.setState(CodeAssistant.class.getName(), ActionState.BUSY);
        sessionService.getSessionAsync(projectId).thenCompose(session -> {
            return taskSupplier.apply(session).whenComplete((response, throwable) -> {
                if (throwable == null)
                {
                    httpLog.response(response, cancellationToken.toString(), stopwatch, true, false);
                    var statusCode = response.statusCode();
                    if (statusCode >= 400 && statusCode < 500)
                    {
                        stateListener.onStateChange(STATE_CHANGED);
                        log.trace("ApiCallRepeater",
                            () -> "Retrying request due to unexpected response status code: " + statusCode);
                        sessionService.getSessionAsync(projectId).thenCompose(newSesssion -> {
                            return taskSupplier.apply(session);
                        }).whenComplete((anotherOneResponse, error) -> {
                            if (error == null)
                            {
                                httpLog.response(anotherOneResponse, cancellationToken.toString(), stopwatch, true,
                                    true);
                                result.complete(anotherOneResponse);
                            }
                            else
                            {
                                result.completeExceptionally(error);
                            }
                        });
                    }
                    else
                    {
                        result.complete(response);
                    }
                }
                else
                {
                    result.completeExceptionally(
                        throwable instanceof CompletionException ? throwable.getCause() : throwable);
                }
            });
        })
            .whenComplete((r, error) -> {
                stateService.setState(CodeAssistant.class.getName(), ActionState.INACTIVE);
                if (!isCancellationException(error))
                {
                    httpLog.error(error, cancellationToken.toString());
                }

                try
                {
                    attachToken.close();
                }
                catch (Exception ex)
                {
                    //
                }

                if (error != null)
                {
                    result.completeExceptionally(error);
                }
            });
    }

    private boolean isCancellationException(Throwable error)
    {
        return error instanceof CompletionException
            && ((CompletionException)error).getCause() instanceof CancellationException;
    }
}
