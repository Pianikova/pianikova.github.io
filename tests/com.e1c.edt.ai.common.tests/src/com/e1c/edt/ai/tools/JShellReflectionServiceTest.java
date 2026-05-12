/**
 *
 */
package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class JShellReflectionServiceTest
{
    private IJShellReflectionService service;

    @Before
    public void setUp()
    {
        var matcher = new WildcardMatcher();
        var typeIndex = new JShellTypeIndex(matcher);
        var formatter = new ReflectionSignatureFormatter();
        var memberResolver = new JShellMemberResolver(typeIndex, matcher, formatter);
        var suggester = new JShellReflectionQuerySuggester();
        service = new JShellReflectionService(typeIndex, memberResolver, formatter, suggester);
    }

    @Test
    public void testMultipleQueriesPreserveOrder()
    {
        var results = service.search(new TestSession(), List.of(
            "java.lang.String.substring", //$NON-NLS-1$
            "java.lang.String.starts*")); //$NON-NLS-1$

        assertEquals(2, results.size());
        assertEquals("java.lang.String.substring", results.get(0).query); //$NON-NLS-1$
        assertEquals("java.lang.String.starts*", results.get(1).query); //$NON-NLS-1$
    }

    @Test
    public void testExactMemberLookup()
    {
        var results = service.search(new TestSession(), List.of("java.lang.String.substring")); //$NON-NLS-1$

        assertEquals("member-search", results.get(0).kind); //$NON-NLS-1$
        assertFalse(results.get(0).results.isEmpty());
        assertEquals("java.lang.String", results.get(0).results.get(0).fqn); //$NON-NLS-1$
        assertTrue(results.get(0).results.get(0).items.stream().anyMatch(item -> item.startsWith("substring("))); //$NON-NLS-1$
    }

    @Test
    public void testWildcardMemberLookup()
    {
        var results = service.search(new TestSession(), List.of("java.lang.String.starts*")); //$NON-NLS-1$

        assertEquals("member-search", results.get(0).kind); //$NON-NLS-1$
        assertTrue(results.get(0).results.get(0).items.stream().allMatch(item -> item.startsWith("starts"))); //$NON-NLS-1$
    }

    @Test
    public void testEnumWildcardReturnsConstantsOnly()
    {
        var results = service.search(new TestSession(), List.of("java.time.DayOfWeek.*")); //$NON-NLS-1$

        assertEquals("member-search", results.get(0).kind); //$NON-NLS-1$
        assertEquals("enum", results.get(0).results.get(0).kind); //$NON-NLS-1$
        assertTrue(results.get(0).results.get(0).items.contains("MONDAY")); //$NON-NLS-1$
        assertFalse(results.get(0).results.get(0).items.stream().anyMatch(item -> item.startsWith("wait("))); //$NON-NLS-1$
    }

    @Test
    public void testTypeLookupReturnsPublicMembers()
    {
        var results = service.search(new TestSession(), List.of("java.lang.String")); //$NON-NLS-1$

        assertEquals("type-search", results.get(0).kind); //$NON-NLS-1$
        assertTrue(results.get(0).results.get(0).items.stream().anyMatch(item -> item.startsWith("substring("))); //$NON-NLS-1$
    }

    @Test
    public void testExactFqnTypeLookupDoesNotRequireOsgiIndex()
    {
        var typeIndex = new FailingIndexWildcardMatcher();
        var formatter = new ReflectionSignatureFormatter();
        var memberResolver = new JShellMemberResolver(typeIndex, new WildcardMatcher(), formatter);
        var reflectionService = new JShellReflectionService(typeIndex, memberResolver, formatter,
            new JShellReflectionQuerySuggester());

        var results = reflectionService.search(new TestSession(), List.of("java.lang.String")); //$NON-NLS-1$

        assertEquals("type-search", results.get(0).kind); //$NON-NLS-1$
        assertEquals("java.lang.String", results.get(0).results.get(0).fqn); //$NON-NLS-1$
    }

    @Test
    public void testImportStatementQueryIsNormalizedToPackage()
    {
        var results = service.search(new TestSession(), List.of("import java.lang.*;")); //$NON-NLS-1$

        assertEquals("package-search", results.get(0).kind); //$NON-NLS-1$
        assertEquals("import java.lang.*;", results.get(0).query); //$NON-NLS-1$
        assertEquals("java.lang", results.get(0).results.get(0).fqn); //$NON-NLS-1$
    }

    @Test
    public void testNotFoundReturnsSuggestions()
    {
        var results = service.search(new TestSession(), List.of("java.lang.String.lengthy*")); //$NON-NLS-1$

        assertEquals("not-found", results.get(0).kind); //$NON-NLS-1$
        assertTrue(results.get(0).suggestions.contains("java.lang.String.*lengthy*")); //$NON-NLS-1$
    }

    private static class TestSession
        implements IJShellSession
    {
        @Override
        public String getSessionId()
        {
            return "test"; //$NON-NLS-1$
        }

        @Override
        public JShellExecutionResult execute(String code)
        {
            return new JShellExecutionResult();
        }

        @Override
        public SessionResult getSessionResult()
        {
            return new SessionResult();
        }

        @Override
        public ClassLoader getClassLoader()
        {
            return String.class.getClassLoader();
        }

        @Override
        public List<String> getImports()
        {
            return List.of();
        }

        @Override
        public void close()
        {
            // Nothing to close.
        }
    }

    private static class FailingIndexWildcardMatcher
        extends JShellTypeIndex
    {
        FailingIndexWildcardMatcher()
        {
            super(new WildcardMatcher());
        }

        @Override
        public boolean hasPackage(String packageName)
        {
            throw new AssertionError("Exact FQN lookup should not scan packages"); //$NON-NLS-1$
        }
    }
}
