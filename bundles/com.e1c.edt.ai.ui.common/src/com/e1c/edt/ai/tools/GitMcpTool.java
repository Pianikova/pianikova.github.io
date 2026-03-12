/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.google.inject.Inject;

public class GitMcpTool
    extends BaseExecuteMcpTool<BaseExecuteRequest>
	implements IMcpTool
{
	public static final String TOOL_NAME = "Git"; //$NON-NLS-1$

	private static String QuestionExample =
		"{\"working_directory\":\"C:\\\\Projects\",\"args\":[\"status\"]}"; //$NON-NLS-1$

	private static String AnswerExample =
		"{\"exit_code\":0,\"std_out\":\"On branch main\\n\",\"std_err\":\"\"}"; //$NON-NLS-1$

	private final McpToolCallSpecification spec;

	@Inject
	public GitMcpTool(IEnvironment environment, IJson json, IMcpToolsCallMessageFactory messageFactory,
		IProcessRunner processRunner)
	{
		super(environment, json, messageFactory, processRunner);
		spec = createSpecification();
	}

	@Override
    protected String getExecutable(BaseExecuteRequest request)
	{
		return "git"; //$NON-NLS-1$
	}

	@Override
    protected Class<BaseExecuteRequest> getRequestType()
	{
        return BaseExecuteRequest.class;
	}

	@Override
	protected String getQuestionExample()
	{
		return QuestionExample;
	}

	@Override
	protected String getAnswerExample()
	{
		return AnswerExample;
	}

	@Override
    protected void validateRequest(BaseExecuteRequest request) throws ToolException
	{
		// Git executable is hardcoded, no validation needed
	}

	@Override
	protected String getToolName()
	{
		return TOOL_NAME;
	}

	@SuppressWarnings("nls")
	@Override
	protected String getToolDescription()
	{
        return "Executes Git commands." +
                "\n\nUse for Git operations in repositories.";
	}

	@Override
	protected void addToolSpecificProperties(HashMap<String, McpToolCallProperty> properties)
	{
		// No git-specific properties needed, using only BaseExecuteRequest properties
	}

	@Override
	protected List<String> getRequiredParameters()
	{
		// No required parameters since executable is hardcoded
		return List.of();
	}

	@Override
	public boolean isExperimental()
	{
        return false;
	}

	@Override
	public McpToolCallSpecification getSpecification()
	{
		return spec;
    }

    @Override
    public CompletableFuture<Boolean> getIsAvailable()
    {
        return processRunner.executeProcess("git", null, List.of("--version"), 5L, TimeUnit.SECONDS, null) //$NON-NLS-1$ //$NON-NLS-2$
            .thenApply(result -> result.isPresent() && result.get().exitCode == 0);
    }
}
