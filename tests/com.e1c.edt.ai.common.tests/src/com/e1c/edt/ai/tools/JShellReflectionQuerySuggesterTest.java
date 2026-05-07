/**
 *
 */
package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class JShellReflectionQuerySuggesterTest
{
    private IJShellReflectionQuerySuggester suggester;

    @Before
    public void setUp()
    {
        suggester = new JShellReflectionQuerySuggester();
    }

    @Test
    public void testSuggestsAccessorPatternsForMemberQuery()
    {
        var suggestions = suggester.suggestForQuery("TypeDescriptionBuilder.numberQualifiers*", 8); //$NON-NLS-1$

        assertTrue(suggestions.contains("TypeDescriptionBuilder.*numberQualifiers*")); //$NON-NLS-1$
        assertTrue(suggestions.contains("TypeDescriptionBuilder.set*NumberQualifiers*")); //$NON-NLS-1$
        assertTrue(suggestions.contains("TypeDescriptionBuilder.get*NumberQualifiers*")); //$NON-NLS-1$
    }

    @Test
    public void testSuggestsWildcardTypeQuery()
    {
        var suggestions = suggester.suggestForQuery("com._1c.g5.v8.dt.mcore.Qualifiers", 8); //$NON-NLS-1$

        assertTrue(suggestions.contains("com._1c.g5.v8.dt.mcore.*Qualifiers*")); //$NON-NLS-1$
        assertTrue(suggestions.contains("*Qualifiers*")); //$NON-NLS-1$
    }

    @Test
    public void testSuggestsFromCompilationErrorsAndCode()
    {
        var error = new CompilationError();
        error.message = "cannot find symbol\\n  symbol:   method numberQualifiers(int)"; //$NON-NLS-1$

        var suggestions = suggester.suggestForCompilationErrors(
            "new TypeDescriptionBuilder().numberQualifiers(4);", //$NON-NLS-1$
            List.of(error),
            12);

        assertTrue(suggestions.contains("TypeDescriptionBuilder.*numberQualifiers*")); //$NON-NLS-1$
        assertTrue(suggestions.contains("*numberQualifiers*")); //$NON-NLS-1$
    }

    @Test
    public void testSuggestsEnumOwnerFromCompilationLocation()
    {
        var error = new CompilationError();
        error.message =
            "cannot find symbol\n  symbol:   variable Turnovers\n  location: class com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterType"; //$NON-NLS-1$

        var suggestions = suggester.suggestForCompilationErrors(
            "register.setRegisterType(AccumulationRegisterType.Turnovers);", //$NON-NLS-1$
            List.of(error),
            12);

        assertTrue(suggestions.contains("com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegisterType.*")); //$NON-NLS-1$
        assertTrue(suggestions.contains("AccumulationRegisterType.*")); //$NON-NLS-1$
    }
}
