/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.navigator;

import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.skills.ISkillResourceResolver;
import com.e1c.edt.ai.skills.SkillRepository;
import com.e1c.edt.ai.skills.SkillSource;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IWorkmateLocations;
import com.e1c.edt.ai.ui.handlers.ISkillFileOpener;
import com.google.inject.Inject;

/**
 * Opens the file behind a scoped node (double-click / context menu) and offers "Reset" to delete an
 * existing override. A not-yet-created {@code WORKMATE.md} is not openable here (it is created via chat).
 */
public class WorkmateNavigatorActionProvider
    extends CommonActionProvider
{
    private static final String WORKMATE_MD = "WORKMATE.md"; //$NON-NLS-1$

    @Inject
    ISkillFileOpener opener;
    @Inject
    ISkillResourceResolver resolver;
    @Inject
    IWorkmateLocations locations;
    @Inject
    ILog log;

    private boolean injectFailureLogged;

    @Override
    public void init(ICommonActionExtensionSite site)
    {
        super.init(site);
        ensureInjected();
    }

    @Override
    public void fillActionBars(IActionBars actionBars)
    {
        var element = selectedElement();
        if (canOpen(element))
        {
            actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, new Action()
            {
                @Override
                public void run()
                {
                    open(element);
                }
            });
        }
    }

    @Override
    public void fillContextMenu(IMenuManager menu)
    {
        var element = selectedElement();
        if (canOpen(element))
        {
            menu.add(new Action(Messages.action_openCreate)
            {
                @Override
                public void run()
                {
                    open(element);
                }
            });
        }
        if (overrideExists(element))
        {
            menu.add(new Action(Messages.action_reset)
            {
                @Override
                public void run()
                {
                    reset(element);
                }
            });
        }
    }

    /**
     * Workmate, skill and tool nodes are all openable: the override file is created on demand from the
     * effective default.
     */
    private boolean canOpen(Object element)
    {
        return element instanceof WorkmateFileNode || element instanceof SkillNode
            || element instanceof SkillToolNode;
    }

    private void open(Object element)
    {
        ensureInjected();
        if (opener == null)
        {
            return;
        }
        if (element instanceof WorkmateFileNode)
        {
            var node = (WorkmateFileNode)element;
            opener.openWorkmate(node.getLevel(), Optional.ofNullable(node.getProject()));
        }
        else if (element instanceof SkillNode)
        {
            var node = (SkillNode)element;
            opener.openSkill(node.getDescriptor().getSkillId(), node.getLevel(), Optional.ofNullable(node.getProject()));
        }
        else if (element instanceof SkillToolNode)
        {
            var node = (SkillToolNode)element;
            opener.openSkillTool(node.getSkillId(), node.getToolId(), node.getLevel(),
                Optional.ofNullable(node.getProject()));
        }
        else
        {
            return;
        }
        // Opening a skill/tool creates the override file on demand: refresh so the "overridden" badge
        // and the "Reset" action appear immediately.
        refresh(element);
    }

    private void reset(Object element)
    {
        ensureInjected();
        if (opener == null)
        {
            return;
        }
        if (element instanceof WorkmateFileNode)
        {
            var node = (WorkmateFileNode)element;
            opener.resetWorkmate(node.getLevel(), Optional.ofNullable(node.getProject()));
        }
        else if (element instanceof SkillNode)
        {
            var node = (SkillNode)element;
            opener.resetSkill(node.getDescriptor().getSkillId(), node.getLevel(), Optional.ofNullable(node.getProject()));
        }
        else if (element instanceof SkillToolNode)
        {
            var node = (SkillToolNode)element;
            opener.resetSkillTool(node.getSkillId(), node.getToolId(), node.getLevel(),
                Optional.ofNullable(node.getProject()));
        }
        else
        {
            return;
        }
        refresh(element);
    }

    private boolean overrideExists(Object element)
    {
        ensureInjected();
        if (resolver == null)
        {
            return false;
        }
        if (element instanceof WorkmateFileNode)
        {
            var node = (WorkmateFileNode)element;
            return resolver.existsAt(node.getLevel(), WORKMATE_MD, projectRoot(node.getProject()));
        }
        if (element instanceof SkillNode)
        {
            var node = (SkillNode)element;
            return resolver.existsAt(node.getLevel(), SkillRepository.skillMarkdownPath(node.getDescriptor().getSkillId()),
                projectRoot(node.getProject()));
        }
        if (element instanceof SkillToolNode)
        {
            var node = (SkillToolNode)element;
            return resolver.existsAt(node.getLevel(), SkillRepository.toolSchemaPath(node.getSkillId(), node.getToolId()),
                projectRoot(node.getProject()));
        }
        return false;
    }

    private Optional<Path> projectRoot(IProject project)
    {
        if (project == null || locations == null)
        {
            return Optional.empty();
        }
        return locations.projectRoot(project);
    }

    private void refresh(Object element)
    {
        var viewer = getActionSite().getStructuredViewer();
        if (viewer != null)
        {
            viewer.refresh(element);
        }
    }

    private Object selectedElement()
    {
        var selection = getContext().getSelection();
        if (selection instanceof IStructuredSelection)
        {
            return ((IStructuredSelection)selection).getFirstElement();
        }
        return null;
    }

    private void ensureInjected()
    {
        if (opener != null)
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
                if (log != null)
                {
                    log.logError(e);
                }
            }
        }
    }
}
