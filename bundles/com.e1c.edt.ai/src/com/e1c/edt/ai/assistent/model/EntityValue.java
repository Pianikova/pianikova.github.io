/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

/**
 * Неизвестное значение при обновлении контекста.
 */
public class EntityValue
{
    /**
     * Путь к объекту, относительно корня проекта. Например, "SSL/src/CommonModules/ОрганизацииСервер/Module.bsl".
     */
    public String path;

    /**
     * Имя поля "meta"/"form"/"local_functions.method_name"/"related_objects"/"related_functions"/"configuration_name".
     */
    public String field;

    /**
     * Хэш объекта. Например, "MD5:977b0ec2292fe3994c174b2df9581163".
     */
    public String hash;
}
