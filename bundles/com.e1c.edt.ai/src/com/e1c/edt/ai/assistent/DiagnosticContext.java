/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILocalContext;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.ITraceScenario;
import com.e1c.edt.ai.IVersionProvider;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Bogdan Sushkov
 *
 */
public class DiagnosticContext
    implements IDiagnosticContext
{
    private final ISettings settings;
    private final ISessionService sessionService;
    private final IDiagnosticMapper mapper;
    private final ILocalContext context;
    private final Provider<IStatistics> statisticsProvider;
    private final IHttpClientBuilder httpClientBuilder;
    private final IRequestBuilder requestBuilder;
    private final ICACertificateReporter caCertificateReporter;
    private final IJson json;
    private final ITokenCheck tokenCheck;
    private final IEnvironment environment;
    private final IVersionProvider versionProvider;
    private final IConfigurationParametersProvider configurationParametersProvider;
    private final IHttpLog httpLog;
    private final ITraceScenario scenario;

    private String sessionId;
    private IProject project;
    private AIContext aiContext;
    private AtomicReference<String> caReport = new AtomicReference<>();

    @Inject
    public DiagnosticContext(ISettings settings, ISessionService sessionService, IDiagnosticMapper mapper,
        ILocalContext context, Provider<IStatistics> statisticsProvider, IJson json,
        IHttpClientBuilder httpClientBuilder, IRequestBuilder requestBuilder,
        ICACertificateReporter caCertificateReporter, ITokenCheck tokenCheck, IEnvironment environment,
        IVersionProvider versionProvider, IConfigurationParametersProvider configurationParametersProvider,
        IHttpLog httpLog, ITraceScenario scenario)
    {
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(sessionService);
        Preconditions.checkNotNull(mapper);
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(httpClientBuilder);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(caCertificateReporter);
        Preconditions.checkNotNull(tokenCheck);
        Preconditions.checkNotNull(environment);
        Preconditions.checkNotNull(versionProvider);
        Preconditions.checkNotNull(configurationParametersProvider);
        Preconditions.checkNotNull(httpLog);
        Preconditions.checkNotNull(scenario);

        this.configurationParametersProvider = configurationParametersProvider;
        this.versionProvider = versionProvider;
        this.environment = environment;
        this.tokenCheck = tokenCheck;
        this.caCertificateReporter = caCertificateReporter;
        this.httpClientBuilder = httpClientBuilder;
        this.requestBuilder = requestBuilder;
        this.json = json;
        this.statisticsProvider = statisticsProvider;
        this.context = context;
        this.settings = settings;
        this.sessionService = sessionService;
        this.mapper = mapper;
        this.httpLog = httpLog;
        this.scenario = scenario;
    }

    @Override
    public void setProject(IProject project)
    {
        this.project = project;
    }

    @Override
    public IProject getProject()
    {
        return project;
    }

    @Override
    public ISettings getSettings()
    {
        return settings;
    }

    @Override
    public ISessionService getSessionService()
    {
        return sessionService;
    }

    @Override
    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    @Override
    public String getSessionId()
    {
        return sessionId;
    }

    @Override
    public IDiagnosticMapper getDiagnosticMapper()
    {
        return mapper;
    }

    @Override
    public ILocalContext getLocalContext()
    {
        return context;
    }

    @Override
    public void setAIContext(AIContext context)
    {
        this.aiContext = context;
    }

    @Override
    public AIContext getAIContext()
    {
        return aiContext;
    }

    @Override
    public IStatistics getStatistics()
    {
        return statisticsProvider.get();
    }

    @Override
    public IJson getJson()
    {
        return json;
    }

    @Override
    public IHttpClientBuilder getHttpClientBuilder()
    {
        return httpClientBuilder;
    }

    @Override
    public IRequestBuilder getRequestBuilder()
    {
        return requestBuilder;
    }

    @Override
    public void releaseContext()
    {
        aiContext = null;
        project = null;
        sessionId = null;
    }

    @Override
    public ICACertificateReporter getCaCertificateReporter()
    {
        return caCertificateReporter;
    }

    @Override
    public ITokenCheck getTokenCheck()
    {
        return tokenCheck;
    }

    @Override
    public String getCAReport()
    {
        return caReport.get();
    }

    @Override
    public void setCaReportIfAbsent(String caReport)
    {
        if (this.caReport.get() == null)
        {
            this.caReport.set(caReport);
        }
    }

    @Override
    public IEnvironment getEnvironment()
    {
        return environment;
    }

    @Override
    public IConfigurationParametersProvider getConfigurationParametersProvider()
    {
        return configurationParametersProvider;
    }

    @Override
    public IVersionProvider getVersionProvider()
    {
        return versionProvider;
    }

    @Override
    public IHttpLog getHttpLog()
    {
        return httpLog;
    }

    @Override
    public ITraceScenario getTraceScenario()
    {
        return scenario;
    }
}
