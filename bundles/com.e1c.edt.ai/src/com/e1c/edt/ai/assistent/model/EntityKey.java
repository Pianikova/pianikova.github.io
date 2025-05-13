/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

/**
 * Неизвестный ключ при обновлении контекста.
 */
public class EntityKey
{
    /**
     * Путь к объекту, относительно корня проекта. Например, "SSL/src/CommonModules/ОрганизацииСервер/Module.bsl".
     */
    public String path;

    /**
     * Имя поля "meta"/"form"/"local_functions.method_name"/"related_objects"/"related_functions"/"configuration_name".
     */
    public String field;
}
