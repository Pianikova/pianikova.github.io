/**
 *
 */
package com.e1c.edt.ai.context.DTO;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class ChildEntity
{
    /**
     * Идентификатор сущности.
     */
    public String id;

    /**
     * Название сущности.
     */
    public String name;

    /**
     * Комментарий.
     */
    public String comment;

    /**
     * Синонимы сущности.
     */
    public Map<String, String> synonym;

    /**
     * Название поля контейнера.
     */
    @SerializedName("container")
    public String container;

    /**
     * Идентификатор родительской сущности.
     */
    @SerializedName("parent_id")
    public String parentId;
}