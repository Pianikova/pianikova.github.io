/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;


/**
 * Запрос для автоматического фонового анализа кода.
 *
 * @author Bogdan Sushkov
 */
public class AutoAnalysisRequest
{
    private final IFile file;
    private final IProject project;

    public AutoAnalysisRequest(IFile file, IProject project)
    {
        this.file = file;
        this.project = project;
    }

    public IFile getFile()
    {
        return file;
    }

    public IProject getProject()
    {
        return project;
    }
}
