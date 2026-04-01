/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class ExecuteMcpTool
    extends BaseExecuteMcpTool<ExecuteMcpTool.Request>
    implements IMcpTool
{
    public static final String TOOL_NAME = "Execute"; //$NON-NLS-1$

    private static String QuestionExample =
        "{\"executable\":\"cmd\",\"working_directory\":\"C:\\\\\",\"args\":[\"/c\",\"whoami\"],\"timeout\":3}"; //$NON-NLS-1$

    private static String AnswerExample =
        "{\"exit_code\":0,\"std_out\":\"john_smith\\n\",\"std_err\":\"\"}"; //$NON-NLS-1$

    private final McpToolCallSpecification spec;

    @Inject
    public ExecuteMcpTool(IEnvironment environment, IJson json, IMcpToolsCallMessageFactory messageFactory,
        IProcessRunner processRunner)
    {
        super(environment, json, messageFactory, processRunner);
        spec = createSpecification();
    }

    @Override
    protected String getExecutable(Request request)
    {
        return request.executable;
    }

    @Override
    protected Class<Request> getRequestType()
    {
        return Request.class;
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

    @SuppressWarnings("nls")
    @Override
    protected void validateRequest(Request request) throws ToolException
    {
        if (request.executable == null || request.executable.isBlank())
        {
            throw new ToolException("`executable` cannot be empty.");
        }
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
        var description = new StringBuilder();
        description.append("Executes a system process.");
        description.append("\n\nUse for OS-level commands, not IDE actions.");
        description.append("\n\nRelated tools:");
        description.append("\n- IDE commands: `" + ExecuteCommandMcpTool.TOOL_NAME + "`.");
        return description.toString();
    }

    @SuppressWarnings("nls")
    @Override
    protected void addToolSpecificProperties(HashMap<String, McpToolCallProperty> properties)
    {
        var executableProp = new McpToolCallProperty();
        executableProp.type = "string";
        executableProp.description = "Executable name or path.";
        properties.put("executable", executableProp);
    }

    @SuppressWarnings("nls")
    @Override
    protected List<String> getRequiredParameters()
    {
        return Arrays.asList("executable");
    }

    @Override
    public McpToolCallSpecification getSpecification()
    {
        return spec;
    }

    public static class Request
        extends BaseExecuteRequest
    {
        @SerializedName("executable")
        public String executable;
    }
}

