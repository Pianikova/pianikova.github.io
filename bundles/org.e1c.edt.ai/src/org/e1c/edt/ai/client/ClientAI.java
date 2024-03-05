/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

import org.e1c.edt.ai.client.model.ChatCreateRequest;
import org.e1c.edt.ai.client.model.ChatInfo;
import org.e1c.edt.ai.client.model.Conversation;
import org.e1c.edt.ai.client.model.ConversationID;
import org.e1c.edt.ai.client.model.Message;
import org.e1c.edt.ai.client.model.MessageRequest;
import org.e1c.edt.ai.client.model.MessageResponse;
import org.e1c.edt.ai.client.model.internal.MessageContent;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This class is an API to 1C.AI service. It is used to create a connection
 * with the AI. It also provides access to manipulation: receiving messages,
 * conversations, sending feedback and getting their list for a dedicated chat.<p>
 * To begin a conversation, you must first check the preferences page on 1C.AI and
 *  set the chat parameters. These parameters will then be used to create the chat. <p>
 *
 * An example of usage with a preference page:
 * <blockquote><pre>
 *      ChatSettings settings = ClientAIPreferenceService.getSettings();
 *      ClientAI chat = new ClientAI(settings);
 *      ConversationID chatID = chat.createChat();
 *      ArrayList&lt;MessageResponse> ans = chat.sendMessage("When will the next salary be?");
 * </pre></blockquote>
 *
 * @see ChatSettings
 * @see MessageResponse
 * @author Bogdan Sushkov
 */
public class ClientAI
    implements IClientAI
{
    private ChatInfo chat;
    private ChatSettings settings;

    /**
     * Constructs the object ClientAI via chat preference page.
     * @param settings AI preference page
     */
    public ClientAI(ChatSettings settings)
    {
        chat = new ChatInfo(settings.getClientToken(), settings.getServiceURL());
        this.setSettings(settings);
    }

    @Override
    public ConversationID createChat() throws AIClientException
    {
        URL url = null;
        try
        {
            url = new URL(chat.getURL() + "/conversations/"); //$NON-NLS-1$
        }
        catch (MalformedURLException e)
        {
            throw new AIClientException(Messages.ClientAI_Cannot_connect, e);
        }
        HttpURLConnection connection = makePOST(url);
        ChatCreateRequest request = new ChatCreateRequest(settings.getDataBaseName(), settings.getModelName(),
            settings.getAccessRoles(), settings.getTags(), settings.getDocumentPath());
        Gson gson = new Gson();
        String requestBody = gson.toJson(request);
        try (OutputStream os = connection.getOutputStream())
        {
            byte[] input = requestBody.getBytes("utf-8"); //$NON-NLS-1$
            os.write(input, 0, input.length);
        }
        catch (IOException e)
        {
            throw new AIClientException(Messages.ClientAI_Response_error, e);
        }
        StringBuilder response = getResponse(connection);
        chat.setConversationUUID(parseJSON(response.toString(), "uuid")); //$NON-NLS-1$
        ConversationID responseUUID = new ConversationID(chat.getConversationUUID());
        return responseUUID;
    }

    @Override
    public Conversation getConversation(String UUID) throws AIClientException
    {
        URL url = null;
        try
        {
            url = new URL(chat.getURL() + "/conversations/" + UUID); //$NON-NLS-1$
        }
        catch (MalformedURLException e)
        {
            throw new AIClientException(Messages.ClientAI_Cannot_connect, e);
        }
        HttpURLConnection connection = makeGET(url);
        StringBuilder response = getResponse(connection);
        Gson gson = new Gson();
        Conversation responseConversation = gson.fromJson(response.toString(), Conversation.class);
        return responseConversation;
    }

    @Override
    public ArrayList<Message> sendMessage(String message)
    {
        URL url = null;
        try
        {
            url = new URL(chat.getURL() + "/conversations/" + chat.getConversationUUID() + "/messages"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        catch (MalformedURLException e)
        {
            throw new AIClientException(Messages.ClientAI_Cannot_connect, e);
        }
        HttpURLConnection connection = makePOST(url);

        MessageRequest request =
            new MessageRequest(message, chat.getParentUUID());
        Gson gson = new Gson();
        String requestBody = gson.toJson(request);
        try (OutputStream os = connection.getOutputStream())
        {
            byte[] input = requestBody.getBytes("utf-8"); //$NON-NLS-1$
            os.write(input, 0, input.length);
        }
        catch (IOException e)
        {
            throw new AIClientException(Messages.ClientAI_Response_error,
                e);
        }
        StringBuilder response = getResponse(connection);
        ArrayList<Message> responseMessages = new ArrayList<>();
        try
        {
            JsonArray jsonArray = JsonParser.parseString(response.toString()).getAsJsonArray();
            for (int i = 0; i < jsonArray.size(); i++)
            {
                responseMessages.add(gson.fromJson(jsonArray.get(i), Message.class));
                ArrayList<String> docString = responseMessages.get(i).getContent().getData().getDocumentsString();
                if (docString != null && !docString.isEmpty())
                {
                    ArrayList<MessageContent.Data.Documents> documents = new ArrayList<>();
                    for (String elem : docString)
                    {
                        documents.add(gson.fromJson(elem, MessageContent.Data.Documents.class));
                    }
                    responseMessages.get(i).getContent().getData().setDocuments(documents);
                }
            }
            chat.setParentUUID(responseMessages.get(3).getUUID());
        }
        catch (RuntimeException e)
        {
            throw new AIClientException(e.getMessage(), e);
        }
        return responseMessages;
    }

    /**
     * @return the chat
     */
    public ChatInfo getChat()
    {
        return chat;
    }

    /**
     * @param chat the chat to set
     */
    public void setChat(ChatInfo chat)
    {
        this.chat = chat;
    }

    /**
     * @return the settings
     */
    public ChatSettings getSettings()
    {
        return settings;
    }

    /**
     * @param settings the settings to set
     */
    public void setSettings(ChatSettings settings)
    {
        this.settings = settings;
    }

    /*
     * Get response
     * @param connection
     * @return response
     */
    private StringBuilder getResponse(HttpURLConnection connection)
    {
        StringBuilder response = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) //$NON-NLS-1$
        {
            response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null)
            {
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
                throw new AIClientException(
                    Messages.ClientAI_Response_error, e);
            }
        }
        return response;
    }

    /*
     * Parse JSON
     * @param json
     * @param toFind
     * @return parsed json
     */
    private String parseJSON(String json, String toFind)
    {
        JsonObject jsonObj = JsonParser.parseString(json).getAsJsonObject();
        return jsonObj.get(toFind).getAsString();
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
        connection.setRequestProperty("X-Auth-Token", chat.getClientToken()); //$NON-NLS-1$
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
        connection.setRequestProperty("X-Auth-Token", chat.getClientToken()); //$NON-NLS-1$
        connection.setRequestProperty("Accept", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
        connection.setDoOutput(true);
        return connection;
    }

    /*
     * Make PUT request for given URL
     * @param url
     * @return connection
     */
    private HttpURLConnection makePUT(URL url)
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
            connection.setRequestMethod("PUT"); //$NON-NLS-1$
        }
        catch (ProtocolException e)
        {
            throw new AIClientException(e.getMessage(), e);
        }
        connection.setRequestProperty("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
        connection.setDoOutput(true);
        return connection;
    }
}
