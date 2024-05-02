/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * This class keeps parameters of active chat. It usually uses
 * to form request with new chat parameters in process of chat creation.
 * @author Bogdan Sushkov
 *
 */
public class ChatCreateRequest
{
    private ArrayList<Database> databases;
    @SerializedName("model_name")
    private String modelName;

    /**
     * Constructor of json for request
     * @param dataBaseName
     * @param modelName
     * @param accessRoles may be <code>null</code>
     * @param tags may be <code>null</code>
     * @param documentPath may be <code>null</code>
     */
    public ChatCreateRequest(String dataBaseName, String modelName, List<String> accessRoles, List<String> tags,
        String documentPath) {
        this.databases = new ArrayList<>();
        databases.add(new Database(dataBaseName, accessRoles, tags, documentPath));
        this.modelName = modelName;
    }

    /**
     * @return the databases
     */
    public List<Database> getDatabases()
    {
        return databases;
    }

    /**
     * @param databases the databases to set
     */
    public void setDatabases(ArrayList<Database> databases)
    {
        this.databases = databases;
    }

    /**
     * @return the modelName
     */
    public String getModelName()
    {
        return modelName;
    }

    /**
     * @param modelName the modelName to set
     */
    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }
}
