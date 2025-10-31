/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

/**
 * Комментарии к параметру метода.
 */
public class CommentParameter
{
    /**
     * Описание параметра.
     */
    public List<CommentDescriptionPart> description;

    /**
     * Имя параметра.
     */
    public String name;

    /**
     * Типы параметра.
     */
    public List<CommentType> types;
}
