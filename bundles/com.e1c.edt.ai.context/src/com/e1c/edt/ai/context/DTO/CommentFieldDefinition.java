/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

/**
 * Комментарии к полю типа.
 */
public class CommentFieldDefinition
{
    /**
     * Наименование поля.
     */
    public String name;

    /**
     * Описание поля.
     */
    public List<CommentDescriptionPart> description;

    /**
     * Типы, которые могут быть присвоены полю.
     */
    public List<CommentType> types;
}
