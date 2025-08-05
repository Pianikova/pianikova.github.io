/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class MetaEntity
{
    /**
     * Имя пространства имен.
     */
    public String namespace;

    /**
     * Полное имя.
     */
    public String fullQualifiedName;

    /**
     * Путь к сущности.
     */
    public String path;

    /**
     * Название сущности.
     */
    public String name;

    /**
     * Тип сущности.
     */
    public String type;

    /**
     * Типы, на которые ссылается сущность, если есть.
     */
    public List<DataType> types;

    /**
     * Комментарий.
     */
    public String comment;

    /**
     * Синонимы сущности.
     */
    public Map<String, String> synonym;

    /**
     * Атрибуты сущности, если есть.
     */
    public List<AttributeEntity> attributes;

    /**
     * Стандартные атрибуты сущности, если есть.
     */
    @SerializedName("standard_attributes")
    public List<AttributeEntity> standardAttributes;

    /**
     * Поля сущности, если есть.
     */
    public List<FieldEntity> fields;

    /**
     * Табличные секции сущности, если есть.
     */
    @SerializedName("tabular_sections")
    public List<TabularSectionEntity> tabularSections;

    /**
     * Регистры ресурсов сущности, если есть.
     */
    @SerializedName("register_resources")
    public List<RegisterResourceEntity> registerResources;

    /**
     * Регистры измерений сущности, если есть.
     */
    @SerializedName("register_dimensions")
    public List<RegisterDimensionEntity> registerDimensions;

    /**
     * Регистры записей сущности, если есть.
     */
    @SerializedName("register_records")
    public List<RegisterRecordEntity> registerRecords;

    /**
     * Енумы сущности, если есть.
     */
    @SerializedName("enum_values")
    public List<EnumValueEntity> enumValues;

    /**
     * Формы сущности, если есть.
     */
    @SerializedName("object_form")
    public List<ObjectFormEntity> objectForms;

    // Too much info:
    // Движение - проведение
    // public String posting;

    // Движение - оперативное проведение
    // @SerializedName("real_time_posting")
    // public String realTimePosting;

    // Движение - удаление движений
    // @SerializedName("register_records_deletion")
    // public String registerRecordsDeletion;

    public List<PredefinedEntity> predefined;

    @SerializedName("based_on")
    public List<MetaEntity> basedOn;

    @SerializedName("subsystem_objects")
    public List<String> subsystemObjects;

    public List<TemplateEntity> templates;

    public List<ColumnEntity> columns;

    public String event;

    public String handler;

    public String description;

    public String key;

    @SerializedName("method_name")
    public String methodName;
}
