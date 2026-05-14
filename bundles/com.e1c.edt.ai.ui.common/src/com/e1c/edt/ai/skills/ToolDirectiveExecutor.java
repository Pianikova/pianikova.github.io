/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.assistent.SkillErrorCode;
import com.e1c.edt.ai.assistent.SkillExecutionException;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class ToolDirectiveExecutor
    implements IToolDirectiveExecutor
{
    private final SkillTemplateProcessor skillTemplateProcessor;
    private final ToolRequestSpecificationParser toolRequestSpecificationParser;
    private final IMcpToolInvoker mcpToolInvoker;

    @Inject
    public ToolDirectiveExecutor(SkillTemplateProcessor skillTemplateProcessor,
        ToolRequestSpecificationParser toolRequestSpecificationParser, IMcpToolInvoker mcpToolInvoker)
    {
        Preconditions.checkNotNull(skillTemplateProcessor);
        Preconditions.checkNotNull(toolRequestSpecificationParser);
        Preconditions.checkNotNull(mcpToolInvoker);

        this.skillTemplateProcessor = skillTemplateProcessor;
        this.toolRequestSpecificationParser = toolRequestSpecificationParser;
        this.mcpToolInvoker = mcpToolInvoker;
    }

    @Override
    public CompletableFuture<String> executeAsync(CachedSkill skill, String toolId, Map<String, String> parameters,
        ICancellationToken cancellationToken)
    {
        try
        {
            var rawJson = skill.getToolRequestTemplate(toolId);
            var resolvedJson = skillTemplateProcessor.resolveJsonPlaceholders(rawJson, parameters);
            var request = toolRequestSpecificationParser.parse(toolId, resolvedJson);

            return mcpToolInvoker.invokeAsync(request, cancellationToken).handle((result, ex) -> {
                if (ex != null)
                {
                    throw new SkillExecutionException(SkillErrorCode.TOOL_EXECUTION_ERROR,
                        "Failed to execute tool " + toolId, unwrapException(ex)); //$NON-NLS-1$
                }
                return result;
            });
        }
        catch (SkillExecutionException se)
        {
            return CompletableFuture.failedFuture(se);
        }
        catch (Exception e)
        {
            return CompletableFuture.failedFuture(new SkillExecutionException(SkillErrorCode.TOOL_EXECUTION_ERROR,
                "Failed to execute tool " + toolId, e)); //$NON-NLS-1$
        }
    }

    private Throwable unwrapException(Throwable ex)
    {
        if (ex instanceof CompletionException && ex.getCause() != null)
        {
            return ex.getCause();
        }
        return ex;
    }

}
