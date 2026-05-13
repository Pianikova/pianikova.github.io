/**
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * @author Bogdan Sushkov
 *
 */
public class RegexTemplateProcessorTest
{
    private final RegexTemplateProcessor processor = new RegexTemplateProcessor();

    @SuppressWarnings("nls")
    @Test
    public void shouldFindSinglePlaceholder()
    {
        // Given
        String text = "Hello ${name}!";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        Set<String> result = processor.find(text, pattern);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.contains("name"));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldFindMultiplePlaceholders()
    {
        // Given
        String text = "Hello ${name}, your age is ${age} and city is ${city}";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        Set<String> result = processor.find(text, pattern);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("name"));
        assertTrue(result.contains("age"));
        assertTrue(result.contains("city"));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldFindPlaceholdersWithSpecialCharacters()
    {
        // Given
        String text = "Values: ${user.name}, ${api_key}, ${test-value_1}";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        Set<String> result = processor.find(text, pattern);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("user.name"));
        assertTrue(result.contains("api_key"));
        assertTrue(result.contains("test-value_1"));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnEmptySetWhenNoPlaceholdersFound()
    {
        // Given
        String text = "Hello world, no placeholders here";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        Set<String> result = processor.find(text, pattern);

        // Then
        assertTrue(result.isEmpty());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldFindPlaceholdersInOrderOfAppearance()
    {
        // Given
        String text = "First ${a} then ${b} then ${c} then ${a} again";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        Set<String> result = processor.find(text, pattern);

        // Then
        assertEquals(3, result.size());
        // Check order is maintained
        String[] resultArray = result.toArray(new String[0]);
        assertEquals("a", resultArray[0]);
        assertEquals("b", resultArray[1]);
        assertEquals("c", resultArray[2]);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldFindDuplicatesOnlyOnce()
    {
        // Given
        String text = "${name} ${name} ${name}";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        Set<String> result = processor.find(text, pattern);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.contains("name"));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReplaceSinglePlaceholder()
    {
        // Given
        String text = "Hello ${name}!";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        String result = processor.replace(text, pattern, match -> match.group(1).toUpperCase());

        // Then
        assertEquals("Hello NAME!", result);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReplaceMultiplePlaceholders()
    {
        // Given
        String text = "Hello ${name}, your age is ${age}";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        String result = processor.replace(text, pattern, match -> "[" + match.group(1) + "]");

        // Then
        assertEquals("Hello [name], your age is [age]", result);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReplaceWithDifferentValues()
    {
        // Given
        String text = "Values: ${a}, ${b}, ${c}";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        String result = processor.replace(text, pattern, match -> {
            String key = match.group(1);
            switch (key)
            {
                case "a":
                    return "1";
                case "b":
                    return "2";
                case "c":
                    return "3";
                default:
                    return "0";
            }
        });

        // Then
        assertEquals("Values: 1, 2, 3", result);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReplaceWithSpecialCharacters()
    {
        // Given
        String text = "Value: ${key}";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        String result = processor.replace(text, pattern, match -> "special !@#$%^&*() value");

        // Then
        assertEquals("Value: special !@#$%^&*() value", result);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotReplaceWhenPatternNotFound()
    {
        // Given
        String text = "Hello world";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        String result = processor.replace(text, pattern, match -> "REPLACED");

        // Then
        assertEquals("Hello world", result);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReplaceWithEmptyString()
    {
        // Given
        String text = "Remove ${this} placeholder";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        String result = processor.replace(text, pattern, match -> "");

        // Then
        assertEquals("Remove  placeholder", result);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldFindWithCustomGroupIndex()
    {
        // Given
        String text = "Match group 1: ${a}, group 2: ${b}";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+):([a-zA-Z0-9._-]+)}");

        // When
        Set<String> result = processor.find(text, pattern, 1);

        // Then
        assertTrue(result.isEmpty()); // No matches with colon pattern
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldHandleEmptyString()
    {
        // Given
        String text = "";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        Set<String> findResult = processor.find(text, pattern);
        String replaceResult = processor.replace(text, pattern, match -> "X");

        // Then
        assertTrue(findResult.isEmpty());
        assertEquals("", replaceResult);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldThrowExceptionWhenNullPattern()
    {
        // Given
        String text = "Value: ${key}";

        // When
        assertThrows(NullPointerException.class, () -> processor.replace(text, null, match -> match.group(1)));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldThrowExceptionWhenNullReplacementProvider()
    {
        // Given
        String text = "Value: ${key}";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        assertThrows(NullPointerException.class, () -> processor.replace(text, pattern, null));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnNullWhenNullText()
    {
        // When
        String result = processor.replace(null, Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}"), match -> match.group(1));

        // Then
        assertNull(result);
    }


    @SuppressWarnings("nls")
    @Test
    public void shouldReplaceConsecutivePlaceholders()
    {
        // Given
        String text = "${a}${b}${c}";
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}");

        // When
        String result = processor.replace(text, pattern, match -> match.group(1));

        // Then
        assertEquals("abc", result);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldHandleToolDirectivesPattern()
    {
        // Given
        String text = "Execute !tool('git_diff') and then !tool('git_recent_commits')";
        Pattern pattern = Pattern.compile("!tool\\('([a-zA-Z0-9._-]+)'\\)");

        // When
        Set<String> result = processor.find(text, pattern);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains("git_diff"));
        assertTrue(result.contains("git_recent_commits"));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReplaceToolDirectives()
    {
        // Given
        String text = "Result: !tool('git_diff')";
        Pattern pattern = Pattern.compile("!tool\\('([a-zA-Z0-9._-]+)'\\)");

        // When
        String result = processor.replace(text, pattern, match -> "TOOL_RESULT: " + match.group(1));

        // Then
        assertEquals("Result: TOOL_RESULT: git_diff", result);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldHandlePatternWithSpecialRegexCharacters()
    {
        // Given
        String text = "Match [a] and [b]";
        Pattern pattern = Pattern.compile("\\[([a-z])\\]");

        // When
        Set<String> result = processor.find(text, pattern);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }
}
