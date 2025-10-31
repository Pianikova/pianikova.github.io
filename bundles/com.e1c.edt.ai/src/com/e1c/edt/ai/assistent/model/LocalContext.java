/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Оперативный контекст.
 */
public class LocalContext
{
    /**
     * Код перед курсором пользователя фиксированной длины. Например, "Процедура...".
     */
    public String prefix;

    /**
     * Код после курсора пользователя фиксированной длины. Например, "...КонецПроцедуры".
     */
    public String suffix;

    /**
     * Путь к редактируемому модулю, начиная с каталога конфигурации. Например, "ERP/src/Catalogs/Валюты/ObjectModule.bsl".
     */
    public String path;

    /**
     * Отступ курсора от начала файла.
     */
    public Integer offset;

    /**
     * True, если пользователь вызвал продолжение кода в ручную комбинацией клавиш.
     */
    public boolean forced;

    /**
     * Содержимое буфера обмена, которое попало туда из среды разработки.
     * Время жизни буфера обмена ограничено 15 минутами.
     */
    public ClipboardInfo clipboard;

    /**
     * True если у пользователя открыт контекст-помощник.
     */
    @SerializedName("content_assist")
    public boolean contentAssist;

    /**
     * Вариант язык программированичя "Russian"/"English". Например, "Russian".
     */
    @SerializedName("script_language")
    public String scriptLanguage;

    /**
     * Язык программирования 1с/java. Например, "1с".
     */
    @SerializedName("programing_language")
    public String programingLanguage;

    /**
     * Позиция курсора в редактируемом модуле. Например, "Procedure" или "ImplicitVariable".
     */
    @SerializedName("cursor_object")
    public String cursorObject;

    /**
     * Уникальное для модуля имя метода, в котором находится курсор. Например,  "СведенияОбОрганизации/0".
     */
    @SerializedName("current_method")
    public String currenMethodName;

    /**
     * Cписок сред (приложений), которые могут выполнять код под курсором. Например, ["ПрограммныйИнтерфейс", "ОрганизацииСервер"].
     */
    @SerializedName("cursor_areas")
    public List<String> cursorAreas;

    /**
     * Cписок связанных с кодом (prefix + suffix) объектов.
     * Передается, когда user_parameters.extended_context == true.
     * Например, ["SERVER", "MOBILE_SERVER", "MOBILE_AUTONOMOUS_SERVER", "EXTERNAL_CONN", "CLIENT"],
     */
    @SerializedName("cursor_environments")
    public List<String> cursorEnvironments;

    /**
     * Список объектов, связанных с кодом (prefix + suffix).
     * Передается, когда user_parameters.extended_context == true.
     * Например, ["Справочник.Организации", "Справочник.Организации.СведенияОбОрганизации", "Справочник.Организации.СведенияОбОрганизации.СведенияОбОрганизации"].
     */
    @SerializedName("related_objects")
    public List<Object> relatedObjects;

    /**
     * Список связанных с кодом (prefix + suffix) вызовов методов.
     * Передается, когда user_parameters.extended_context == true.
     */
    @SerializedName("related_functions")
    public List<Object> relatedFunctions;

    /**
     * Список предложений от контекстного ассистента, которые могут быть вставлены в код.
     * Передается, когда user_parameters.extended_context == true.
     */
    @SerializedName("proposals")
    public List<Proposal> proposals;
}

// }
