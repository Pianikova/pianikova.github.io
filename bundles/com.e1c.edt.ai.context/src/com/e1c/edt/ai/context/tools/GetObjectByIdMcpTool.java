/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.context.tools;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;

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
import com.e1c.edt.ai.context.IEntityFactory;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class GetObjectByIdMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "1C_GetObjectById"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"project_name\": \"MyProject\",\n"
        + "  \"object_id\": 17239405821723\n"
        + "}";

    @SuppressWarnings("nls")
    private static String AnswerExample =
        "{\n"
        + "  \"resoure_uri\": \"bm:...\",\n"
        + "  \"fqn\": \"Catalog.MyCatalog\",\n"
        + "  \"is_top\": true,\n"
        + "  \"relative_file_path\": \"Catalogs/MyCatalog/Forms/ItemForm/Ext/Module.bsl\"\n"
        + "}";
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
        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "Cannot deserialize arguments. Use this example: " + QuestionExample));
        }

        var request = optionalRequest.get();
        var projectName = request.projectName;
        var objectId = request.objectId;

        if (projectName == null || projectName.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "Project name is required."));
        }

        if (objectId == null)
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "Object id is required."));
        }

        return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled before execution.");
            }

            var root = ResourcesPlugin.getWorkspace().getRoot();
            var project = root.getProject(projectName);

            if (project == null || !project.exists())
            {
                return messageFactory.createError(this, call, "Project not found: " + projectName);
            }

            var model = modelManager.getModel(project);
            if (model == null)
            {
                return messageFactory.createError(this, call, "Model not available for project: " + projectName);
            }

            try
            {
                var bmObject = model.executeReadonlyTask(new IBmTask<IBmObject>()
                {
                    @Override
                    public IBmObject execute(IBmTransaction transaction, IProgressMonitor progressMonitor)
                    {
                        // Check cancellation inside BM task
                        if (cancellationToken.isCanceled())
                        {
                            throw new OperationCanceledException("Operation cancelled during BM task execution");
                        }
                        return transaction.getObjectById(objectId);
                    }

                    @Override
                    public Object getId()
                    {
                        return "GetObjectByIdMcpTool/" + objectId;
                    }

                    @Override
                    public String getName()
                    {
                        return "Get object by id: " + objectId;
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

                var response = new Response();
                response.resoureUri = bmObject.bmGetUriAsString();
                response.fqn = bmObject.bmGetFqn();
                response.isTop = bmObject.bmIsTop();

                var topObject = bmObject.bmGetTopObject();
                if (topObject != null)
                {
                    response.topObjectId = topObject.bmGetId();
                }

                var fileSystem = projectFileSystemSupportProvider.getProjectFileSystemSupport(project);
                if (fileSystem != null)
                {
                    var file = fileSystem.getFile(bmObject);
                    if (file != null)
                    {
                        var location = file.getRawLocation();
                        if (location != null)
                        {
                            response.absoluteFilePath = location.toOSString();
                        }

                        var relativePath = file.getProjectRelativePath();
                        if (relativePath != null)
                        {
                            response.relativeFilePath = relativePath.toPortableString();

                            // Build filePath safely without double slashes
                            var filePath = new Path(project.getName()).append(relativePath);
                            response.filePath = filePath.toPortableString();
                        }
                    }
                }

                if (cancellationToken.isCanceled())
                {
                    return messageFactory.createError(this, call, "Operation was cancelled during entity creation.");
                }

                if (bmObject instanceof Form)
                {
                    response.objectModel = entityFactory.createFormEntity((Form)bmObject, cancellationToken).orElse(null);
                }
                else
                {
                    response.objectModel = entityFactory.createMetaEntity(bmObject, cancellationToken);
                }

                var content = json.serialize(response);
                return messageFactory.createMessage(this, call, content);
            }
            catch (OperationCanceledException e)
            {
                return messageFactory.createError(this, call, "Cannot get object by id: " + e.getMessage());
            }
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
        description.append("Returns 1C configuration object by its unique ID.");
        description.append("\nFor example:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);
        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new HashMap<String, McpToolCallProperty>();

        var projectNameProp = new McpToolCallProperty();
        projectNameProp.type = "string";
        projectNameProp.description = "1C project name where the object is located.";
        properties.put("project_name", projectNameProp);

        var objectIdProp = new McpToolCallProperty();
        objectIdProp.type = "number";
        objectIdProp.description = "Unique identifier of the 1C configuration object.";
        properties.put("object_id", objectIdProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("project_name", "object_id");

        spec.function.parameters = parameters;
        return spec;
        // @formatter:on
    }

    private static class Request
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

    private static class Response
    {
        @SerializedName("resoure_uri")
        public String resoureUri;

        @SerializedName("fqn")
        public String fqn;

        @SerializedName("is_top")
        public boolean isTop;

        @SerializedName("top_object_id")
        public Long topObjectId; // Changed to Long to support null

        @SerializedName("object_model")
        public Object objectModel;

        @SerializedName("relative_file_path")
        public String relativeFilePath;

        @SerializedName("file_path")
        public String filePath; // Fixed serialization name

        @SerializedName("absolute_file_path")
        public String absoluteFilePath;
    }
}
