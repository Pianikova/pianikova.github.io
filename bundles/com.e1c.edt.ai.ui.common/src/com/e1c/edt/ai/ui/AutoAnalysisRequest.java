/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IFile;

import com.e1c.edt.ai.assistent.model.ProjectId;

/**
 * Запрос для автоматического фонового анализа кода.
 *
 * @author Bogdan Sushkov
 */
public class AutoAnalysisRequest
{
    private final IFile file;
    private final ProjectId projectId;

    public AutoAnalysisRequest(IFile file, ProjectId projectId)
    {
        this.file = file;
        this.projectId = projectId;
    }

    public IFile getFile()
    {
        return file;
    }

    public ProjectId getProjectId()
    {
        return projectId;
    }
}
