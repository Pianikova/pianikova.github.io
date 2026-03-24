/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpRequest.BodyPublishers;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.assistent.model.ConversationRequest;
import com.google.common.base.Stopwatch;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * @author Bogdan Sushkov
 *
 */
public class ChatDiagnosticTest
    implements IDiagnosticTest
{

    @Override
    public String id()
    {
        return "chat-diagnostic-test"; //$NON-NLS-1$
    }

    @Override
    public String title()
    {
        return Messages.ChatDiagnosticTest_Title;
    }

    @SuppressWarnings("nls")
    @Override
    public DiagnosticResult execute(IDiagnosticContext context, IProgressMonitor monitor)
    {
        String chatWebUrl = context.getSettings().getChatUrl().toString();
        var facts = new HashMap<String, String>();
        facts.put("url", chatWebUrl.toString());
        facts.put("java.home", System.getProperty("java.home"));

        DiagnosticResult webRes = probeWebViewAsync(chatWebUrl, "document.readyState === 'complete'", context, facts);

        // chat api
        String http = context.getSettings().getUrl().toString() + "chat_api/v1/conversations";
        Map<String, String> factsHttp = new HashMap<>();
        factsHttp.put("url", http);

        DiagnosticResult httpRes = probeHttpAsync(context.getSessionId(), context, factsHttp);

        if (webRes.getSeverity() != DiagnosticSeverity.OK)
        {
            return webRes;
        }
        if (httpRes.getSeverity() != DiagnosticSeverity.OK)
        {
            return httpRes;
        }

        return DiagnosticResult.ok(Messages.ChatDiagnosticTest_ExecutedSuccessfully);
    }

    @SuppressWarnings("nls")
    private DiagnosticResult probeHttpAsync(String sessionId, IDiagnosticContext context, Map<String, String> facts)
    {
        ConversationRequest req = new ConversationRequest();
        req.isChat = true;
        req.programmingLanguage = "1c";
        req.scriptLanguage = "ru";
        req.uiLanguage = "ru";
        req.skillName = "docstring";

        var request = context.getJson().serialize(req);
        var client = context.getHttpClientBuilder().create().build();
        var bodyPublisher = BodyPublishers.ofString(request);

        var builderOpt = context.getRequestBuilder().create(facts.getOrDefault("url", ""));
        if (builderOpt.isEmpty())
        {
            throw new AIClientException(
                "Issue occured because cannot instantiate code completion client with url:"
                    + facts.getOrDefault("url", ""),
                null);
        }
        var builder = builderOpt.get();

        var requestInner = builder.header("Session-Id", sessionId)
            .POST(bodyPublisher)
            .build();
        var stopwatch = Stopwatch.createStarted();

        return client.sendAsync(requestInner, java.net.http.HttpResponse.BodyHandlers.ofString())
            .orTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .thenApply(response -> context.getHttpLog().response(response, id(), stopwatch, true, false))
            .handle((response, err) -> {
                final var status = response == null ? 500 : response.statusCode();
                if (err == null && status >= 200 && status < 300)
                {
                    return DiagnosticResult.ok(Messages.ChatDiagnosticTest_ChatApiAvailable);
                }
                return context.getDiagnosticMapper().map("chatApi-diagnostic-test", response.statusCode(), err, facts);
            })
            .join();
    }

    private DiagnosticResult probeWebViewAsync(String url, String readyJsExpr, IDiagnosticContext context,
        Map<String, String> facts)
    {
        CompletableFuture<Void> cf = new CompletableFuture<>();

        ensureFxStarted();

        Platform.runLater(() -> {
            final WebEngine engine = createHiddenEngine();
            final Worker<Void> worker = engine.getLoadWorker();

            @SuppressWarnings("unchecked")
            final ChangeListener<Worker.State>[] holder = new ChangeListener[1];

            Runnable cleanup = () -> {
                try
                {
                    if (holder[0] != null)
                        worker.stateProperty().removeListener(holder[0]);
                }
                catch (Throwable t)
                {
                    //
                }
                try
                {
                    engine.load(null);
                }
                catch (Throwable t)
                {
                    //
                }
            };

            holder[0] = (obs, old, st) -> {
                try
                {
                    if (st == Worker.State.SUCCEEDED)
                    {
                        Object r = engine.executeScript(readyJsExpr);
                        boolean ok = (r instanceof Boolean) ? (Boolean)r : (r != null);
                        if (ok)
                        {
                            cf.complete(null);
                        }
                        else
                        {
                            cf.completeExceptionally(new RuntimeException(
                                "WebView loaded, but readiness JS returned false: " + readyJsExpr)); //$NON-NLS-1$
                        }
                        return;
                    }

                    if (st == Worker.State.FAILED || st == Worker.State.CANCELLED)
                    {
                        Throwable ex = worker.getException();
                        cf.completeExceptionally(ex != null ? ex : new RuntimeException("WebEngine load " + st)); //$NON-NLS-1$
                    }
                }
                catch (Throwable t)
                {
                    cf.completeExceptionally(t);
                }
                finally
                {
                    if (st == Worker.State.SUCCEEDED || st == Worker.State.FAILED || st == Worker.State.CANCELLED
                        || cf.isDone())
                    {
                        cleanup.run();
                    }
                }
            };

            worker.stateProperty().addListener(holder[0]);

            try
            {
                engine.load(url.toString());
            }
            catch (Throwable t)
            {
                cleanup.run();
                cf.completeExceptionally(t);
            }
        });
        return cf.orTimeout(20, java.util.concurrent.TimeUnit.SECONDS).handle((resp, err) -> {
            if (err == null)
            {
                return DiagnosticResult.ok(Messages.ChatDiagnosticTest_ChatWebUiLoaded);
            }
            return context.getDiagnosticMapper().map("chatWebView-diagnostic-test", 0, err, facts); //$NON-NLS-1$
        }).join();
    }

    // @formatter:off
    private void ensureFxStarted()
    {
        try
        {
            Platform.startup(() -> { /**/ });
        }
        catch (IllegalStateException hadStarted)
        {
            // Toolkit already initialized
        }
    }
    // @formatter:on

    private WebEngine createHiddenEngine()
    {
        WebView view = new WebView();
        new Scene(view);
        WebEngine engine = view.getEngine();
        engine.setJavaScriptEnabled(true);
        return engine;
    }
}
