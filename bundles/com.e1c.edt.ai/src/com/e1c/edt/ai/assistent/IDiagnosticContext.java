/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILocalContext;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.ITraceScenario;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.assistent.model.ProjectId;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IDiagnosticContext
{

    ISettings getSettings();

    ISessionService getSessionService();

    IStatistics getStatistics();

    IJson getJson();

    IHttpClientBuilder getHttpClientBuilder();

    IRequestBuilder getRequestBuilder();

    ILocalContext getLocalContext();

    IDiagnosticMapper getDiagnosticMapper();

    ICACertificateReporter getCaCertificateReporter();

    void setSessionId(String sessionId);

    void setProject(ProjectId project);

    void setAIContext(AIContext context);

    AIContext getAIContext();

    String getSessionId();

    ProjectId getProject();

    void releaseContext();

    String getCAReport();

    void setCaReportIfAbsent(String caReport);

    ITokenCheck getTokenCheck();

    IEnvironment getEnvironment();

    IConfigurationParametersProvider getConfigurationParametersProvider();

    IVersionProvider getVersionProvider();

    IHttpLog getHttpLog();

    ITraceScenario getTraceScenario();

}