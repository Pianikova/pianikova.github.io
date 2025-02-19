/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Optional;

import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.IVersionProvider;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class RequestBuilder
    implements IRequestBuilder
{
    private final ISettingsProvider settingsProvider;
    private final IVersionProvider versionProvider;
    private final IUISettings uiSettings;

    @Inject
    public RequestBuilder(ISettingsProvider settingsProvider, IVersionProvider versionProvider, IUISettings uiSettings)
    {
        Preconditions.checkNotNull(settingsProvider);
        Preconditions.checkNotNull(versionProvider);
        Preconditions.checkNotNull(uiSettings);
        this.versionProvider = versionProvider;
        this.settingsProvider = settingsProvider;
        this.uiSettings = uiSettings;
    }

    @Override
    public Optional<HttpRequest.Builder> create(String relativePath)
    {
        var settings = settingsProvider.getSettings();
        URI uri;
        try
        {
            uri = settings.getLlmParameters().url.toURI().resolve(relativePath);
        }
        catch (URISyntaxException e)
        {
            return Optional.empty();
        }

        var request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(uiSettings.getTimeout())
            .header("Accept", "application/json") //$NON-NLS-1$//$NON-NLS-2$
            .header("Content-Type", "application/json") //$NON-NLS-1$//$NON-NLS-2$
            .header("Authorization", settings.getClientToken()) //$NON-NLS-1$
            .header("Unique-Id", settings.getClientUniqueId()); //$NON-NLS-1$

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
