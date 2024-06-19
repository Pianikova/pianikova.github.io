/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import static org.mockito.Mockito.mock;

import org.e1c.edt.ai.CodeCompletionAction;
import org.e1c.edt.ai.CodeCompletionActionHandler;
import org.e1c.edt.ai.ICodeCompletionContext;
import org.e1c.edt.ai.ICodeCompletionSession;
import org.junit.Assert;
import org.junit.Test;

public class CodeCompletionActionHandlerTest
{
    @SuppressWarnings("unchecked")
    private final ICodeCompletionSession<ICodeCompletionContext> sesssion = mock(ICodeCompletionSession.class);

    @Test
    public void shouldHandleSUGGEST()
    {
        // Given
        var<ICodeCompletionContext> handler = createInstance();

        // When
        var actualAction = handler.handle(sesssion, CodeCompletionAction.SUGGEST, ' ', 0, false);

        // Then
        Assert.assertEquals(CodeCompletionAction.SUGGEST, actualAction);
    }

    private CodeCompletionActionHandler<ICodeCompletionContext> createInstance()
    {
        return new CodeCompletionActionHandler<>();
    }
}
