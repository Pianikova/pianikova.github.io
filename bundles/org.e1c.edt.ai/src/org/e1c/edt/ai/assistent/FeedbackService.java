/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.assistent.model.AcceptedCodeFeedback;
import org.e1c.edt.ai.assistent.model.CursorInfo;
import org.e1c.edt.ai.assistent.model.FinalCodeFeedback;
import org.e1c.edt.ai.assistent.model.IssueFeedback;
import org.e1c.edt.ai.assistent.model.IssueType;
import org.e1c.edt.ai.client.AIClientException;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class FeedbackService
    implements IFeedbackService
{
    private final IHttpLog log;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clienBuilder;
    private final IJson json;

    @Inject
    public FeedbackService(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder, IJson json)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clienBuilder = clientBuilder;
        this.json = json;
    }

    @Override
    public CompletableFuture<Void> acceptedCodeAsync(String uuid, String code, Optional<CursorInfo> cursorStartInfo,
        Optional<CursorInfo> cursorEndInfo)
    {
        var builder = requestBuilder.create("./feedbacks/accepted_code"); //$NON-NLS-1$
        if (builder.isEmpty())
        {
            return CompletableFuture.completedFuture(null);
        }

        var feedback = new AcceptedCodeFeedback();
        feedback.requestUuid = uuid;
        feedback.acceptedCode = code;
        feedback.cursorStartInfo = cursorStartInfo.orElse(null);
        feedback.cursorEndInfo = cursorEndInfo.orElse(null);
        var body = json.serialize(feedback);
        var request = builder.get().POST(BodyPublishers.ofString(body)).build();
        return sendFeebackAsync(request, body);
    }

    @Override
    public CompletableFuture<Void> finalizeCodeAsync(String uuid, String code)
    {
        var builder = requestBuilder.create("./feedbacks/final_code"); //$NON-NLS-1$
        if (builder.isEmpty())
        {
            return CompletableFuture.completedFuture(null);
        }

        var feedback = new FinalCodeFeedback();
        feedback.requestUuid = uuid;
        feedback.finalCode = code;
        var body = json.serialize(feedback);
        var request = builder.get().POST(BodyPublishers.ofString(body)).build();
        return sendFeebackAsync(request, body);
    }

    @Override
    public CompletableFuture<Void> issueAsync(String uuid, IssueType type, String description)
    {
        var builder = requestBuilder.create("./feedbacks/issue"); //$NON-NLS-1$
        if (builder.isEmpty())
        {
            return CompletableFuture.completedFuture(null);
        }

        var feedback = new IssueFeedback();
        feedback.requestUuid = uuid;
        feedback.issueType = type;
        feedback.issueDescription = description;
        var body = json.serialize(feedback);
        var request = builder.get().POST(BodyPublishers.ofString(body)).build();
        return sendFeebackAsync(request, body);
    }

    private CompletableFuture<Void> sendFeebackAsync(HttpRequest request, String body)
    {
        log.request(request, null, body);
        return clienBuilder.create()
            .build()
            .sendAsync(request, BodyHandlers.ofString())
            .thenApplyAsync(response -> log.response(response, null))
            .thenApplyAsync(response -> {
                var statusCode = response.statusCode();
                if (statusCode >= 300)
                {
                    throw new AIClientException("AI HTTP feedback response status code is " + statusCode, null); //$NON-NLS-1$
                }

                return response;
            })
            .thenApply(response -> null);
    }
}
