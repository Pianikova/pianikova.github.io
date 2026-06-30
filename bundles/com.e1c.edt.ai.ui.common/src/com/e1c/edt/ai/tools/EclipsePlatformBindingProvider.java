/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.PlatformUI;

import com.google.inject.Singleton;

/**
 * Provides JShell bindings for Eclipse platform services.
 */
@Singleton
public class EclipsePlatformBindingProvider
	implements IJShellBindingProvider, IJShellManualProvider
{
    @Override
    public String getScope()
    {
        return "eclipse"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public Map<String, JShellBindingDescription> getBindings()
	{
        var bindings = new HashMap<String, JShellBindingDescription>();
        var workbench = PlatformUI.getWorkbench();
		if (workbench != null)
		{
            bindings.put("workbench", new JShellBindingDescription("Eclipse workbench instance",
                "var activeWindow = workbench.getActiveWorkbenchWindow();\nSystem.out.println(\"Active window: \" + activeWindow);\nvar activePage = activeWindow != null ? activeWindow.getActivePage() : null;\nSystem.out.println(\"Active page: \" + activePage);",
                workbench, org.eclipse.ui.IWorkbench.class));

            var commandService = workbench.getService(org.eclipse.ui.commands.ICommandService.class);
            if (commandService != null)
            {
                bindings.put("commandService", new JShellBindingDescription(
                    "Eclipse ICommandService: lookup Command objects and build ParameterizedCommand",
                    "var cmd = commandService.getCommand(\"org.eclipse.ui.file.save\");\nSystem.out.println(\"defined=\" + cmd.isDefined());",
                    commandService, org.eclipse.ui.commands.ICommandService.class));
            }

            var handlerService = workbench.getService(org.eclipse.ui.handlers.IHandlerService.class);
            if (handlerService != null)
            {
                bindings.put("handlerService", new JShellBindingDescription(
                    "Eclipse IHandlerService: execute commands and obtain the live IEvaluationContext",
                    "Object result = handlerService.executeCommand(\"org.eclipse.ui.file.save\", null);\nSystem.out.println(\"result=\" + result);",
                    handlerService, org.eclipse.ui.handlers.IHandlerService.class));
            }
		}

        // Workspace access
        var root = ResourcesPlugin.getWorkspace().getRoot();
        if (root != null)
        {
            bindings.put("workspaceRoot",
                new JShellBindingDescription("Eclipse workspace root for accessing all projects",
                    buildWorkspaceRootDescription(), root, org.eclipse.core.resources.IWorkspaceRoot.class));
        }

		return bindings;
	}

    @Override
    public String getDescription()
    {
        return "Eclipse platform services (workbench, UI, resources)"; //$NON-NLS-1$
    }

    @Override
    @SuppressWarnings("nls")
    public String getUseCases()
    {
        return "- Access Eclipse workbench and UI components" +
                "\n- Get active editor, windows, pages" +
                "\n- Execute Eclipse commands programmatically" +
                "\n- Access Eclipse resources and preferences";
    }

    @Override
    public Collection<JShellManualEntry> getManualEntries()
    {
        return List.of(
            new JShellManualEntry(
                "eclipse_overview", //$NON-NLS-1$
                "eclipse", //$NON-NLS-1$
                "Eclipse JShell Overview", //$NON-NLS-1$
                "Choose bindings and guardrails for Eclipse UI and workspace automation.", //$NON-NLS-1$
                buildOverviewManual(),
                List.of("workbench", "workspaceRoot"), //$NON-NLS-1$ //$NON-NLS-2$
                List.of("overview", "eclipse", "ui", "workspace", "bindings") ), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            new JShellManualEntry(
                "active_workbench", //$NON-NLS-1$
                "eclipse", //$NON-NLS-1$
                "Inspect Active Workbench UI", //$NON-NLS-1$
                "Read active window, page, and editor from the running Eclipse UI.", //$NON-NLS-1$
                buildActiveWorkbenchManual(),
                List.of("workbench"), //$NON-NLS-1$
                List.of("active window", "active editor", "workbench", "selection") ), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            new JShellManualEntry(
                "workspace_projects", //$NON-NLS-1$
                "eclipse", //$NON-NLS-1$
                "Inspect Workspace Projects", //$NON-NLS-1$
                "Enumerate projects and resources through Eclipse workspace APIs.", //$NON-NLS-1$
                buildWorkspaceProjectsManual(),
                List.of("workspaceRoot"), //$NON-NLS-1$
                List.of("workspace", "project", "resource", "projects")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            new JShellManualEntry(
                "execute_command", //$NON-NLS-1$
                "eclipse", //$NON-NLS-1$
                "Execute IDE Commands via JShell", //$NON-NLS-1$
                "Run any Eclipse command with parameterless, string, or arbitrary typed arguments.", //$NON-NLS-1$
                buildExecuteCommandManual(),
                List.of("commandService", "handlerService", "workbench"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                List.of("command", "execute command", "handler", "ide action", "run command", "parameterized")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        );
    }

    @Override
    public Collection<Class<?>> getSignificantClasses()
    {
        return List.of(
            org.eclipse.core.runtime.IAdaptable.class, org.eclipse.ui.IWorkbench.class,
            org.eclipse.swt.widgets.Display.class,
            org.eclipse.swt.widgets.Shell.class,
            org.eclipse.ui.IWorkbenchWindow.class,
            org.eclipse.ui.IWorkbenchPage.class,
            org.eclipse.ui.IEditorPart.class,
            org.eclipse.jface.operation.IRunnableContext.class,
            org.eclipse.core.runtime.IAdaptable.class,
            org.eclipse.core.runtime.IProgressMonitor.class,
            org.eclipse.core.commands.common.EventManager.class, java.io.ByteArrayInputStream.class,
            org.eclipse.ui.commands.ICommandService.class, org.eclipse.ui.handlers.IHandlerService.class,
            org.eclipse.core.commands.Command.class, org.eclipse.core.commands.ParameterizedCommand.class,
            org.eclipse.core.commands.ExecutionEvent.class, org.eclipse.core.expressions.IEvaluationContext.class,
            org.eclipse.core.expressions.EvaluationContext.class
        );
    }

    @SuppressWarnings("nls")
    @Override
    public Collection<String> getImports()
    {
        // @formatter:off
		return List.of(
            "import org.eclipse.swt.SWT;",
            "import org.eclipse.swt.widgets.Display;",
            "import org.eclipse.swt.widgets.Shell;",
            "import org.eclipse.ui.IEditorPart;",
            "import org.eclipse.ui.IWorkbench;",
            "import org.eclipse.ui.IWorkbenchPage;",
            "import org.eclipse.ui.IWorkbenchWindow;",
            "import org.eclipse.jface.operation.IRunnableContext;",
            "import org.eclipse.core.runtime.IAdaptable;",
            "import org.eclipse.core.runtime.IProgressMonitor;",
            "import org.eclipse.core.commands.common.EventManager;",
			"import java.util.UUID;",
			"import java.io.ByteArrayInputStream;",
            "import java.io.UnsupportedEncodingException;",
            "import org.eclipse.ui.commands.ICommandService;",
            "import org.eclipse.ui.handlers.IHandlerService;",
            "import org.eclipse.core.commands.Command;",
            "import org.eclipse.core.commands.ParameterizedCommand;",
            "import org.eclipse.core.commands.ExecutionEvent;",
            "import org.eclipse.core.expressions.IEvaluationContext;",
            "import org.eclipse.core.expressions.EvaluationContext;"
		);
		// @formatter:on
    }

    @SuppressWarnings("nls")
    private String buildWorkspaceRootDescription()
    {
        var desc = new StringBuilder();
        desc.append("## IWorkspaceRoot - Workspace Root\n\n");
        desc.append("Eclipse workspace root for accessing all projects.\n\n");
        desc.append("### Get Project by Name\n");
        desc.append("```java\n");
        desc.append("// Get project by name\n");
        desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("if (project.exists()) {\n");
        desc.append("    System.out.println(\"Project exists: \" + project.getName());\n");
        desc.append("}\n");
        desc.append("```\n\n");

        desc.append("### Get All Projects\n");
        desc.append("```java\n");
        desc.append("// Get all projects in workspace\n");
        desc.append("IProject[] projects = workspaceRoot.getProjects();\n");
        desc.append("for (IProject project : projects) {\n");
        desc.append("    System.out.println(\"Project: \" + project.getName());\n");
        desc.append("}\n");
        desc.append("```\n\n");

        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildOverviewManual()
    {
        var desc = new StringBuilder();
        desc.append("## When to use\n\n");
        desc.append(getUseCases());
        desc.append("\n\n## Recommended bindings\n");
        desc.append("- `workbench`: access windows, pages, editors, and Eclipse UI state\n");
        desc.append("- `workspaceRoot`: access projects and workspace resources\n");
        desc.append("- `commandService`: look up IDE commands and build ParameterizedCommand\n");
        desc.append("- `handlerService`: execute IDE commands (see manual `execute_command`)\n");
        desc.append("\n## Rules\n");
        desc.append("- Prefer inspection first, mutation second\n");
        desc.append("- Guard against `null` active window/page/editor\n");
        desc.append("- Use `System.out.println(...)` for visible JShell output\n");
        desc.append("- Resolve `IProject` from `workspaceRoot` before passing it to project-specific APIs\n");
        desc.append("\n## Starter snippet\n");
        desc.append("```java\n");
        desc.append("var window = workbench.getActiveWorkbenchWindow();\n");
        desc.append("var page = window != null ? window.getActivePage() : null;\n");
        desc.append("System.out.println(\"Window: \" + window);\n");
        desc.append("System.out.println(\"Page: \" + page);\n");
        desc.append("```\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildExecuteCommandManual()
    {
        var desc = new StringBuilder();
        desc.append("## Scenario: Execute an IDE command via JShell\n\n");
        desc.append("Call `JShell` with `scope: \"eclipse\"` for this workflow.\n\n");
        desc.append("Run any Eclipse/EDT command programmatically. Unlike the legacy command tool, ");
        desc.append("this supports commands that need NON-String (arbitrary typed) arguments.\n\n");

        desc.append("### Recommended bindings\n");
        desc.append("- `commandService` (ICommandService) - look up `Command`, build `ParameterizedCommand`\n");
        desc.append("- `handlerService` (IHandlerService) - execute commands, get the live `IEvaluationContext`\n");
        desc.append("- `workbench` - activate the right window/page/editor when a command needs UI context\n\n");

        desc.append("### Discover the command first\n");
        desc.append("1. `GetCommandCategories` - list categories.\n");
        desc.append("2. `GetCommands` (optionally with `category_id`) - get each command's `id`, declared ");
        desc.append("`parameters` (id/name/optional/value constraints), return type, hotkey.\n");
        desc.append("3. Use the exact `id`; never guess command ids.\n\n");

        desc.append("### Pattern 1 - Parameterless (most common)\n");
        desc.append("```java\n");
        desc.append("Object result = handlerService.executeCommand(\"org.eclipse.ui.file.save\", null);\n");
        desc.append("System.out.println(\"result=\" + result);\n");
        desc.append("return result;\n");
        desc.append("```\n\n");

        desc.append("### Pattern 2 - Declared STRING parameters\n");
        desc.append("```java\n");
        desc.append("var command = commandService.getCommand(\"the.command.id\");\n");
        desc.append("var params = new java.util.HashMap<String, String>();\n");
        desc.append("params.put(\"declaredParamId\", \"stringValue\");\n");
        desc.append("var pc = ParameterizedCommand.generateCommand(command, params);\n");
        desc.append("return handlerService.executeCommand(pc, null);\n");
        desc.append("```\n\n");

        desc.append("### Pattern 3 - ARBITRARY TYPED arguments (no string-only restriction)\n");
        desc.append("Pass any object as a context variable and execute the handler directly, ");
        desc.append("bypassing `ParameterValueConverter` entirely. Start from the live context so ");
        desc.append("handlers still see the current selection / active part.\n");
        desc.append("```java\n");
        desc.append("var command = commandService.getCommand(\"the.command.id\");\n");
        desc.append("var base = handlerService.getCurrentState();              // IEvaluationContext\n");
        desc.append("var ctx = new EvaluationContext(base, base.getDefaultVariable());\n");
        desc.append("ctx.addVariable(\"myArgName\", someTypedObject);            // any Object, not only String\n");
        desc.append("var event = new ExecutionEvent(command, java.util.Collections.emptyMap(), null, ctx);\n");
        desc.append("return command.executeWithChecks(event);\n");
        desc.append("```\n");
        desc.append("The handler reads it via `org.eclipse.ui.handlers.HandlerUtil.getVariable(event, \"myArgName\")` ");
        desc.append("or `event.getApplicationContext()`. Choose names the target handler actually reads.\n\n");

        desc.append("### Return values\n");
        desc.append("Most commands return `null` on success - print success and `return null;`. ");
        desc.append("Otherwise `return` the value so it appears in `return_value`.\n\n");

        desc.append("### Error handling (`org.eclipse.core.commands.*`)\n");
        desc.append("- `ExecutionException` - the handler failed during execution\n");
        desc.append("- `NotDefinedException` (`org.eclipse.core.commands.common.NotDefinedException`) - id not defined\n");
        desc.append("- `NotEnabledException` - command disabled; fix the active part/selection and retry\n");
        desc.append("- `NotHandledException` - no active handler in the current context; activate the right ");
        desc.append("editor/view via `workbench`, then retry\n\n");

        desc.append("### Selection-dependent commands and opening files\n");
        desc.append("Many workbench commands (build, delete, rename, \"Open Resource\") read the LIVE ");
        desc.append("workbench selection / active part, NOT a variable you add to `EvaluationContext`. ");
        desc.append("For these, `executeWithChecks` / `executeCommand` may throw `NotEnabledException` ");
        desc.append("even though you set `selection` in the context. Two robust options: set the real ");
        desc.append("selection first via the active part's selection provider, or call the underlying ");
        desc.append("API directly (usually simpler and deterministic from JShell).\n\n");
        desc.append("Build a project (API):\n");
        desc.append("```java\n");
        desc.append("var project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("project.build(org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD, ");
        desc.append("new org.eclipse.core.runtime.NullProgressMonitor());\n");
        desc.append("System.out.println(\"build triggered for \" + project.getName());\n");
        desc.append("return null;\n");
        desc.append("```\n\n");
        desc.append("Open a file in an editor (API - there is no non-modal command for this):\n");
        desc.append("```java\n");
        desc.append("var project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("org.eclipse.core.resources.IFile file = project.getFile(\".project\");\n");
        desc.append("var window = workbench.getActiveWorkbenchWindow();\n");
        desc.append("var page = window != null ? window.getActivePage() : null;\n");
        desc.append("var editor = org.eclipse.ui.ide.IDE.openEditor(page, file);   // typed IFile argument\n");
        desc.append("System.out.println(\"opened: \" + (editor != null ? editor.getTitle() : \"null\"));\n");
        desc.append("return null;\n");
        desc.append("```\n\n");

        desc.append("### Most-used commands - quick reference\n");
        desc.append("Verified in EDT. Many of these are enabled only with the right active editor/selection; ");
        desc.append("the API path is the most deterministic from JShell. NEVER execute these MODAL commands ");
        desc.append("here - they block the UI thread: `org.eclipse.ui.window.preferences`, ");
        desc.append("`org.eclipse.ui.file.properties`, `org.eclipse.ui.navigate.openResource`, ");
        desc.append("`org.eclipse.ui.project.cleanAction`.\n\n");
        desc.append("- Save all editors -> API: `page.saveAllEditors(false)`\n");
        desc.append("- Close all editors -> command (Pattern 1): `org.eclipse.ui.file.closeAll`\n");
        desc.append("- Refresh -> API: `resource.refreshLocal(IResource.DEPTH_INFINITE, monitor)`\n");
        desc.append("- Build project -> API: `project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor)`\n");
        desc.append("- Build all (workspace) -> API: ");
        desc.append("`ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor)`\n");
        desc.append("- Clean project -> API: `project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor)` ");
        desc.append("(the `cleanAction` command opens a modal dialog)\n");
        desc.append("- Open a view -> command (Pattern 2): `org.eclipse.ui.views.showView`, ");
        desc.append("param `org.eclipse.ui.views.showView.viewId`\n");
        desc.append("- Open a file -> API: `org.eclipse.ui.ide.IDE.openEditor(page, file)` (no non-modal command exists)\n");
        desc.append("- Delete resource -> API: `resource.delete(true, monitor)`\n");
        desc.append("- Rename/move resource -> API: `resource.move(newFullPath, true, monitor)`\n\n");
        desc.append("Verified safe batch (save all, close all, refresh, build):\n");
        desc.append("```java\n");
        desc.append("var npm = new org.eclipse.core.runtime.NullProgressMonitor();\n");
        desc.append("var win = workbench.getActiveWorkbenchWindow();\n");
        desc.append("var page = win != null ? win.getActivePage() : null;\n");
        desc.append("if (page != null) page.saveAllEditors(false);\n");
        desc.append("handlerService.executeCommand(\"org.eclipse.ui.file.closeAll\", null);\n");
        desc.append("var project = workspaceRoot.getProject(\"MyProject\");\n");
        desc.append("project.refreshLocal(org.eclipse.core.resources.IResource.DEPTH_INFINITE, npm);\n");
        desc.append("project.build(org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD, npm);\n");
        desc.append("return null;\n");
        desc.append("```\n\n");

        desc.append("### Rules\n");
        desc.append("- Discover the id with `GetCommands` before executing; do not invent ids.\n");
        desc.append("- Prefer Pattern 1; use Pattern 3 only when an argument is not a String.\n");
        desc.append("- Start the EvaluationContext from `handlerService.getCurrentState()` to preserve ");
        desc.append("the selection / active part.\n");
        desc.append("- Code already runs on the UI thread - do NOT add `Display.syncExec`.\n");
        desc.append("- For selection-dependent commands (build/delete/rename) a fresh `EvaluationContext` ");
        desc.append("is often not enough - set the live selection or call the underlying API.\n");
        desc.append("- Always end with `return <value>;` (use `return null;` when there is nothing to return).\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildActiveWorkbenchManual()
    {
        var desc = new StringBuilder();
        desc.append("## Scenario: Inspect active workbench state\n\n");
        desc.append("### Recommended bindings\n");
        desc.append("- `workbench`\n\n");
        desc.append("### Safe workflow\n");
        desc.append("1. Read the active window.\n");
        desc.append("2. Read the active page.\n");
        desc.append("3. Read the active editor.\n");
        desc.append("4. Print all values before taking actions.\n\n");
        desc.append("### Example\n");
        desc.append("```java\n");
        desc.append("var window = workbench.getActiveWorkbenchWindow();\n");
        desc.append("var page = window != null ? window.getActivePage() : null;\n");
        desc.append("var editor = page != null ? page.getActiveEditor() : null;\n");
        desc.append("System.out.println(\"Window: \" + window);\n");
        desc.append("System.out.println(\"Page: \" + page);\n");
        desc.append("System.out.println(\"Editor: \" + editor);\n");
        desc.append("```\n\n");
        desc.append("### Common mistakes\n");
        desc.append("- Assuming `workbench.getActiveWorkbenchWindow()` is never `null`\n");
        desc.append("- Calling editor-specific APIs without checking the editor input type\n");
        return desc.toString();
    }

    @SuppressWarnings("nls")
    private String buildWorkspaceProjectsManual()
    {
        var desc = new StringBuilder();
        desc.append("## Scenario: Inspect workspace projects\n\n");
        desc.append("### Recommended bindings\n");
        desc.append("- `workspaceRoot`\n\n");
        desc.append("### Example\n");
        desc.append("```java\n");
        desc.append("for (var project : workspaceRoot.getProjects()) {\n");
        desc.append("    System.out.println(project.getName() + \" | open=\" + project.isOpen());\n");
        desc.append("}\n");
        desc.append("```\n\n");
        desc.append("### Notes\n");
        desc.append("- Check `project.exists()` and `project.isOpen()` before deeper operations\n");
        desc.append("- Pass the resolved `IProject` into EDT bindings like `projectManager`\n\n");
        desc.append(buildWorkspaceRootDescription());
        return desc.toString();
    }
}
