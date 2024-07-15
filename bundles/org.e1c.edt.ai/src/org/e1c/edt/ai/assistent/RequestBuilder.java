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

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class RequestBuilder implements IRequestBuilder
{
    private final ISettingsProvider settingsProvider;

    @Inject
    public RequestBuilder(ISettingsProvider settingsProvider)
    {
        Preconditions.checkNotNull(settingsProvider);
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

        return Optional.of(HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofMinutes(1))
            .header("Accept", "application/json") //$NON-NLS-1$//$NON-NLS-2$
            .header("Content-Type", "application/json") //$NON-NLS-1$//$NON-NLS-2$
            .header("Authorization", settings.getClientToken())); //$NON-NLS-1$
    }
}
