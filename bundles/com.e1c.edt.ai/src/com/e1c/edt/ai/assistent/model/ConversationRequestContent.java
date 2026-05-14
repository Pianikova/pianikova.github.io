/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.JsonElement;

/**
 * Содержимое запроса для отправки сообщения в API
 */
public class ConversationRequestContent
{
	/**
	 * Дополнительные данные для контента (новая структура API)
	 */
    public JsonElement content;

	/**
     * Список доступных инструментов (ClientServerTool | ClientTool)
     */
	public List<ToolDefinition> tools;
}
