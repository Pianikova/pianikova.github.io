/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ui.IResourceProvider;
import com.e1c.edt.ai.ui.IWorkmateLocations;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Default {@link ISkillRegistry}. Collects skill ids from the bundle and from every existing
 * {@code .workmate/skills} directory, then resolves and parses each effective {@code SKILL.md}.
 */
public class SkillRegistry
    implements ISkillRegistry
{
    private static final String NAME_KEY = "name"; //$NON-NLS-1$
    private static final String DESCRIPTION_KEY = "description"; //$NON-NLS-1$

    private final IResourceProvider resourceProvider;
    private final IWorkmateLocations locations;
    private final ISkillResourceResolver resolver;
    private final ISkillMdParser parser;
    private final ILog log;

    @Inject
    public SkillRegistry(IResourceProvider resourceProvider, IWorkmateLocations locations,
        ISkillResourceResolver resolver, ISkillMdParser parser, ILog log)
    {
        Preconditions.checkNotNull(resourceProvider);
        Preconditions.checkNotNull(locations);
        Preconditions.checkNotNull(resolver);
        Preconditions.checkNotNull(parser);
        Preconditions.checkNotNull(log);
        this.resourceProvider = resourceProvider;
        this.locations = locations;
        this.resolver = resolver;
        this.parser = parser;
        this.log = log;
    }

    @Override
    public List<SkillDescriptor> listSkills(Optional<IProject> project)
    {
        var projectRoot = project.flatMap(locations::projectRoot);

        var ids = new LinkedHashSet<String>();
        ids.addAll(resourceProvider.listChildNames(SkillRepository.SKILLS_DIR));
        collectOverrideIds(ids, projectRoot);
        collectOverrideIds(ids, locations.workspaceRoot());
        collectOverrideIds(ids, locations.userHome());

        var descriptors = new ArrayList<SkillDescriptor>();
        for (var id : ids)
        {
            try
            {
                var resolved = resolver.resolve(SkillRepository.skillMarkdownPath(id), projectRoot);
                if (resolved.isEmpty())
                {
                    continue;
                }
                var parsed = parser.parse(id, resolved.get().getContent());
                var metadata = parsed.getMetadata();
                var name = metadata.get(NAME_KEY);
                descriptors.add(new SkillDescriptor(id, name != null ? name : id, metadata.get(DESCRIPTION_KEY),
                    resolved.get().getSource(), new ArrayList<>(parsed.getToolIds())));
            }
            catch (Exception e)
            {
                log.logError(e);
            }
        }
        return descriptors;
    }

    private void collectOverrideIds(Set<String> ids, Optional<Path> root)
    {
        if (root.isEmpty())
        {
            return;
        }

        var skillsDir = root.get().resolve(IWorkmateLocations.WORKMATE_DIR).resolve(SkillRepository.SKILLS_DIR);
        if (!Files.isDirectory(skillsDir))
        {
            return;
        }

        try (Stream<Path> stream = Files.list(skillsDir))
        {
            stream.filter(Files::isDirectory).forEach(dir -> ids.add(dir.getFileName().toString()));
        }
        catch (IOException e)
        {
            log.logError(e);
        }
    }
}
