/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ui.IResourceProvider;
import com.e1c.edt.ai.ui.IWorkmateLocations;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Default {@link ISkillResourceResolver}: checks {@code <level>/.workmate/<relativePath>} for the
 * project, workspace and user levels (in that order) and falls back to the bundled default via
 * {@link IResourceProvider}.
 */
public class SkillResourceResolver
    implements ISkillResourceResolver
{
    private final IWorkmateLocations locations;
    private final IResourceProvider resourceProvider;
    private final ILog log;

    @Inject
    public SkillResourceResolver(IWorkmateLocations locations, IResourceProvider resourceProvider, ILog log)
    {
        Preconditions.checkNotNull(locations);
        Preconditions.checkNotNull(resourceProvider);
        Preconditions.checkNotNull(log);
        this.locations = locations;
        this.resourceProvider = resourceProvider;
        this.log = log;
    }

    @Override
    public Optional<ResolvedSkillResource> resolve(String relativePath, Optional<Path> projectRoot)
    {
        if (projectRoot.isPresent())
        {
            var resolved = readFromLevel(projectRoot, relativePath, SkillSource.PROJECT);
            if (resolved.isPresent())
            {
                return resolved;
            }
        }

        var workspace = readFromLevel(locations.workspaceRoot(), relativePath, SkillSource.WORKSPACE);
        if (workspace.isPresent())
        {
            return workspace;
        }

        var user = readFromLevel(locations.userHome(), relativePath, SkillSource.USER);
        if (user.isPresent())
        {
            return user;
        }

        return resourceProvider.getTextResource(relativePath)
            .map(content -> new ResolvedSkillResource(content, SkillSource.BUNDLE, null));
    }

    @Override
    public boolean existsAt(SkillSource level, String relativePath, Optional<Path> projectRoot)
    {
        var base = baseForLevel(level, projectRoot);
        if (base.isEmpty())
        {
            return false;
        }
        return Files.isRegularFile(resolveLevelFile(base.get(), relativePath));
    }

    private Optional<Path> baseForLevel(SkillSource level, Optional<Path> projectRoot)
    {
        switch (level)
        {
        case PROJECT:
            return projectRoot;
        case WORKSPACE:
            return locations.workspaceRoot();
        case USER:
            return locations.userHome();
        default:
            return Optional.empty();
        }
    }

    private static Path resolveLevelFile(Path root, String relativePath)
    {
        var file = root.resolve(IWorkmateLocations.WORKMATE_DIR);
        for (var segment : relativePath.split("/")) //$NON-NLS-1$
        {
            if (!segment.isEmpty())
            {
                file = file.resolve(segment);
            }
        }
        return file;
    }

    private Optional<ResolvedSkillResource> readFromLevel(Optional<Path> root, String relativePath, SkillSource source)
    {
        if (root.isEmpty())
        {
            return Optional.empty();
        }

        var file = resolveLevelFile(root.get(), relativePath);

        if (!Files.isRegularFile(file))
        {
            return Optional.empty();
        }

        try
        {
            return Optional.of(new ResolvedSkillResource(Files.readString(file, StandardCharsets.UTF_8), source, file));
        }
        catch (IOException e)
        {
            log.logError(e);
            return Optional.empty();
        }
    }
}
