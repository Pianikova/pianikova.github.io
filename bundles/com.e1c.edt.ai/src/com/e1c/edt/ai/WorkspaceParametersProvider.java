/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.UUID;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.InstanceScope;

import com.e1c.edt.ai.assistent.model.WorkspaceParameters;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Возвращает параметры текущего рабочего окружения (workspace). Идентификатор генерируется
 * один раз и сохраняется в настройках workspace ({@link InstanceScope}), поэтому он стабилен
 * между перезапусками EDT и уникален для каждого рабочего окружения.
 */
@Singleton
public class WorkspaceParametersProvider
    implements IWorkspaceParametersProvider
{
    private static final String PREFERENCE_NODE = "com.e1c.edt.ai"; //$NON-NLS-1$
    private static final String WORKSPACE_UUID_KEY = "workspaceUuid"; //$NON-NLS-1$

    private final ILog log;

    private String uuid;

    @Inject
    public WorkspaceParametersProvider(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @Override
    public WorkspaceParameters get()
    {
        var parameters = new WorkspaceParameters();
        parameters.uuid = getOrCreateUuid();

        var location = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        if (location != null)
        {
            parameters.path = location.toOSString();
        }

        return parameters;
    }

    private synchronized String getOrCreateUuid()
    {
        if (uuid != null)
        {
            return uuid;
        }

        var node = InstanceScope.INSTANCE.getNode(PREFERENCE_NODE);
        var stored = node.get(WORKSPACE_UUID_KEY, null);
        if (stored != null && !stored.isBlank())
        {
            uuid = stored;
            return uuid;
        }

        var generated = UUID.randomUUID().toString();
        node.put(WORKSPACE_UUID_KEY, generated);
        try
        {
            node.flush();
        }
        catch (Exception e)
        {
            log.logError(e);
        }

        uuid = generated;
        return uuid;
    }
}
