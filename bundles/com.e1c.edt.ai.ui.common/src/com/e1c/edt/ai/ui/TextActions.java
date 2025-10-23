/**
 *
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.Observables;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.ITools;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.ToolInvokeRequest;
import com.e1c.edt.ai.assistent.model.ToolInvokeRequestContent;
import com.e1c.edt.ai.assistent.model.ToolInvokeResponse;
import com.e1c.edt.ai.assistent.model.VisualContext;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class TextActions implements ITextActions
{
    private final ILog log;
    private final IDispatcher dispatcher;
    private final ISettings settings;
    private final ITools tools;
    private final IResourceProvider resourceProvider;
    private final IJson json;
    private Job currentJob;

    @Inject
    public TextActions(ILog log, IDispatcher dispatcher, ISettings settings, ITools tools,
        IResourceProvider resourceProvider, IJson json)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(tools);
        Preconditions.checkNotNull(resourceProvider);
        Preconditions.checkNotNull(json);
        this.log = log;
        this.dispatcher = dispatcher;
        this.settings = settings;
        this.tools = tools;
        this.resourceProvider = resourceProvider;
        this.json = json;
    }

    @Override
    public IObservable<TextImprovements> ceateTextImprovementsSource(VisualContext context, TextAction action,
        ICancellationToken cancellationToken)
    {
        return Observables.create(observer -> {
            var job = dispatcher.createJob(Messages.BackgroundJobName, jobCtx -> {
                try
                {
                    ceateTextImprovements(context, action, observer, cancellationToken);
                }
                catch (Exception error)
                {
                    log.logError(error);
                    observer.onError(error);
                }
            }, cancellationToken);
            runJob(job);
            return Closeables.Empty;
        });
    }

    @SuppressWarnings("nls")
    private void ceateTextImprovements(VisualContext context, TextAction action, IObserver<TextImprovements> observer,
        ICancellationToken cancellationToken)
    {
        var contextJson = json.serialize(context);
        var toolInvokeRequest = new ToolInvokeRequest();
        toolInvokeRequest.toolName = "raw";
        toolInvokeRequest.uiLanguage = settings.getLanguage();
        var content = new ToolInvokeRequestContent();
        toolInvokeRequest.content = content;
        content.instruction = resourceProvider.getTextResource(action.resourceName)
            .orElse("")
            .replace("${language}", settings.getLanguage())
            .replace("${context}", contextJson);

        log.trace(TracingSources.API_CALLS, "Prompt", () -> content.instruction);
        var message = new StringBuilder();
        var uudi = new StringBuilder();
        var invokeSource = tools.createInvokeSource(ProjectId.Default, toolInvokeRequest, cancellationToken);
        invokeSource.subscribe(new IObserver<ToolInvokeResponse>()
        {
            @Override
            public void onNext(ToolInvokeResponse value)
            {
                var content = value.content;
                if (value.uuid != null)
                {
                    uudi.setLength(0);
                    uudi.append(value.uuid);
                }

                if (content != null)
                {
                    var text = content.text;
                    if (text != null)
                    {
                        if (value.finished)
                        {
                            text = text.trim();
                        }
                        else
                        {
                            synchronized (message)
                            {
                                message.append(text);
                                text = message.toString().trim();
                            }
                        }

                        if (!text.isBlank())
                        {
                            observer.onNext(new TextImprovements(uudi.toString(), text));
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable error)
            {
                uudi.setLength(0);
                observer.onError(error);
            }

            @Override
            public void onCompleted()
            {
                uudi.setLength(0);
                observer.onCompleted();
            }
        });
    }

    private synchronized void runJob(Job job)
    {
        if (currentJob != null)
        {
            currentJob.cancel();
            currentJob = null;
        }

        currentJob = job;
        currentJob.setPriority(Job.DECORATE);
        job.schedule();
    }
}
