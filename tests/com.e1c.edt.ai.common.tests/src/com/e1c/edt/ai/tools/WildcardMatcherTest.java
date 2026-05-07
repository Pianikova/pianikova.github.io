/**
 *
 */
package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class WildcardMatcherTest
{
    private IWildcardMatcher matcher;

    @Before
    public void setUp()
    {
        matcher = new WildcardMatcher();
    }

    @Test
    public void testExactMatch()
    {
        assertTrue(matcher.matches("TypeDescriptionBuilder", "TypeDescriptionBuilder")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("TypeDescriptionBuilder", "TypeDescription")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testPrefixWildcard()
    {
        assertTrue(matcher.matches("set*", "setNumberQualifiers")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("set*", "set")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("set*", "getNumberQualifiers")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testSuffixWildcard()
    {
        assertTrue(matcher.matches("*HierarchyType", "HierarchyType")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("*HierarchyType", "MdHierarchyType")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("*HierarchyType", "HierarchyKind")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testMiddleWildcard()
    {
        assertTrue(matcher.matches("get*Name", "getFullName")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("get*Name", "getName")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("get*Name", "setFullName")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testFqnWildcard()
    {
        assertTrue(matcher.matches("com.example.*Builder", "com.example.TypeDescriptionBuilder")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("com.example.*Builder", "com.other.TypeDescriptionBuilder")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testHasWildcard()
    {
        assertTrue(matcher.hasWildcard("Type*")); //$NON-NLS-1$
        assertFalse(matcher.hasWildcard("TypeDescriptionBuilder")); //$NON-NLS-1$
        assertFalse(matcher.hasWildcard(null));
    }

    @Test
    public void testNullAndEmptyValues()
    {
        assertFalse(matcher.matches(null, "TypeDescriptionBuilder")); //$NON-NLS-1$
        assertFalse(matcher.matches("Type*", null)); //$NON-NLS-1$
        assertTrue(matcher.matches("", "")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("", "TypeDescriptionBuilder")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
