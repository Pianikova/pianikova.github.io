/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import javax.net.ssl.SSLException;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ITraceScenario;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.TraceScenarioType;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.Session;
import com.e1c.edt.ai.assistent.model.SessionRequest;
import com.e1c.edt.ai.assistent.model.SystemInfo;
import com.e1c.edt.ai.assistent.model.UserParameters;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

class SessionService
    implements ISessionService
{
    private final IHttpLog log;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clientBuilder;
    private final IJson json;
    private final ISettingsTracker settingsTracker;
    private final IResponseCache responseCache;
    private final IVersionProvider versionProvider;
    private final ISettings settings;
    private final ISettingsSetter settingsSetter;
    private final IEnvironment environment;
    private final IConfigurationParametersProvider configurationParametersProvider;
    private final IStateService stateService;
    private final ITraceScenario traceScenario;
    private final Object sessionChainLock = new Object();
    private CompletableFuture<?> sessionChainTail = CompletableFuture.completedFuture(null);

    @Inject
    public SessionService(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder, IJson json,
        ISettingsTracker settingsTracker, IResponseCache responseCache, IVersionProvider versionProvider,
        ISettings settings, ISettingsSetter settingsSetter, IEnvironment environment,
        IConfigurationParametersProvider configurationParametersProvider, IStateService stateService,
        ITraceScenario traceScenario)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(settingsTracker);
        Preconditions.checkNotNull(responseCache);
        Preconditions.checkNotNull(versionProvider);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(settingsSetter);
        Preconditions.checkNotNull(environment);
        Preconditions.checkNotNull(configurationParametersProvider);
        Preconditions.checkNotNull(stateService);
        Preconditions.checkNotNull(traceScenario);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clientBuilder = clientBuilder;
        this.json = json;
        this.settingsTracker = settingsTracker;
        this.responseCache = responseCache;
        this.versionProvider = versionProvider;
        this.settings = settings;
        this.settingsSetter = settingsSetter;
        this.environment = environment;
        this.configurationParametersProvider = configurationParametersProvider;
        this.stateService = stateService;
        this.traceScenario = traceScenario;
    }

    @Override
    public CompletableFuture<Optional<Session>> getSessionAsync(IProject project)
    {
        Preconditions.checkNotNull(project);
        var reset = settingsTracker.register(SessionService.class.getName(), settings.getUserParameters());
        return responseCache.get(project, () -> getSession(project), reset);
    }

    @Override
    public CompletableFuture<Optional<Session>> getGlobalSessionAsync()
    {
        var reset = settingsTracker.register(SessionService.class.getName(), settings.getUserParameters());
        return responseCache.getGlobal(() -> getSession(null), reset);
    }

    private CompletableFuture<Optional<Session>> getSession(IProject project)
    {
        var builder = requestBuilder.create(settings.getUrl() + "api/v1/create_session"); //$NON-NLS-1$
        if (builder.isEmpty())
        {
            // Malformed service URL / unbuildable request is a configuration error a retry cannot fix: fail fast so
            // SessionCall surfaces it instead of looping through the null-sessionId backoff.
            return CompletableFuture.failedFuture(
                new AIClientException("create_session request could not be built (check service URL)", null)); //$NON-NLS-1$
        }

        var sessionRequest = new SessionRequest();
        var userParams = settings.getUserParameters();
        sessionRequest.serviceParameters = userParams;
        var userParameters = new UserParameters();
        sessionRequest.userParameters = userParameters;
        var pluginVersion = versionProvider.getPluginVersion();
        if (pluginVersion != null)
        {
            userParameters.pluginVersion = pluginVersion.toString();
        }

        var platformVersion = versionProvider.getPlatformVersion();
        if (platformVersion != null && !platformVersion.isBlank())
        {
            userParameters.edtVersion = platformVersion;
        }

        userParameters.tabWidth = settings.getTabWidth();
        userParameters.codeCompletionLinesCount = settings.getCodeCompletionLinesCount();
        userParameters.codeCompletionPolicy = settings.getCodeCompletionPolicy();
        userParameters.isContinuousCodeCompletion =
            CodeCompletionPolicy.MODERATE.isMeet(userParameters.codeCompletionPolicy);
        userParameters.minRequestDelayMs = settings.getMinRequestDelay().toMillis();
        userParameters.timeoutMs = settings.getTimeout().toMillis();
        userParameters.lineSeparator = settings.getLineSeparator();
        userParameters.language = settings.getLanguage();
        userParameters.configurationParameters = project == null ? null
            : configurationParametersProvider.getParameters(project).orElse(null);
        userParameters.globalContext = userParams.globalContext;
        userParameters.experimental = userParams.experimental;

        var systemInfo = new SystemInfo();
        sessionRequest.systemInfo = systemInfo;
        systemInfo.osName = environment.getOSName();
        systemInfo.osVersion = environment.getOSVersion();
        systemInfo.arch = environment.getArch();
        systemInfo.availableProcessors = environment.getAvailableProcessors();
        environment.getProcessorName().ifPresent(val -> systemInfo.processorName = val);
        environment.getTotalPhysicalMemorySize().ifPresent(val -> systemInfo.totalPhysicalMemorySize = val);

        var requestBody = json.serialize(sessionRequest);
        var requestBuilder = builder.get();
        var instanceType = settings.getInstanceType();
        if (instanceType.isPresent())
        {
            requestBuilder = requestBuilder.header("Instance-Type", instanceType.get()); //$NON-NLS-1$
        }

        requestBuilder = requestBuilder.POST(BodyPublishers.ofString(requestBody));
        var request = requestBuilder.build();
        return chainSessionRequest(() -> getSessionAsync(project, request, requestBody));
    }

    /**
     * Serializes the actual create_session sends under the same client token. The server returns HTTP 423 with
     * {@code error_type "locked"} ("Token is busy right now") for create_session calls that overlap on one token;
     * the client then mistakes the body for a missing session and retries in a storm. Running the sends one at a
     * time keeps the server from ever seeing concurrent calls. Each project still gets its own session — this only
     * orders the sends, it does not share a session across projects.
     */
    private CompletableFuture<Optional<Session>> chainSessionRequest(
        Supplier<CompletableFuture<Optional<Session>>> task)
    {
        synchronized (sessionChainLock)
        {
            // handle() swallows the previous request's outcome so a failed/timed-out create_session never blocks the
            // next one; the chain advances regardless of success or error.
            var result = sessionChainTail.handle((r, e) -> (Void)null).thenCompose(ignored -> task.get());
            sessionChainTail = result;
            return result;
        }
    }

    private CompletableFuture<Optional<Session>> getSessionAsync(IProject project, HttpRequest request, String body)
    {
        if (traceScenario.getActive() == TraceScenarioType.SSL_ERROR)
        {
            var sslException = new javax.net.ssl.SSLHandshakeException("Simulated SSL handshake error for testing"); //$NON-NLS-1$
            stateService.setState(ServiceState.SSL_ERROR);
            return CompletableFuture.failedFuture(sslException);
        }

        log.request(request, null, body);
        var stopwatch = Stopwatch.createStarted();
        var busyToken = stateService.busy();
        return clientBuilder.get()
            .sendAsync(request, BodyHandlers.ofString())
            // Run post-response work asynchronously so it never executes on the shared HttpClient's response-callback
            // threads: createCession -> Settings.applySessionParameters takes a global lock and busyToken.close() does
            // a blocking Display.syncExec on the UI thread. Running those inline starves the shared client's worker
            // threads and stalls every other in-flight request (the null sessionId + TimeoutException storm). The chat
            // path in Conversations already uses thenApplyAsync for the same reason.
            .thenApplyAsync(response -> log.response(response, null, stopwatch, true, true))
            .thenApplyAsync(response -> createSession(project, response))
            .whenCompleteAsync((session, error) -> {
                try
                {
                    busyToken.close();
                }
                catch (Exception e)
                {
                    //
                }
                finally
                {
                    if (error != null)
                    {
                        var actualError = error instanceof CompletionException ? error.getCause() : error;
                        if (actualError instanceof SSLException)
                        {
                            stateService.setState(ServiceState.SSL_ERROR);
                        }
                    }
                }
            });
    }

    @SuppressWarnings("nls")
    private Optional<Session> createSession(IProject project, HttpResponse<String> response)
    {
        var content = response.body();
        var status = response.statusCode();
        var session = json.deserialize(content, Session.class);
        if (session.isEmpty() || session.get().sessionId == null)
        {
            // A null sessionId triggers the retry storm in SessionCall but used to be silent here, so it was
            // indistinguishable from a timeout. Log the status code and the (small) body verbatim so we can tell
            // apart a throttle/error envelope from a genuine empty/200 response the server returns for concurrent
            // create_session calls.
            log.error("create_session response without sessionId (status: " + status + ", parsed: "
                + session.isPresent() + ", body: " + abbreviate(content) + ")", null);

            if (isFatalStatus(status))
            {
                // Auth/bad-request errors will not change on retry: fail the future so SessionCall fails fast and
                // surfaces the real error, instead of looping the null-sessionId backoff. Transient causes (423
                // "locked", 429, 408, 5xx, empty/200) fall through to an empty Optional, which SessionCall retries.
                throw new AIClientException("create_session failed: HTTP " + status, status, abbreviate(content), null);
            }

            return session;
        }

        if (project == null)
        {
            settingsSetter.applyGlobalSessionParameters(session.get().userParameters);
        }
        else
        {
            settingsSetter.applySessionParameters(project, session.get().userParameters);
        }
        return session;
    }

    private static boolean isFatalStatus(int status)
    {
        // 4xx client errors a retry will not fix (bad token/auth/request), excluding known transient ones:
        // 423 locked (token busy), 429 rate limit, 408 request timeout.
        return status >= 400 && status < 500 && status != 423 && status != 429 && status != 408;
    }

    @SuppressWarnings("nls")
    private static String abbreviate(String content)
    {
        if (content == null)
        {
            return "<null>";
        }

        var oneLine = content.replaceAll("\\s+", " ").strip();
        return oneLine.length() > 512 ? oneLine.substring(0, 512) + "..." : oneLine;
    }
}
