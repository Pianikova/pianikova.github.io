/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

// {
/**
 * Значение контекста.
 */
public class EntityValue
{
    /**
     * Путь к объекту, относительно корня проекта. Например, "SSL/src/CommonModules/ОрганизацииСервер/Module.bsl".
     */
    public String path;

    /**
     * Имя поля "meta" или "form" или "local_functions.method_name" или "related_objects" или "related_functions".
     */
    public String field;

    /**
     * Хэш объекта. Например, "MD5:977b0ec2292fe3994c174b2df9581163".
     */
    public String hash;
}
// }
