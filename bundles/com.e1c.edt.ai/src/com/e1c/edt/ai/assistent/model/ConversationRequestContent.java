/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;

public class ConversationRequestContent
{
    /**
     * Команда пользователя (наличие поля зависит от tool_name)
     */
    public String instruction;

    /**
     * Код пользователя
     */
    public List<ConversationCode> code;
}
