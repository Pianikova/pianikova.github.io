/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.navigator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.skills.ISkillRegistry;
import com.e1c.edt.ai.skills.SkillSource;
import com.e1c.edt.ai.ui.BaseActivator;
import com.google.inject.Inject;

/**
 * Contributes the scoped "Workmate" tree: a root node (User / Workspace scopes) next to the projects,
 * and a project scope node under each project. Each scope exposes its {@code WORKMATE.md} and a
 * "Skills" group whose skills expand into their tools.
 * <p>
 * Created by the Common Navigator Framework: injection is lazy and every callback is guarded so a
 * failure logs and degrades to "no children" instead of letting CNF silently drop the extension.
 */
public class WorkmateNavigatorContentProvider
    implements ICommonContentProvider
{
    private static final Object[] EMPTY = new Object[0];

    @Inject
    ISkillRegistry registry;
    @Inject
    ILog log;

    private boolean injectFailureLogged;

    @Override
    public Object[] getElements(Object inputElement)
    {
        return getChildren(inputElement);
    }

    @Override
    public Object[] getChildren(Object parentElement)
    {
        try
        {
            if (parentElement instanceof IWorkspaceRoot)
            {
                return new Object[] { new WorkmateRootNode() };
            }
            if (parentElement instanceof WorkmateRootNode)
            {
                return new Object[] { new ScopeNode(SkillSource.USER, null), new ScopeNode(SkillSource.WORKSPACE, null) };
            }
            if (parentElement instanceof IProject)
            {
                return ((IProject)parentElement).isOpen()
                    ? new Object[] { new ScopeNode(SkillSource.PROJECT, (IProject)parentElement) } : EMPTY;
            }
            if (parentElement instanceof ScopeNode)
            {
                var scope = (ScopeNode)parentElement;
                return new Object[] { new WorkmateFileNode(scope.getProject(), scope.getLevel()),
                    new SkillsGroupNode(scope.getLevel(), scope.getProject()) };
            }
            if (parentElement instanceof SkillsGroupNode)
            {
                var group = (SkillsGroupNode)parentElement;
                ensureInjected();
                if (registry == null)
                {
                    return EMPTY;
                }
                return registry.listSkills(Optional.ofNullable(group.getProject()))
                    .stream()
                    .map(descriptor -> new SkillNode(group.getLevel(), group.getProject(), descriptor))
                    .toArray();
            }
            if (parentElement instanceof SkillNode)
            {
                var node = (SkillNode)parentElement;
                var tools = new ArrayList<>();
                for (var toolId : node.getDescriptor().getToolIds())
                {
                    tools.add(new SkillToolNode(node.getLevel(), node.getProject(), node.getDescriptor(), toolId));
                }
                return tools.toArray();
            }
        }
        catch (Exception e)
        {
            logError(e);
        }
        return EMPTY;
    }

    @Override
    public Object getParent(Object element)
    {
        if (element instanceof WorkmateRootNode)
        {
            return ResourcesPlugin.getWorkspace().getRoot();
        }
        if (element instanceof ScopeNode)
        {
            var scope = (ScopeNode)element;
            return scope.getLevel() == SkillSource.PROJECT ? scope.getProject() : new WorkmateRootNode();
        }
        if (element instanceof WorkmateFileNode)
        {
            var node = (WorkmateFileNode)element;
            return new ScopeNode(node.getLevel(), node.getProject());
        }
        if (element instanceof SkillsGroupNode)
        {
            var group = (SkillsGroupNode)element;
            return new ScopeNode(group.getLevel(), group.getProject());
        }
        if (element instanceof SkillNode)
        {
            var node = (SkillNode)element;
            return new SkillsGroupNode(node.getLevel(), node.getProject());
        }
        if (element instanceof SkillToolNode)
        {
            var node = (SkillToolNode)element;
            return new SkillNode(node.getLevel(), node.getProject(), node.getDescriptor());
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element)
    {
        if (element instanceof SkillNode)
        {
            return !((SkillNode)element).getDescriptor().getToolIds().isEmpty();
        }
        if (element instanceof WorkmateFileNode || element instanceof SkillToolNode)
        {
            return false;
        }
        return element instanceof IWorkspaceRoot || element instanceof IProject || element instanceof WorkmateRootNode
            || element instanceof ScopeNode || element instanceof SkillsGroupNode;
    }

    @Override
    public void dispose()
    {
        // nothing to dispose
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
        // stateless
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
        if (registry != null)
        {
            return;
        }
        // Retry until the injector is ready: CNF may instantiate this provider (and call init())
        // before the plugin's Guice injector is available. Latching the failure would leave the
        // tree permanently empty; instead we re-attempt and log the error only once.
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
