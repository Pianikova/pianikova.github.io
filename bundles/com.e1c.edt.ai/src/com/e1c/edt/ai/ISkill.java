/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.assistent.Conversations;
import com.e1c.edt.ai.assistent.SendUserMessageRequest;


/**
 * Prepares a final message for the {@link Conversations} pipeline.
 * <p>
 * A skill is invoked by {@link ConversationFacade} when a {@code SendUserMessageRequest}
 * contains a {@code skillId}. The skill is responsible for building the final prompt
 * from the request data, optional skill parameters, and any preprocessing results
 * obtained via tools.
 * <p>
 * The facade does not know how a particular skill works. It only delegates request
 * preparation to the selected skill and then sends the prepared message through the
 * regular Conversations flow.
 * <p>
 * Typical skill responsibilities include:
 * <ul>
 *   <li>loading a prompt template from resources;</li>
 *   <li>reading {@code instruction} and {@code skillParameters};</li>
 *   <li>calling tools for preprocessing if needed;</li>
 *   <li>inserting variables into the template;</li>
 *   <li>returning the final prepared instruction.</li>
 * </ul>
 *
 * @author - Bogdan Sushkov
 */
public interface ISkill
{
    CompletableFuture<String> prepareAsync(SendUserMessageRequest request, ICancellationToken token);
}
