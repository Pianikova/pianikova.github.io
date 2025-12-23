/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.IBmTask;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.form.model.Form;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;


public class GetObjectByIdMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "1с_ide_get_object_by_id"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"Проект_1\",\n"
        + "  \"object_id\": 9876543210\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "JSON object";

    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IBmModelManager modelManager;
    private final IEntityFactory entityFactory;
    private final IProjectFileSystemSupportProvider projectFileSystemSupportProvider;

    @Inject
    public GetObjectByIdMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IBmModelManager modelManager,
        IEntityFactory entityFactory, IProjectFileSystemSupportProvider projectFileSystemSupportProvider)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(modelManager);
        Preconditions.checkNotNull(entityFactory);
        Preconditions.checkNotNull(projectFileSystemSupportProvider);
        this.json = json;
        this.messageFactory = messageFactory;
        this.modelManager = modelManager;
        this.entityFactory = entityFactory;
        this.projectFileSystemSupportProvider = projectFileSystemSupportProvider;
        spec = createSpecification();
    }

    @Override
    public boolean isExperimental()
    {
        return true;
    }

    @Override
    public McpToolCallSpecification getSpecification()
    {
        return spec;
    }

    @SuppressWarnings({ "nls" })
    @Override
    public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken)
    {
        var optionalCallArgs = json.deserialize(call.function.arguments, CallArguments.class);
        if (optionalCallArgs.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "Cannot deserialize arguments. Use this example: " + QuestionExample));
        }

        var callArgs = optionalCallArgs.get();
        var projectName = callArgs.projectName;
        var objectId = callArgs.objectId;

        if (projectName == null || projectName.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "Project is required."));
        }

        if (objectId == null)
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "Object id is required."));
        }

        // Return a CompletableFuture that will be completed asynchronously
        return CompletableFuture.supplyAsync(() -> {
            // Check for cancellation before starting the heavy operation
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var root = ResourcesPlugin.getWorkspace().getRoot();
            var project = root.getProject(projectName);
            if (project == null)
            {
                return messageFactory.createError(this, call, "Project not found: " + projectName);
            }

            var model = modelManager.getModel(project);
            var bmObject = model.executeReadonlyTask(new IBmTask<IBmObject>()
            {
                @Override
                public IBmObject execute(IBmTransaction arg0, IProgressMonitor arg1)
                {
                    return arg0.getObjectById(objectId);
                }

                @Override
                public Object getId()
                {
                    return call.id;
                }

                @Override
                public String getName()
                {
                    return call.id;
                }

                @Override
                public Object getServiceId()
                {
                    return "GetObjectByIdMcpTool";
                }
            });

            if (bmObject == null)
            {
                return messageFactory.createError(this, call, "Object not found: " + objectId);
            }

            Object obj = null;
            if (bmObject instanceof Form)
            {
                obj = entityFactory.createFormEntity((Form)bmObject, cancellationToken).orElse(null);
            }
            else
            {
                obj = entityFactory.createMetaEntity(bmObject, cancellationToken);
            }

            var result = new Result();
            if (obj != null)
            {
                var fileSystem = projectFileSystemSupportProvider.getProjectFileSystemSupport(project);
                if (fileSystem != null)
                {
                    var file = fileSystem.getFile(bmObject);
                    if (file != null)
                    {
                        var location = file.getRawLocation();
                        if (location != null)
                        {
                            result.absoluteFilePath = location.toOSString();
                        }

                        var relativePath = file.getProjectRelativePath();
                        if (relativePath != null)
                        {
                            result.projectRelativeFilePath = relativePath.toOSString();
                        }

                    }
                }
            }
            else
            {
                result.objectModel = "";
            }

            return json.serialize(result);
        }).handle((result, ex) -> {
            if (ex != null)
            {
                // Handle exceptions from the async block
                String errorMessage = ex.getCause() instanceof CoreException || ex.getCause() instanceof OperationCanceledException
                    ? "Cannot search. " + ex.getMessage()
                    : ex.getMessage();

                return messageFactory.createError(this, call, errorMessage);
            }

            // Handle successful result
            var content = json.serialize(result);
            return messageFactory.createMessage(this, call, content);
        });
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification()
    {
     // @formatter:off
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        var description = new StringBuilder();

        description.append("Returns 1C object by its id.");

        description.append("\nFor exapmple:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);

        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new HashMap<String, McpToolCallProperty>();

        var searchQueryProp = new McpToolCallProperty();
        searchQueryProp.type = "string";
        searchQueryProp.description = "1C project name.";
        properties.put("project_name", searchQueryProp);

        var projectsDirProp = new McpToolCallProperty();
        projectsDirProp.type = "number";
        projectsDirProp.description = "1C object id.";
        properties.put("object_id", projectsDirProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("project_name", "object_id");

        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }

    private static class CallArguments
    {
        /**
         * Defines project name.
         */
        @SerializedName("project_name")
        public String projectName;

        /**
         * Defines object id.

         */
        @SerializedName("object_id")
        public Long objectId;
    }

    private static class Result
    {
        @SerializedName("object_model")
        public Object objectModel;

        @SerializedName("absolute_file_path")
        public Object absoluteFilePath;

        @SerializedName("project_relative_file _path")
        public Object projectRelativeFilePath;
    }
}