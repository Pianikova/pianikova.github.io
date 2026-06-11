/**
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.RegexTemplateProcessor;
import com.e1c.edt.ai.ui.IResourceProvider;
import com.e1c.edt.ai.ui.IWorkmateLocations;

/**
 * Tests for {@link SkillRegistry}: union of bundle defaults and {@code .workmate} overrides,
 * de-duplication, and correct source level per skill.
 */
@SuppressWarnings("nls")
public class SkillRegistryTest
{
    private Path userDir;
    private Path workspaceDir;

    private IResourceProvider resourceProvider;
    private TestLocations locations;
    private SkillRegistry registry;

    @Before
    public void setUp() throws IOException
    {
        userDir = Files.createTempDirectory("reg-user");
        workspaceDir = Files.createTempDirectory("reg-ws");

        resourceProvider = mock(IResourceProvider.class);
        locations = new TestLocations(userDir, workspaceDir);

        var templateProcessor = new SkillTemplateProcessor(new RegexTemplateProcessor(), mock(IJson.class));
        var resolver = new SkillResourceResolver(locations, resourceProvider, mock(ILog.class));
        registry = new SkillRegistry(resourceProvider, locations, resolver, new SkillMdParser(templateProcessor),
            mock(ILog.class));
    }

    @After
    public void tearDown() throws IOException
    {
        deleteRecursively(userDir);
        deleteRecursively(workspaceDir);
    }

    @Test
    public void unionsBundleAndOverridesWithoutDuplicates() throws IOException
    {
        // Bundle ships "git-commit"; the user adds "custom"; the workspace overrides "git-commit".
        when(resourceProvider.listChildNames("skills")).thenReturn(Set.of("git-commit"));
        when(resourceProvider.getTextResource(SkillRepository.skillMarkdownPath("git-commit")))
            .thenReturn(Optional.of(skillMd("git-commit", "bundle git-commit")));

        writeSkill(workspaceDir, "git-commit", skillMd("git-commit", "workspace git-commit"));
        writeSkill(userDir, "custom", skillMd("custom", "my custom skill"));

        var skills = registry.listSkills(Optional.empty());

        var byId = new LinkedHashMap<String, SkillDescriptor>();
        skills.forEach(descriptor -> byId.put(descriptor.getSkillId(), descriptor));

        assertEquals(2, byId.size());
        assertTrue(byId.containsKey("git-commit"));
        assertTrue(byId.containsKey("custom"));
        // workspace override takes precedence over the bundled default
        assertEquals(SkillSource.WORKSPACE, byId.get("git-commit").getSource());
        assertEquals(SkillSource.USER, byId.get("custom").getSource());
        assertEquals("workspace git-commit", byId.get("git-commit").getDescription().orElse(null));
    }

    @Test
    public void reportsBundleSourceForNonOverriddenSkill()
    {
        when(resourceProvider.listChildNames("skills")).thenReturn(Set.of("git-review"));
        when(resourceProvider.getTextResource(SkillRepository.skillMarkdownPath("git-review")))
            .thenReturn(Optional.of(skillMd("git-review", "review code")));

        var skills = registry.listSkills(Optional.empty());

        assertEquals(1, skills.size());
        assertEquals("git-review", skills.get(0).getSkillId());
        assertEquals(SkillSource.BUNDLE, skills.get(0).getSource());
    }

    private static String skillMd(String name, String description)
    {
        return "---\nname: " + name + "\ndescription: " + description + "\n---\nbody";
    }

    private static void writeSkill(Path root, String skillId, String content) throws IOException
    {
        var file = root.resolve(IWorkmateLocations.WORKMATE_DIR)
            .resolve("skills")
            .resolve(skillId)
            .resolve("SKILL.md");
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static void deleteRecursively(Path root) throws IOException
    {
        if (!Files.exists(root))
        {
            return;
        }
        try (var walk = Files.walk(root))
        {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                try
                {
                    Files.deleteIfExists(path);
                }
                catch (IOException e)
                {
                    // ignore in tests
                }
            });
        }
    }

    private static final class TestLocations
        implements IWorkmateLocations
    {
        private final Path userHome;
        private final Path workspace;

        TestLocations(Path userHome, Path workspace)
        {
            this.userHome = userHome;
            this.workspace = workspace;
        }

        @Override
        public Optional<Path> userHome()
        {
            return Optional.of(userHome);
        }

        @Override
        public Optional<Path> workspaceRoot()
        {
            return Optional.of(workspace);
        }

        @Override
        public Optional<Path> projectRoot(org.eclipse.core.resources.IProject project)
        {
            return Optional.empty();
        }
    }
}
