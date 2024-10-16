/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Optional;

import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.IVersionProvider;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class RequestBuilder implements IRequestBuilder
{
    private final ISettingsProvider settingsProvider;
    private final IVersionProvider versionProvider;

    @Inject
    public RequestBuilder(ISettingsProvider settingsProvider, IVersionProvider versionProvider)
    {
        Preconditions.checkNotNull(settingsProvider);
        Preconditions.checkNotNull(versionProvider);
        this.versionProvider = versionProvider;
        this.settingsProvider = settingsProvider;
    }

    @Override
    public Optional<HttpRequest.Builder> create(String relativePath)
    {
        var otionalSettings = settingsProvider.getSettings();
        if (otionalSettings.isEmpty())
        {
            return Optional.empty();
        }

        var settings = otionalSettings.get();
        URI uri;
        try
        {
            uri = settings.getApiURL().toURI().resolve(relativePath);
        }
        catch (URISyntaxException e)
        {
            return Optional.empty();
        }

        var request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofMinutes(1))
            .header("Accept", "application/json") //$NON-NLS-1$//$NON-NLS-2$
            .header("Content-Type", "application/json") //$NON-NLS-1$//$NON-NLS-2$
            .header("Authorization", settings.getClientToken()); //$NON-NLS-1$

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
