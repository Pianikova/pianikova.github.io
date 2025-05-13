/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MethodEntity
{
    /**
     * Уникальный идентификатор метода. Например, "file:/SSL/src/CommonForms/ФормаОтчета/Module.bsl?start\u003d147942\u0026finish\u003d148876".
     */
    public String uuid;

    /**
     * Путь к файлу, в котором объявлен метод. Например, "/SSL/src/CommonForms/ФормаОтчета/Module.bsl".
     */
    public String path;

    /**
     * Начальная позиции метода в файле.
     */
    public Integer start;

    /**
     * Конечная позиции метода в файле.
     */
    public Integer finish;

    /**
     * Имя метода.
     */
    public String name;

    /**
     * Вид метода Процедура/Функция/Procedure/Function.
     */
    public String kind;

    /**
     * Код метода.
     */
    public String code;

    /**
     * Список областей, в которых объявлен метод. Например, ["СлужебныеПроцедурыИФункции", "Сервер"].
     */
    public List<String> areas;

    /**
     * Список окружений, в которых объявлен метод. Например, ["SERVER", "MOBILE_SERVER", "MOBILE_AUTONOMOUS_SERVER"].
     */
    public List<String> environments;

    /**
     * Строка сигнатуры метода. Например, "Функция СведенияОбОрганизации(Знач Организация, Знач Поля = "", Знач Дата = Неопределено, Знач КодЯзыка = Неопределено) Экспорт".
     */
    @SerializedName("signature_str")
    public String signatureStr;

    /**
     * Структурированная сигнатура метода.
     */
    @SerializedName("signature_structurized")
    public SignatureStructurized signatureStructurized;

    /**
     * Список комментариев к методу.
     */
    public List<String> comment;

    /**
     * Структурированный комментарий к методу.
     */
    @SerializedName("сomment_structurized")
    public Comment structurizedComment;
}
