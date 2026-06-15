/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.skills.ISkillResourceResolver;
import com.e1c.edt.ai.skills.ResolvedSkillResource;
import com.e1c.edt.ai.skills.SkillRepository;
import com.e1c.edt.ai.skills.SkillSource;
import com.e1c.edt.ai.ui.IWorkmateLocations;
import com.e1c.edt.ai.ui.navigator.Messages;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Default {@link ISkillFileOpener}.
 */
public class SkillFileOpener
    implements ISkillFileOpener
{
    private static final String SKILLS_SEGMENT = "skills"; //$NON-NLS-1$
    private static final String SKILL_MD_SEGMENT = "SKILL.md"; //$NON-NLS-1$
    private static final String WORKMATE_MD_SEGMENT = "WORKMATE.md"; //$NON-NLS-1$
    private static final String TOOLS_SEGMENT = "tools"; //$NON-NLS-1$
    private static final String DEFAULT_TEXT_EDITOR_ID = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$

    private final IWorkmateLocations locations;
    private final ISkillResourceResolver resolver;
    private final ILog log;

    @Inject
    public SkillFileOpener(IWorkmateLocations locations, ISkillResourceResolver resolver, ILog log)
    {
        Preconditions.checkNotNull(locations);
        Preconditions.checkNotNull(resolver);
        Preconditions.checkNotNull(log);
        this.locations = locations;
        this.resolver = resolver;
        this.log = log;
    }

    @Override
    public void openSkill(String skillId, SkillSource level, Optional<IProject> project)
    {
        openOrCreate(level, project, List.of(IWorkmateLocations.WORKMATE_DIR, SKILLS_SEGMENT, skillId, SKILL_MD_SEGMENT),
            () -> resolver.resolve(SkillRepository.skillMarkdownPath(skillId), project.flatMap(locations::projectRoot))
                .map(ResolvedSkillResource::getContent)
                .orElse(""), //$NON-NLS-1$
            true, skillId + " · " + SKILL_MD_SEGMENT + scopeSuffix(level)); //$NON-NLS-1$
    }

    @Override
    public void openSkillTool(String skillId, String toolId, SkillSource level, Optional<IProject> project)
    {
        openOrCreate(level, project,
            List.of(IWorkmateLocations.WORKMATE_DIR, SKILLS_SEGMENT, skillId, TOOLS_SEGMENT, toolId + ".json"), //$NON-NLS-1$
            () -> resolver.resolve(SkillRepository.toolSchemaPath(skillId, toolId), project.flatMap(locations::projectRoot))
                .map(ResolvedSkillResource::getContent)
                .orElse(""), //$NON-NLS-1$
            true, skillId + " · " + toolId + ".json" + scopeSuffix(level)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void openWorkmate(SkillSource level, Optional<IProject> project)
    {
        // Same behaviour as skills: create the override from the effective default (bundled WORKMATE.md
        // or an inherited level) and open it.
        openOrCreate(level, project, List.of(IWorkmateLocations.WORKMATE_DIR, WORKMATE_MD_SEGMENT),
            () -> resolver.resolve(WORKMATE_MD_SEGMENT, project.flatMap(locations::projectRoot))
                .map(ResolvedSkillResource::getContent)
                .orElse(""), //$NON-NLS-1$
            true, WORKMATE_MD_SEGMENT + scopeSuffix(level));
    }

    @Override
    public void resetSkill(String skillId, SkillSource level, Optional<IProject> project)
    {
        delete(level, project, List.of(IWorkmateLocations.WORKMATE_DIR, SKILLS_SEGMENT, skillId, SKILL_MD_SEGMENT));
    }

    @Override
    public void resetSkillTool(String skillId, String toolId, SkillSource level, Optional<IProject> project)
    {
        delete(level, project,
            List.of(IWorkmateLocations.WORKMATE_DIR, SKILLS_SEGMENT, skillId, TOOLS_SEGMENT, toolId + ".json")); //$NON-NLS-1$
    }

    @Override
    public void resetWorkmate(SkillSource level, Optional<IProject> project)
    {
        delete(level, project, List.of(IWorkmateLocations.WORKMATE_DIR, WORKMATE_MD_SEGMENT));
    }

    private void openOrCreate(SkillSource level, Optional<IProject> project, List<String> relativeSegments,
        Supplier<String> seed, boolean createIfAbsent, String title)
    {
        var base = baseFor(level, project);
        if (base.isEmpty())
        {
            return;
        }

        var file = IWorkmateLocations.resolve(base.get(), relativeSegments);

        try
        {
            if (!Files.exists(file))
            {
                if (!createIfAbsent)
                {
                    return;
                }
                Files.createDirectories(file.getParent());
                Files.writeString(file, seed.get(), StandardCharsets.UTF_8);
            }

            var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            var fileStore = EFS.getLocalFileSystem().getStore(file.toUri());
            var input = new WorkmateEditorInput(fileStore, title, file.toString());
            page.openEditor(input, editorIdFor(file.getFileName().toString()));
        }
        catch (Exception e)
        {
            log.logError(e);
        }
    }

    private void delete(SkillSource level, Optional<IProject> project, List<String> relativeSegments)
    {
        var base = baseFor(level, project);
        if (base.isEmpty())
        {
            return;
        }
        var file = IWorkmateLocations.resolve(base.get(), relativeSegments);
        try
        {
            closeEditor(file);
            Files.deleteIfExists(file);
        }
        catch (Exception e)
        {
            log.logError(e);
        }
    }

    private void closeEditor(Path file)
    {
        var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null || window.getActivePage() == null)
        {
            return;
        }
        var page = window.getActivePage();
        var uri = file.toUri();
        for (IEditorReference reference : page.getEditorReferences())
        {
            try
            {
                var editorInput = reference.getEditorInput();
                if (editorInput instanceof FileStoreEditorInput
                    && uri.equals(((FileStoreEditorInput)editorInput).getURI()))
                {
                    page.closeEditors(new IEditorReference[] { reference }, false);
                }
            }
            catch (PartInitException e)
            {
                // ignore: editor input could not be resolved
            }
        }
    }

    private static String editorIdFor(String fileName)
    {
        try
        {
            var descriptor = IDE.getEditorDescriptor(fileName, true, false);
            return descriptor != null && descriptor.isInternal() ? descriptor.getId() : DEFAULT_TEXT_EDITOR_ID;
        }
        catch (PartInitException e)
        {
            return DEFAULT_TEXT_EDITOR_ID;
        }
    }

    private static String scopeSuffix(SkillSource level)
    {
        return " [" + scopeName(level) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static String scopeName(SkillSource level)
    {
        switch (level)
        {
        case WORKSPACE:
            return Messages.node_workspace;
        case USER:
            return Messages.node_user;
        default:
            return Messages.node_project;
        }
    }

    private Optional<Path> baseFor(SkillSource level, Optional<IProject> project)
    {
        switch (level)
        {
        case PROJECT:
            return project.flatMap(locations::projectRoot);
        case WORKSPACE:
            return locations.workspaceRoot();
        case USER:
            return locations.userHome();
        default:
            return Optional.empty();
        }
    }
}
