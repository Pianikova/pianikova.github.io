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
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;

import org.e1c.edt.ai.assistent.model.AITextRequest;
import org.e1c.edt.ai.assistent.model.AITextResponse;
import org.e1c.edt.ai.client.AIClientException;
import org.e1c.edt.ai.client.Messages;

import com.google.gson.Gson;

/**
 * @author Bogdan Sushkov
 *
 */
public class AICodeAssistant
    implements IAICodeAssistant
{
    private String URL;
    private int maxTokens;

    /**
     * Constructor of assistant
     * @param URL service URL
     * @param maxTokens max characters in response, <b>20</b> is default value
     */
    public AICodeAssistant(String URL, int maxTokens)
    {
        this.URL = URL;
        this.maxTokens = maxTokens;
    }

    @Override
    public AITextResponse generateText(String text, CancellationToken cancellationToken)
    {
        URL url = null;
        try
        {
            url = new URL(URL + "/generate"); //$NON-NLS-1$
        }
        catch (MalformedURLException e)
        {
            throw new AIClientException(Messages.ClientAI_Cannot_connect, e);
        }

        HttpURLConnection connection = makePOST(url);
        AITextRequest request = new AITextRequest();
        request.setInputs(text);
        AITextRequest.Parameters params = request.new Parameters();
        params.setMaxNewTokens(this.maxTokens);
        params.setStop(List.of("\n"));
        request.setParameters(params);
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
        return textResponse;
    }


    /*
     * Get response
     * @param connection
     * @return response
     */
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


    /*
     * Make GET request for given URL
     * @param url
     * @return connection
     */
    private HttpURLConnection makeGET(URL url)
    {
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection)url.openConnection();
        }
        catch (IOException e)
        {
            throw new AIClientException(Messages.ClientAI_Cannot_connect, e);
        }
        try
        {
            connection.setRequestMethod("GET"); //$NON-NLS-1$
        }
        catch (ProtocolException e)
        {
            throw new AIClientException(e.getMessage(), e);
        }
        connection.setRequestProperty("Accept", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
        connection.setDoOutput(true);
        return connection;
    }

    /*
     * Make POST request for given URL
     * @param url
     * @return connection
     */
    private HttpURLConnection makePOST(URL url)
    {
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection)url.openConnection();
        }
        catch (IOException e)
        {
            throw new AIClientException(Messages.ClientAI_Cannot_connect, e);
        }
        try
        {
            connection.setRequestMethod("POST"); //$NON-NLS-1$
        }
        catch (ProtocolException e)
        {
            throw new AIClientException(e.getMessage(), e);
        }
        connection.setRequestProperty("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
        connection.setRequestProperty("Accept", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
        connection.setDoOutput(true);
        return connection;
    }

}
