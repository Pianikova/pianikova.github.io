/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

// {
/**
 * Ключ контекста.
 */
public class EntityKey
{
    /**
     * Путь к объекту, относительно корня проекта. Например, "SSL/src/CommonModules/ОрганизацииСервер/Module.bsl".
     */
    public String path;

    /**
     * Имя поля "meta" или "form" или "local_functions.method_name" или "related_objects" или "related_functions".
     */
    public String field;
}
// }
