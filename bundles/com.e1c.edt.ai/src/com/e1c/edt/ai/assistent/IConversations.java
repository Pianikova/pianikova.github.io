/**
 *
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.assistent.model.ConversationAskRequest;
import com.e1c.edt.ai.assistent.model.ConversationAskResponse;
import com.e1c.edt.ai.assistent.model.ConversationRequest;
import com.e1c.edt.ai.assistent.model.ConversationResponse;
import com.e1c.edt.ai.assistent.model.ProjectId;

public interface IConversations
{
    CompletableFuture<Optional<ConversationResponse>> createConversationAsync(ProjectId projectId,
        ConversationRequest request,
        ICancellationToken cancellationToken);

    IObservable<ConversationAskResponse> createAskSource(ProjectId projectId, String conversationId,
        ConversationAskRequest request, ICancellationToken cancellationToken);
}
