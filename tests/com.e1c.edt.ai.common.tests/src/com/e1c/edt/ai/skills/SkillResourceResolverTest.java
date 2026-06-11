/**
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ui.IResourceProvider;
import com.e1c.edt.ai.ui.IWorkmateLocations;

/**
 * Tests for {@link SkillResourceResolver}: per-file override resolution across the
 * project / workspace / user / bundle levels.
 */
@SuppressWarnings("nls")
public class SkillResourceResolverTest
{
    private static final String SKILL_PATH = "skills/demo/SKILL.md";
    private static final String TOOL_PATH = "skills/demo/tools/git_diff.json";

    private Path userDir;
    private Path workspaceDir;
    private Path projectDir;

    private IResourceProvider resourceProvider;
    private TestLocations locations;
    private SkillResourceResolver resolver;

    @Before
    public void setUp() throws IOException
    {
        userDir = Files.createTempDirectory("skill-user");
        workspaceDir = Files.createTempDirectory("skill-ws");
        projectDir = Files.createTempDirectory("skill-proj");

        resourceProvider = mock(IResourceProvider.class);
        locations = new TestLocations(userDir, workspaceDir);
        resolver = new SkillResourceResolver(locations, resourceProvider, mock(ILog.class));
    }

    @After
    public void tearDown() throws IOException
    {
        deleteRecursively(userDir);
        deleteRecursively(workspaceDir);
        deleteRecursively(projectDir);
    }

    @Test
    public void projectLevelWinsOverAllOthers() throws IOException
    {
        writeOverride(userDir, SKILL_PATH, "user");
        writeOverride(workspaceDir, SKILL_PATH, "workspace");
        writeOverride(projectDir, SKILL_PATH, "project");
        when(resourceProvider.getTextResource(SKILL_PATH)).thenReturn(Optional.of("bundle"));

        var resolved = resolver.resolve(SKILL_PATH, Optional.of(projectDir)).orElseThrow();

        assertEquals("project", resolved.getContent());
        assertEquals(SkillSource.PROJECT, resolved.getSource());
    }

    @Test
    public void workspaceLevelWinsOverUserAndBundle() throws IOException
    {
        writeOverride(userDir, SKILL_PATH, "user");
        writeOverride(workspaceDir, SKILL_PATH, "workspace");
        when(resourceProvider.getTextResource(SKILL_PATH)).thenReturn(Optional.of("bundle"));

        var resolved = resolver.resolve(SKILL_PATH, Optional.of(projectDir)).orElseThrow();

        assertEquals("workspace", resolved.getContent());
        assertEquals(SkillSource.WORKSPACE, resolved.getSource());
    }

    @Test
    public void fallsBackToBundleWhenNoOverrideExists()
    {
        when(resourceProvider.getTextResource(SKILL_PATH)).thenReturn(Optional.of("bundle"));

        var resolved = resolver.resolve(SKILL_PATH, Optional.of(projectDir)).orElseThrow();

        assertEquals("bundle", resolved.getContent());
        assertEquals(SkillSource.BUNDLE, resolved.getSource());
    }

    @Test
    public void overrideIsPerFile() throws IOException
    {
        // Only SKILL.md is overridden (at user level); the tool schema stays the bundled default.
        writeOverride(userDir, SKILL_PATH, "user-md");
        when(resourceProvider.getTextResource(TOOL_PATH)).thenReturn(Optional.of("bundle-json"));

        var md = resolver.resolve(SKILL_PATH, Optional.empty()).orElseThrow();
        var tool = resolver.resolve(TOOL_PATH, Optional.empty()).orElseThrow();

        assertEquals(SkillSource.USER, md.getSource());
        assertEquals("user-md", md.getContent());
        assertEquals(SkillSource.BUNDLE, tool.getSource());
        assertEquals("bundle-json", tool.getContent());
    }

    @Test
    public void returnsEmptyWhenNowhereFound()
    {
        when(resourceProvider.getTextResource(SKILL_PATH)).thenReturn(Optional.empty());

        assertFalse(resolver.resolve(SKILL_PATH, Optional.of(projectDir)).isPresent());
    }

    @Test
    public void projectLevelIgnoredWhenProjectRootAbsent() throws IOException
    {
        writeOverride(projectDir, SKILL_PATH, "project");
        writeOverride(userDir, SKILL_PATH, "user");
        when(resourceProvider.getTextResource(SKILL_PATH)).thenReturn(Optional.of("bundle"));

        // No project root passed -> project-level override must be skipped.
        var resolved = resolver.resolve(SKILL_PATH, Optional.empty()).orElseThrow();

        assertEquals(SkillSource.USER, resolved.getSource());
        assertTrue(resolved.getPath().isPresent());
    }

    private static void writeOverride(Path root, String relativePath, String content) throws IOException
    {
        var file = root.resolve(IWorkmateLocations.WORKMATE_DIR);
        for (var segment : relativePath.split("/"))
        {
            file = file.resolve(segment);
        }
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

    /**
     * In-memory {@link IWorkmateLocations} backed by fixed directories.
     */
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
