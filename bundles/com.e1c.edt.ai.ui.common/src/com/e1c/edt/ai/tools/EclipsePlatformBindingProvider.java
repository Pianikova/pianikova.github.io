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
                List.of("workspace", "project", "resource", "projects")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
            org.eclipse.core.commands.common.EventManager.class, java.io.ByteArrayInputStream.class
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
            "import java.io.UnsupportedEncodingException;"
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
        desc.append("\n## Rules\n");
        desc.append("- Prefer inspection first, mutation second\n");
        desc.append("- Guard against `null` active window/page/editor\n");
        desc.append("- Use `System.out.println(...)` for visible JShell output\n");
        desc.append("- Resolve `IProject` from `workspaceRoot` before moving into EDT metadata APIs\n");
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
