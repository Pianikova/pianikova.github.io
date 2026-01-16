/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

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
        toolBinder.addBinding().to(ExecuteMcpTool.class);
        toolBinder.addBinding().to(GetProjectsMcpTool.class);
        toolBinder.addBinding().to(GetCommandCategoriesMcpTool.class);
        toolBinder.addBinding().to(GetCommandsMcpTool.class);
        // toolBinder.addBinding().to(ExecuteCommandMcpTool.class);
        toolBinder.addBinding().to(ReadMcpTool.class);
        toolBinder.addBinding().to(FindMcpTool.class);
        toolBinder.addBinding().to(WriteMcpTool.class);
        toolBinder.addBinding().to(EditMcpTool.class);
        toolBinder.addBinding().to(GetMarkersMcpTool.class);
        toolBinder.addBinding().to(SetMarkersMcpTool.class);
        toolBinder.addBinding().to(ClearMarkersMcpTool.class);
        bind(IProcessRunner.class).to(ProcessRunner.class).in(Singleton.class);
        bind(ICancellationProgressMonitor.class).to(CancellationProgressMonitor.class);
        bind(IBuildWaiter.class).to(BuildWaiter.class).in(Singleton.class);
    }
}