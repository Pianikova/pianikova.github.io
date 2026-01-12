/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpTools;
import com.e1c.edt.ai.McpTools;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class MCPToolsModule
    extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(IMcpTools.class).to(McpTools.class).in(Singleton.class);
        var toolBinder = Multibinder.newSetBinder(binder(), IMcpTool.class);
        toolBinder.addBinding().to(ProcessRunnerMcpTool.class);
        toolBinder.addBinding().to(GetProjectsMcpTool.class);
        toolBinder.addBinding().to(GetCommandCategoriesMcpTool.class);
        toolBinder.addBinding().to(GetCommandsMcpTool.class);
        // toolBinder.addBinding().to(ExecuteCommandsMcpTool.class);
        toolBinder.addBinding().to(ReadProjectFileMcpTool.class);
        toolBinder.addBinding().to(FileFindMcpTool.class);
        bind(IProcessRunner.class).to(ProcessRunner.class).in(Singleton.class);
        bind(IProgressMonitor.class).to(ToolProgressMonitor.class).in(Singleton.class);
    }
}