/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.quickfix;

import java.util.Map;
import java.util.Optional;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.SkillExecutionRequest;
import com.e1c.edt.ai.skills.ISkillExecutor;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IDispatcher;
import com.google.inject.Inject;

/** Runs a Workmate fix for selected EDT Configuration Problems markers. */
public class ConfigurationProblemAIHandler
    extends AbstractHandler
{
    public static final String COMMAND_ID = "com.e1c.edt.ai.ui.commands.fixConfigurationProblem.ai"; //$NON-NLS-1$
    private static final String SKILL_NAME = "quick-fix-configuration-problem"; //$NON-NLS-1$

    @Inject
    ISettings settings;
    @Inject
    IEditingSupport editingSupport;
    @Inject
    ISkillExecutor skillExecutor;
    @Inject
    IDispatcher dispatcher;
    @Inject
    IChat chat;
    @Inject
    ILog log;

    public ConfigurationProblemAIHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean isEnabled()
    {
        return settings.isEnabled() && currentSelection().isPresent();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        try
        {
            Optional<ConfigurationProblemSelection> selected =
                ConfigurationProblemSelection.from(HandlerUtil.getCurrentSelection(event));
            if (selected.isEmpty() || editingSupport.isReadOnly(selected.get().getProject()))
            {
                return null;
            }

            ConfigurationProblemSelection problems = selected.get();
            AIContext context = new AIContext(problems.getProject(), "", null); //$NON-NLS-1$
            SkillExecutionRequest request = new SkillExecutionRequest(SKILL_NAME,
                Map.of("project_name", problems.getProject().getName(), //$NON-NLS-1$
                    "configuration_problems", ConfigurationProblemFormatter.format(problems.getMarkers()))); //$NON-NLS-1$
            skillExecutor.executeAsync(request, CancellationTokens.NONE)
                .thenAccept(result -> dispatcher.dispatchAsync(() -> chat.askQuestion(context, result.getPrompt())))
                .exceptionally(error -> {
                    log.logError(error);
                    return null;
                });
        }
        catch (Exception e)
        {
            log.logError(e);
        }
        return null;
    }

    private static Optional<ConfigurationProblemSelection> currentSelection()
    {
        var workbench = PlatformUI.getWorkbench();
        var window = workbench != null ? workbench.getActiveWorkbenchWindow() : null;
        var page = window != null ? window.getActivePage() : null;
        ISelection selection = page != null ? page.getSelection() : null;
        return ConfigurationProblemSelection.from(selection);
    }
}
