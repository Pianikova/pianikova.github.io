/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import com.google.inject.Inject;

/**
 * Триггер фонового анализа изменений в коде.
 * Подписывается на события сохранения файлов и запускает анализ.
 *
 * @author Bogdan Sushkov
 */
public class BackgroundAnalysisTrigger
    implements IResourceChangeListener
{
    private final BackgroundAnalysisManager analysisManager;
    private final IUI ui;

    @Inject
    public BackgroundAnalysisTrigger(BackgroundAnalysisManager analysisManager, IUI ui)
    {
        this.ui = ui;
        this.analysisManager = analysisManager;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        if (event.getType() != IResourceChangeEvent.POST_CHANGE)
        {
            return;
        }
        IResourceDelta delta = event.getDelta();
        if (delta == null)
        {
            return;
        }
        // Analyze only the file currently open in the active editor. Batch workspace changes
        // (git rebase/merge, "Save All", refactorings) touch many files at once; without this gate
        // each would spawn its own background review, flooding the assistant. If no editor is active,
        // there is nothing the user is looking at — skip.
        var activeFile = ui.getActiveFile();
        if (activeFile.isEmpty())
        {
            return;
        }
        try
            {
            delta.accept(d -> {
                IResource resource = d.getResource();
                if (!(resource instanceof IFile))
                {
                    return true;
                }
                if (d.getKind() != IResourceDelta.CHANGED)
                {
                    return true;
                }
                if ((d.getFlags() & IResourceDelta.CONTENT) == 0)
                {
                    return true;
                }
                if (isSameFile(activeFile.get(), (IFile)resource))
                {
                    analysisManager.onFileSaved((IFile)resource);
                }

                return true;
            });
        }
        catch (CoreException e)
        {
            // игнорируем ошибки обхода дерева изменений
        }
    }

    /**
     * Compares by on-disk location so the same physical file matches regardless of which
     * (possibly overlapping/nested) project handle the delta reports it under.
     */
    private static boolean isSameFile(IFile a, IFile b)
    {
        var locationA = a.getLocation();
        var locationB = b.getLocation();
        if (locationA != null && locationB != null)
        {
            return locationA.equals(locationB);
        }
        return a.getFullPath().equals(b.getFullPath());
    }
}
