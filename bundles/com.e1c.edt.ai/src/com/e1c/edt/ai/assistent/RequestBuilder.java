/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Optional;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.ServiceState;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class RequestBuilder
    implements IRequestBuilder
{
    private final ISettings settings;
    private final IVersionProvider versionProvider;
    private final IStateService stateService;

    @Inject
    public RequestBuilder(ISettings settings, IVersionProvider versionProvider, IStateService stateService)
    {
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(versionProvider);
        Preconditions.checkNotNull(stateService);
        this.settings = settings;
        this.versionProvider = versionProvider;
        this.stateService = stateService;
    }

    @Override
    public Optional<HttpRequest.Builder> create(String uriStr)
    {
        URI uri;
        try
        {
            uri = new URI(uriStr);
        }
        catch (URISyntaxException e)
        {
            return Optional.empty();
        }

        var request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(settings.getTimeout())
            .header("Accept", "application/json") //$NON-NLS-1$//$NON-NLS-2$
            .header("Content-Type", "application/json") //$NON-NLS-1$//$NON-NLS-2$
            .header("Unique-Id", settings.getClientUniqueId()); //$NON-NLS-1$

        try
        {
            request = request.header("Authorization", settings.getClientToken()); //$NON-NLS-1$
        }
        catch (IllegalArgumentException e)
        {
            stateService.setState(HttpLog.class.getName(), ServiceState.TOKEN_FAILED);
            // ignore
        }

        var pluginVersion = versionProvider.getPluginVersion();
        var edtVersion = versionProvider.getPlatformVersion();

        if (pluginVersion != null)
        {
            request.header("plugin_version", pluginVersion.toString()); //$NON-NLS-1$
        }

        if (edtVersion != null)
        {
            request.header("EDT_version", edtVersion); //$NON-NLS-1$
        }

        return Optional.of(request);
    }
}
