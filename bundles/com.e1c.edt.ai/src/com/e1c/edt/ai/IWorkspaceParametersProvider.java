/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.WorkspaceParameters;

/**
 * Поставщик параметров рабочего окружения (workspace) для запроса создания сессии.
 */
public interface IWorkspaceParametersProvider
{
    WorkspaceParameters get();
}
