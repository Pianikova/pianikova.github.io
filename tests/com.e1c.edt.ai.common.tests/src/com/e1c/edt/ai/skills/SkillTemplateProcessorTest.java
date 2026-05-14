/**
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.RegexTemplateProcessor;
import com.e1c.edt.ai.assistent.SkillExecutionException;

/**
 * @author Bogdan Sushkov
 *
 */
@SuppressWarnings("nls")
public class SkillTemplateProcessorTest
{
    private final RegexTemplateProcessor templateProcessor = new RegexTemplateProcessor();
    private final IJson json = mock(IJson.class);

    @Test
    public void shouldFindSingleToolDirective()
    {
        // Given
        String text = "Execute !tool('git_diff') and get result";
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        Set<String> result = processor.findToolIds(text);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.contains("git_diff"));
    }

    @Test
    public void shouldFindMultipleToolDirectives()
    {
        // Given
        String text = "Execute !tool('git_diff') and then !tool('git_recent_commits')";
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        Set<String> result = processor.findToolIds(text);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains("git_diff"));
        assertTrue(result.contains("git_recent_commits"));
    }

    @Test
    public void shouldReturnEmptySetWhenNoToolDirectives()
    {
        // Given
        String text = "No tool directives here";
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        Set<String> result = processor.findToolIds(text);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    public void shouldResolveSinglePlaceholder()
    {
        // Given
        String text = "Hello ${name}!";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("name", "World");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.resolvePlaceholders(text, parameters);

        // Then
        assertEquals("Hello World!", result);
    }

    @Test
    public void shouldResolveMultiplePlaceholders()
    {
        // Given
        String text = "Hello ${name}, your age is ${age} and city is ${city}";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("name", "John");
        parameters.put("age", "30");
        parameters.put("city", "Moscow");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.resolvePlaceholders(text, parameters);

        // Then
        assertEquals("Hello John, your age is 30 and city is Moscow", result);
    }

    @Test
    public void shouldThrowExceptionWhenParameterMissing()
    {
        // Given
        String text = "Hello ${name}!";
        Map<String, String> parameters = new HashMap<>();
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        try
        {
            processor.resolvePlaceholders(text, parameters);
            fail("Should throw exception for missing parameter");
        }
        catch (SkillExecutionException e)
        {
            // Then
            assertTrue(e.getMessage().contains("Missing skill parameter: name"));
        }
    }

    @Test
    public void shouldResolvePlaceholdersWithSpecialCharacters()
    {
        // Given
        String text = "Values: ${key1}, ${key_2}, ${key.3}";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key_2", "value2");
        parameters.put("key.3", "value3");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.resolvePlaceholders(text, parameters);

        // Then
        assertEquals("Values: value1, value2, value3", result);
    }

    @Test
    public void shouldReplacePlaceholdersWithEmptyString()
    {
        // Given
        String text = "Remove ${empty} placeholder";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("empty", "");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.resolvePlaceholders(text, parameters);

        // Then
        assertEquals("Remove  placeholder", result);
    }

    @Test
    public void shouldResolvePlaceholderWithSpecialCharsInValue()
    {
        // Given
        String text = "Value: ${key}";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key", "special !@#$%^&*() value");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.resolvePlaceholders(text, parameters);

        // Then
        assertEquals("Value: special !@#$%^&*() value", result);
    }

    @Test
    public void shouldResolveJsonPlaceholders()
    {
        // Given
        String text = "{\"key\": \"${value}\"}";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("value", "test");
        when(json.serialize("test")).thenReturn("\"test\"");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.resolveJsonPlaceholders(text, parameters);

        // Then
        assertEquals("{\"key\": \"test\"}", result);
    }

    @Test
    public void shouldResolveJsonPlaceholderWithQuotes()
    {
        // Given
        String text = "{\"key\": \"${value}\"}";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("value", "test \"quoted\" value");
        when(json.serialize("test \"quoted\" value")).thenReturn("\"test \\\"quoted\\\" value\"");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.resolveJsonPlaceholders(text, parameters);

        // Then
        assertEquals("{\"key\": \"test \\\"quoted\\\" value\"}", result);
    }

    @Test
    public void shouldThrowExceptionWhenJsonParameterMissing()
    {
        // Given
        String text = "{\"key\": \"${value}\"}";
        Map<String, String> parameters = new HashMap<>();
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        try
        {
            processor.resolveJsonPlaceholders(text, parameters);
            fail("Should throw exception for missing parameter");
        }
        catch (SkillExecutionException e)
        {
            // Then
            assertTrue(e.getMessage().contains("Missing skill parameter: value"));
        }
    }

    @Test
    public void shouldThrowExceptionWhenJsonParameterIsNull()
    {
        // Given
        String text = "{\"key\": \"${value}\"}";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("value", null);
        when(json.serialize(null)).thenReturn("null");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        try
        {
            processor.resolveJsonPlaceholders(text, parameters);
            fail("Should throw exception for missing parameter");
        }
        catch (SkillExecutionException e)
        {
            // Then
            assertTrue(e.getMessage().contains("Null skill parameter for key"));
        }
    }

    @Test
    public void shouldReplaceSingleToolResult()
    {
        // Given
        String text = "Result: !tool('git_diff')";
        Map<String, String> toolResults = new HashMap<>();
        toolResults.put("git_diff", "file1.java: changed");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.replaceToolResults(text, toolResults);

        // Then
        assertEquals("Result: file1.java: changed", result);
    }

    @Test
    public void shouldReplaceMultipleToolResults()
    {
        // Given
        String text = "Diff: !tool('git_diff')\nCommits: !tool('git_recent_commits')";
        Map<String, String> toolResults = new HashMap<>();
        toolResults.put("git_diff", "diff content");
        toolResults.put("git_recent_commits", "commit messages");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.replaceToolResults(text, toolResults);

        // Then
        assertEquals("Diff: diff content\nCommits: commit messages", result);
    }

    @Test
    public void shouldThrowExceptionWhenToolResultMissing()
    {
        // Given
        String text = "Result: !tool('missing_tool')";
        Map<String, String> toolResults = new HashMap<>();
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        try
        {
            processor.replaceToolResults(text, toolResults);
            fail("Should throw exception for missing tool result");
        }
        catch (SkillExecutionException e)
        {
            // Then
            assertTrue(e.getMessage().contains("Missing tool result: missing_tool"));
        }
    }

    @Test
    public void shouldHandleToolResultWithSpecialCharacters()
    {
        // Given
        String text = "Result: !tool('git_diff')";
        Map<String, String> toolResults = new HashMap<>();
        toolResults.put("git_diff", "diff !@#$%^&*() content\nwith\nnewlines");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.replaceToolResults(text, toolResults);

        // Then
        assertEquals("Result: diff !@#$%^&*() content\nwith\nnewlines", result);
    }

    @Test
    public void shouldNotReplaceWhenNoToolDirectives()
    {
        // Given
        String text = "No tool directives here";
        Map<String, String> toolResults = new HashMap<>();
        toolResults.put("git_diff", "some result");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.replaceToolResults(text, toolResults);

        // Then
        assertEquals("No tool directives here", result);
    }

    @Test
    public void shouldHandleMultipleSameToolDirectives()
    {
        // Given
        String text = "First: !tool('git_diff'), Second: !tool('git_diff')";
        Map<String, String> toolResults = new HashMap<>();
        toolResults.put("git_diff", "result");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.replaceToolResults(text, toolResults);

        // Then
        assertEquals("First: result, Second: result", result);
    }

    @Test
    public void shouldResolvePlaceholderInComplexTemplate()
    {
        // Given
        String text = "---\nname: ${skill_name}\n---\nBody with ${param1} and ${param2}";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("skill_name", "my-skill");
        parameters.put("param1", "value1");
        parameters.put("param2", "value2");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.resolvePlaceholders(text, parameters);

        // Then
        assertEquals("---\nname: my-skill\n---\nBody with value1 and value2", result);
    }

    @Test
    public void shouldHandlePlaceholderWithDotsAndDashes()
    {
        // Given
        String text = "Value: ${user.name} and ${api-key-v1}";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("user.name", "John");
        parameters.put("api-key-v1", "123");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.resolvePlaceholders(text, parameters);

        // Then
        assertEquals("Value: John and 123", result);
    }

    @Test
    public void shouldHandleEmptyParametersMap()
    {
        // Given
        String text = "No placeholders here";
        Map<String, String> parameters = new HashMap<>();
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        String result = processor.resolvePlaceholders(text, parameters);

        // Then
        assertEquals("No placeholders here", result);
    }

    @Test
    public void shouldThrowExceptionWhenTemplateContainsUnknownPlaceholder()
    {
        // Given
        String text = "Value: ${missing_key}";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("other_key", "value");
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        try
        {
            processor.resolvePlaceholders(text, parameters);
            fail("Should throw exception for unknown placeholder");
        }
        catch (SkillExecutionException e)
        {
            // Then
            assertTrue(e.getMessage().contains("Missing skill parameter: missing_key"));
        }
    }

    @Test
    public void shouldHandleNullPlaceholderValue()
    {
        // Given
        String text = "Value: ${key}";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key", null);
        var processor = new SkillTemplateProcessor(templateProcessor, json);

        // When
        try
        {
            processor.resolvePlaceholders(text, parameters);
            fail("Should throw exception for null parameter value");
        }
        catch (SkillExecutionException e)
        {
            // Then
            assertTrue(e.getMessage().contains("Null skill parameter for key: key"));
        }
    }
}
