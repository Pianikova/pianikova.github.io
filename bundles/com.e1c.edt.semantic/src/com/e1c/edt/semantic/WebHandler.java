/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.semantic;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.e1c.edt.ai.context.IEntityInfo;
import com.e1c.edt.ai.context.IRelatedEntities;
import com.e1c.edt.ai.context.DTO.EntityInfoRequest;
import com.e1c.edt.ai.context.DTO.RelatedEntitiesRequest;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class WebHandler
    extends AbstractHandler
{
    private final ILog log;
    private final IJson json;
    private final IRelatedEntities relatedEntities;
    private final IEntityInfo entityInfo;
    private final IIDE ide;

    @Inject
    public WebHandler(ILog log, IJson json, IRelatedEntities relatedEntities, IEntityInfo entityInfo, IIDE ide)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(relatedEntities);
        Preconditions.checkNotNull(entityInfo);
        Preconditions.checkNotNull(ide);
        this.log = log;
        this.json = json;
        this.relatedEntities = relatedEntities;
        this.entityInfo = entityInfo;
        this.ide = ide;
    }

    @SuppressWarnings("nls")
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(baseRequest);
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(response);
        try
        {
            var isHandled = false;
            switch (target.toLowerCase())
            {
            case "/related_entities":
                isHandled =
                    handle(request, RelatedEntitiesRequest.class, relatedEntities::getRelatedEntities, response);
                break;
            case "/entity":
                isHandled = handle(request, EntityInfoRequest.class, entityInfo::getInfo, response);
                break;
            case "/ide_close":
                isHandled = handle(request, CloseRequest.class, ide::close, response);
                break;
            }

            if (isHandled)
            {
                response.flushBuffer();
                response.setStatus(200);
                baseRequest.setHandled(true);
            }

            final var handled = isHandled;
            log.trace("request", () -> {
                var info = new StringBuilder();
                info.append("target: ");
                info.append(target);

                info.append(System.lineSeparator());
                info.append("baseRequest: ");
                info.append(baseRequest);

                info.append(System.lineSeparator());
                info.append("request: ");
                info.append(request);

                info.append(System.lineSeparator());
                info.append("isHandled: ");
                info.append(handled);

                info.append(System.lineSeparator());
                info.append("response: ");
                info.append(response);

                return info.toString();
            }, Verbosity.DEFAULT);
        }
        catch (Exception e)
        {
            log.logError(e);
            response.getWriter().append(e.getMessage());
            response.setStatus(500);
            baseRequest.setHandled(true);
        }
    }

    private <TRequest, TResponse> boolean handle(HttpServletRequest request, Class<TRequest> classOfTRequest,
        BiFunction<TRequest, ICancellationToken, Optional<TResponse>> handler, HttpServletResponse response)
    {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(classOfTRequest);
        Preconditions.checkNotNull(handler);
        Preconditions.checkNotNull(response);
        return readContent(request).flatMap(content -> json.deserialize(content, classOfTRequest))
            .flatMap(r -> handler.apply(r, CancellationTokens.NONE))
            .map(r -> json.serialize(r))
            .map(content -> wrireContent(response, content))
            .orElse(false);
    }

    private Optional<String> readContent(HttpServletRequest request)
    {
        Preconditions.checkNotNull(request);
        var content = new StringBuilder();
        String line;
        try
        {
            var reader = request.getReader();
            while ((line = reader.readLine()) != null)
            {
                content.append(line);
            }
        }
        catch (IOException e)
        {
            log.logError(e);
            return Optional.empty();
        }

        return Optional.of(content.toString());
    }

    @SuppressWarnings("nls")
    private Boolean wrireContent(HttpServletResponse response, String content)
    {
        Preconditions.checkNotNull(response);
        Preconditions.checkNotNull(content);
        try
        {
            response.addHeader("Content-Type", "application/json");
            response.getWriter().append(content);
        }
        catch (IOException e)
        {
            log.logError(e);
            return false;
        }

        return true;
    }
}
