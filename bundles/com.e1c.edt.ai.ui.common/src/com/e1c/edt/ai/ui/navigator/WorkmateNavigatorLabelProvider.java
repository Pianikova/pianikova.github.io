/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.navigator;

import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.skills.ISkillResourceResolver;
import com.e1c.edt.ai.skills.SkillRepository;
import com.e1c.edt.ai.skills.SkillSource;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IWorkmateLocations;
import com.e1c.edt.ai.ui.Images;
import com.google.inject.Inject;

/**
 * Labels for the scoped Workmate navigator nodes: localized names, the shared {@code ai.png} icon, and
 * an "overridden" badge when the file physically exists at the node's scope level.
 */
public class WorkmateNavigatorLabelProvider
    extends LabelProvider
    implements ICommonLabelProvider
{
    private static final String WORKMATE_MD = "WORKMATE.md"; //$NON-NLS-1$

    @Inject
    ISkillResourceResolver resolver;
    @Inject
    IWorkmateLocations locations;
    @Inject
    ILog log;

    private boolean injectFailureLogged;

    @Override
    public String getText(Object element)
    {
        try
        {
            if (element instanceof WorkmateRootNode)
            {
                return Messages.node_workmate;
            }
            if (element instanceof ScopeNode)
            {
                return scopeLabel(((ScopeNode)element).getLevel());
            }
            if (element instanceof SkillsGroupNode)
            {
                return Messages.node_skills;
            }
            if (element instanceof WorkmateFileNode)
            {
                var node = (WorkmateFileNode)element;
                return withBadge(WORKMATE_MD, node.getLevel(), node.getProject(), WORKMATE_MD);
            }
            if (element instanceof SkillNode)
            {
                var node = (SkillNode)element;
                return withBadge(node.getDescriptor().getName(), node.getLevel(), node.getProject(),
                    SkillRepository.skillMarkdownPath(node.getDescriptor().getSkillId()));
            }
            if (element instanceof SkillToolNode)
            {
                var node = (SkillToolNode)element;
                return withBadge(node.getToolId(), node.getLevel(), node.getProject(),
                    SkillRepository.toolSchemaPath(node.getSkillId(), node.getToolId()));
            }
        }
        catch (Exception e)
        {
            logError(e);
        }
        return null;
    }

    @Override
    public Image getImage(Object element)
    {
        // The navigator may render before the plugin's activator has started (static image registry
        // not yet available): guard so a missing icon never breaks label rendering.
        if (BaseActivator.getDefault() == null)
        {
            return null;
        }
        try
        {
            if (element instanceof WorkmateRootNode)
            {
                return BaseActivator.getImage(Images.AI);
            }
            // The project-level scope node is also titled "Workmate"/"Напарник", so it shares the root icon.
            if (element instanceof ScopeNode && ((ScopeNode)element).getLevel() == SkillSource.PROJECT)
            {
                return BaseActivator.getImage(Images.AI);
            }
            var shared = PlatformUI.getWorkbench().getSharedImages();
            if (element instanceof ScopeNode || element instanceof SkillsGroupNode)
            {
                return shared.getImage(ISharedImages.IMG_OBJ_FOLDER);
            }
            if (element instanceof SkillNode)
            {
                return shared.getImage(ISharedImages.IMG_OBJ_ELEMENT);
            }
            if (element instanceof WorkmateFileNode || element instanceof SkillToolNode)
            {
                return shared.getImage(ISharedImages.IMG_OBJ_FILE);
            }
        }
        catch (Exception e)
        {
            return null;
        }
        return null;
    }

    @Override
    public String getDescription(Object element)
    {
        if (element instanceof SkillNode)
        {
            return ((SkillNode)element).getDescriptor().getDescription().orElse(null);
        }
        return null;
    }

    private String scopeLabel(SkillSource level)
    {
        switch (level)
        {
        case USER:
            return Messages.node_user;
        case WORKSPACE:
            return Messages.node_workspace;
        default:
            return Messages.node_workmate;
        }
    }

    private String withBadge(String name, SkillSource level, IProject project, String relativePath)
    {
        ensureInjected();
        if (resolver != null && resolver.existsAt(level, relativePath, projectRoot(project)))
        {
            return name + " [" + Messages.badge_overridden + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return name;
    }

    private Optional<Path> projectRoot(IProject project)
    {
        if (project == null || locations == null)
        {
            return Optional.empty();
        }
        return locations.projectRoot(project);
    }

    @Override
    public void init(ICommonContentExtensionSite config)
    {
        ensureInjected();
    }

    @Override
    public void restoreState(IMemento memento)
    {
        // no state
    }

    @Override
    public void saveState(IMemento memento)
    {
        // no state
    }

    private void ensureInjected()
    {
        if (resolver != null)
        {
            return;
        }
        try
        {
            BaseActivator.injectMembers(this);
        }
        catch (Exception e)
        {
            if (!injectFailureLogged)
            {
                injectFailureLogged = true;
                logError(e);
            }
        }
    }

    private void logError(Throwable e)
    {
        if (log != null)
        {
            log.logError(e);
        }
    }
}
