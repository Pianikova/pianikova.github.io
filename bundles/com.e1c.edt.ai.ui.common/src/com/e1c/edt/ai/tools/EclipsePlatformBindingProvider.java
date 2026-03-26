/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.PlatformUI;

import com.google.inject.Singleton;

/**
 * Provides JShell bindings for Eclipse platform services.
 */
@Singleton
public class EclipsePlatformBindingProvider
	implements IJShellBindingProvider
{
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
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        bindings.put("workspaceRoot", new JShellBindingDescription("Eclipse workspace root for accessing all projects",
            buildWorkspaceRootDescription(), root, org.eclipse.core.resources.IWorkspaceRoot.class));

		return bindings;
	}

    @Override
    public String getDescription()
    {
        return "Eclipse platform services (workbench, UI, resources)";
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
            org.eclipse.core.commands.common.EventManager.class
        );
    }

    @SuppressWarnings("nls")
    @Override
    public Collection<String> getImports()
    {
        // @formatter:off
		return List.of(
            "import org.eclipse.swt.widgets.*;",
			"import org.eclipse.swt.*;",
			"import org.eclipse.ui.*;",
			"import org.eclipse.jface.operation.*;",
			"import org.eclipse.core.runtime.*;",
			"import org.eclipse.core.commands.common.*;"
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
}
