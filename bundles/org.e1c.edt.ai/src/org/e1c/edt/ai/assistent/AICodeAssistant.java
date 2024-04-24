/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.util.Optional;

import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.assistent.model.AITextRequest;
import org.e1c.edt.ai.assistent.model.AITextResponse;
import org.e1c.edt.ai.client.AIClientException;
import org.e1c.edt.ai.client.AISettings;
import org.e1c.edt.ai.client.Messages;

import com.google.gson.Gson;

/**
 * @author Bogdan Sushkov
 *
 */
public class AICodeAssistant
    implements IAICodeAssistant
{
    private ISettingsProvider settingsProvider;

    public AICodeAssistant(ISettingsProvider settingsProvider)
    {
        this.settingsProvider = settingsProvider;
    }

    @Override
    public Optional<AITextResponse> generateText(String text, CancellationToken cancellationToken)
    {
        var optionalSettings = settingsProvider.getSettings();
        if (optionalSettings.isEmpty())
        {
            return Optional.empty();
        }

        var settings = optionalSettings.get();
        HttpURLConnection connection = createPostConnection(settings);
        AITextRequest request = new AITextRequest();
        request.setInputs(text);
        request.setParameters(settings.getLlmParameters());
        Gson gson = new Gson();
        String requestBody = gson.toJson(request);

        cancellationToken.throwIfCanceled();

        try (OutputStream os = connection.getOutputStream())
        {
            byte[] input = requestBody.getBytes("utf-8"); //$NON-NLS-1$

            cancellationToken.throwIfCanceled();

            os.write(input, 0, input.length);
        }
        catch (IOException e)
        {
            throw new AIClientException(Messages.ClientAI_Response_error, e);
        }

        StringBuilder response = getResponse(connection, cancellationToken);

        cancellationToken.throwIfCanceled();

        AITextResponse textResponse = gson.fromJson(response.toString(), AITextResponse.class);
        return Optional.ofNullable(textResponse);
    }


    private StringBuilder getResponse(HttpURLConnection connection, CancellationToken cancellationToken)
    {
        StringBuilder response = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) //$NON-NLS-1$
        {
            response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null)
            {
                cancellationToken.throwIfCanceled();
                response.append(responseLine.trim());
            }
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AIClientException(e.getMessage(), e);
        }
        catch (IOException e)
        {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) //$NON-NLS-1$
            {
                response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null)
                {
                    response.append(responseLine.trim());
                }

                throw new AIClientException(Messages.ClientAI_Server_status_500, e);
            }
            catch (UnsupportedEncodingException e1)
            {
                throw new AIClientException(e1.getMessage(), e1);
            }
            catch (IOException e2)
            {
                throw new AIClientException(Messages.ClientAI_Response_error, e);
            }
        }

        return response;
    }

    private HttpURLConnection createConnection(AISettings settings, String requestMethod)
    {
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection)settings.getApiURL().openConnection();
        }
        catch (IOException e)
        {
            throw new AIClientException(Messages.ClientAI_Cannot_connect, e);
        }

        try
        {
            connection.setRequestMethod(requestMethod);

        }
        catch (ProtocolException e)
        {
            throw new AIClientException(e.getMessage(), e);
        }

        connection.addRequestProperty("client_id", settings.getClientToken()); //$NON-NLS-1$
        connection.setDoOutput(true);
        return connection;
    }

    private HttpURLConnection createPostConnection(AISettings settings)
    {
        HttpURLConnection connection = createConnection(settings, "POST"); //$NON-NLS-1$
        connection.setRequestProperty("Accept", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
        connection.setRequestProperty("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
        return connection;
    }
}
