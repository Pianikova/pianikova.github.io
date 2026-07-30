/**
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.RegexTemplateProcessor;
import com.e1c.edt.ai.assistent.SkillExecutionException;
import com.e1c.edt.ai.assistent.model.SkillMetadata;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillMdParserTest
{
    private final RegexTemplateProcessor regexTemplateProcessor = new RegexTemplateProcessor();
    private final IJson json = mock(IJson.class);
    private final SkillTemplateProcessor templateProcessor = new SkillTemplateProcessor(regexTemplateProcessor, json);

    @SuppressWarnings("nls")
    @Test
    public void shouldParseSkillWithFrontmatter()
    {
        // Given
        String skillContent = "---\nname: test-skill\ndescription: Test description\n---\nSkill body content";
        var parser = new SkillMdParser(templateProcessor);

        // When
        var result = parser.parse("test-skill", skillContent);

        // Then
        assertNotNull(result);
        assertEquals("test-skill", result.getSkillId());
        assertEquals("Skill body content", result.getTemplate());

        SkillMetadata metadata = result.getMetadata();
        assertNotNull(metadata);
        Map<String, String> metadataMap = metadata.getValues();
        assertEquals("test-skill", metadataMap.get("name"));
        assertEquals("Test description", metadataMap.get("description"));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldParseAllowedTools()
    {
        String skillContent =
            "---\nname: test-skill\nallowed-tools: [JGit, GetProjects, Read]\n---\nSkill body content";
        var parser = new SkillMdParser(templateProcessor);

        var allowedTools = parser.parse("test-skill", skillContent).getMetadata().getAllowedTools();

        assertTrue(allowedTools.isPresent());
        assertEquals(Set.of("JGit", "GetProjects", "Read"), allowedTools.get());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldTreatWildcardAllowedToolsAsUnrestricted()
    {
        String skillContent = "---\nname: test-skill\nallowed-tools: *\n---\nSkill body content";
        var parser = new SkillMdParser(templateProcessor);

        assertFalse(parser.parse("test-skill", skillContent).getMetadata().getAllowedTools().isPresent());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldParseQuotedCommaSeparatedAllowedTools()
    {
        String skillContent =
            "---\nname: test-skill\nallowed-tools: \"JGit, Read\"\n---\nSkill body content";
        var parser = new SkillMdParser(templateProcessor);

        var allowedTools = parser.parse("test-skill", skillContent).getMetadata().getAllowedTools();

        assertTrue(allowedTools.isPresent());
        assertEquals(Set.of("JGit", "Read"), allowedTools.get());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldParseEmptyAllowedToolsArray()
    {
        String skillContent = "---\nname: test-skill\nallowed-tools: []\n---\nSkill body content";
        var parser = new SkillMdParser(templateProcessor);

        var allowedTools = parser.parse("test-skill", skillContent).getMetadata().getAllowedTools();

        assertTrue(allowedTools.isPresent());
        assertTrue(allowedTools.get().isEmpty());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldParseSkillWithoutFrontmatter()
    {
        // Given
        String skillContent = "Skill body content without frontmatter";
        var parser = new SkillMdParser(templateProcessor);

        // When
        var result = parser.parse("test-skill", skillContent);

        // Then
        assertNotNull(result);
        assertEquals("test-skill", result.getSkillId());
        assertEquals("Skill body content without frontmatter", result.getTemplate());

        SkillMetadata metadata = result.getMetadata();
        assertNotNull(metadata);
        Map<String, String> metadataMap = metadata.getValues();
        assertTrue(metadataMap.isEmpty());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldParseSkillWithToolDirectives()
    {
        // Given
        String skillContent = "---\nname: test-skill\n---\nSome text !tool('git_diff') more text";
        var parser = new SkillMdParser(templateProcessor);

        // When
        var result = parser.parse("test-skill", skillContent);

        // Then
        assertNotNull(result);
        Set<String> toolIds = result.getToolIds();
        assertEquals(1, toolIds.size());
        assertTrue(toolIds.contains("git_diff"));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldParseSkillWithMultipleToolDirectives()
    {
        // Given
        String skillContent = "---\nname: test-skill\n---\nText !tool('git_diff') and !tool('git_recent_commits')";
        var parser = new SkillMdParser(templateProcessor);

        // When
        var result = parser.parse("test-skill", skillContent);

        // Then
        assertNotNull(result);
        Set<String> toolIds = result.getToolIds();
        assertEquals(2, toolIds.size());
        assertTrue(toolIds.contains("git_diff"));
        assertTrue(toolIds.contains("git_recent_commits"));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldThrowExceptionWhenContentIsNull()
    {
        // Given
        var parser = new SkillMdParser(templateProcessor);

        // When
        try
        {
            parser.parse("test-skill", null);
            fail("Should throw exception for null content");
        }
        catch (SkillExecutionException e)
        {
            // Then
            assertEquals(com.e1c.edt.ai.assistent.SkillErrorCode.SKILL_PARSE_ERROR, e.getCode());
        }
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldThrowExceptionWhenContentIsEmpty()
    {
        // Given
        var parser = new SkillMdParser(templateProcessor);

        // When
        try
        {
            parser.parse("test-skill", "");
            fail("Should throw exception for empty content");
        }
        catch (SkillExecutionException e)
        {
            // Then
            assertEquals(com.e1c.edt.ai.assistent.SkillErrorCode.SKILL_PARSE_ERROR, e.getCode());
        }
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldThrowExceptionWhenFrontmatterNotClosed()
    {
        // Given
        String skillContent = "---\nname: test-skill\nno closing delimiter";
        var parser = new SkillMdParser(templateProcessor);

        // When
        try
        {
            parser.parse("test-skill", skillContent);
            fail("Should throw exception for unclosed frontmatter");
        }
        catch (SkillExecutionException e)
        {
            // Then
            assertEquals(com.e1c.edt.ai.assistent.SkillErrorCode.SKILL_PARSE_ERROR, e.getCode());
        }
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldThrowExceptionWhenFrontmatterLineInvalid()
    {
        // Given
        String skillContent = "---\ninvalid line without colon\n---\nBody";
        var parser = new SkillMdParser(templateProcessor);

        // When
        try
        {
            parser.parse("test-skill", skillContent);
            fail("Should throw exception for invalid frontmatter line");
        }
        catch (SkillExecutionException e)
        {
            // Then
            assertEquals(com.e1c.edt.ai.assistent.SkillErrorCode.SKILL_PARSE_ERROR, e.getCode());
        }
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldThrowExceptionWhenFrontmatterLineHasColonAtStart()
    {
        // Given
        String skillContent = "---\n:value starts with colon\n---\nBody";
        var parser = new SkillMdParser(templateProcessor);

        // When
        try
        {
            parser.parse("test-skill", skillContent);
            fail("Should throw exception for frontmatter line with colon at start");
        }
        catch (SkillExecutionException e)
        {
            // Then
            assertEquals(com.e1c.edt.ai.assistent.SkillErrorCode.SKILL_PARSE_ERROR, e.getCode());
        }
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldParseFrontmatterWithEmptyLines()
    {
        // Given
        String skillContent = "---\nname: test-skill\n\n\ndescription: Test\n---\nBody";
        var parser = new SkillMdParser(templateProcessor);

        // When
        var result = parser.parse("test-skill", skillContent);

        // Then
        assertNotNull(result);
        Map<String, String> metadataMap = result.getMetadata().getValues();
        assertEquals(2, metadataMap.size());
        assertEquals("test-skill", metadataMap.get("name"));
        assertEquals("Test", metadataMap.get("description"));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldParseFrontmatterWithTrimmedValues()
    {
        // Given
        String skillContent = "---\nname:  test-skill  \ndescription:  Test description  \n---\nBody";
        var parser = new SkillMdParser(templateProcessor);

        // When
        var result = parser.parse("test-skill", skillContent);

        // Then
        assertNotNull(result);
        Map<String, String> metadataMap = result.getMetadata().getValues();
        assertEquals("test-skill", metadataMap.get("name"));
        assertEquals("Test description", metadataMap.get("description"));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldParseFrontmatterWithSpecialCharacters()
    {
        // Given
        String skillContent = "---\nname: test-skill-v1.2\ndescription: Test with special chars: !@#$%^&*()\n---\nBody";
        var parser = new SkillMdParser(templateProcessor);

        // When
        var result = parser.parse("test-skill", skillContent);

        // Then
        assertNotNull(result);
        Map<String, String> metadataMap = result.getMetadata().getValues();
        assertEquals("test-skill-v1.2", metadataMap.get("name"));
        assertEquals("Test with special chars: !@#$%^&*()", metadataMap.get("description"));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNormalizeLineEndings()
    {
        // Given
        String skillContent = "---\r\nname: test-skill\r\n---\r\nBody content";
        var parser = new SkillMdParser(templateProcessor);

        // When
        var result = parser.parse("test-skill", skillContent);

        // Then
        assertNotNull(result);
        assertEquals("Body content", result.getTemplate());
        Map<String, String> metadataMap = result.getMetadata().getValues();
        assertEquals("test-skill", metadataMap.get("name"));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldHandleMultipleFrontmatterKeysWithSameName()
    {
        // Given
        String skillContent = "---\nname: first\nname: second\n---\nBody";
        var parser = new SkillMdParser(templateProcessor);

        // When
        var result = parser.parse("test-skill", skillContent);

        // Then
        assertNotNull(result);
        Map<String, String> metadataMap = result.getMetadata().getValues();
        assertEquals(1, metadataMap.size());
        assertEquals("second", metadataMap.get("name")); // Last value wins
    }
}
