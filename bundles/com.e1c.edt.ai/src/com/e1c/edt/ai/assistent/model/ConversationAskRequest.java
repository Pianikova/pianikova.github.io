/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class ConversationAskRequest
{
	/**
	 * Идентификатор сообщения на который отвечаем
	 */
	@SerializedName("parent_uuid")
	public String parentUuid;

	/**
     * Роль отправителя (user, tool)
     */
	public String role;

	/**
     * Содержимое запроса, полиморфное поле:
     * <ul>
     * <li>Для role=user: объект UserMessageContent</li>
     * <li>Для role=tool: массив объектов List[ToolMessageContent]</li>
     * </ul>
     */
    public JsonElement content;
}
