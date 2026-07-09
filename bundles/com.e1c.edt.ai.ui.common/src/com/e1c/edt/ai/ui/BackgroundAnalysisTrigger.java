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
                ui.getLastSourceViewer().ifPresent(viewer -> analysisManager.onFileSaved((IFile)resource));

                return true;
            });
        }
        catch (CoreException e)
        {
            // игнорируем ошибки обхода дерева изменений
        }
    }
}
