/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CodeCompletionActionHandlerTest
{
    private static final int DEFAULT_OFFSET = 33;
    @SuppressWarnings("unchecked")
    private static final ICodeCompletionSession<ICodeCompletionContext> DefaultSesssion =
        mock(ICodeCompletionSession.class);

    @Parameter(0)
    public String description;

    @Parameter(1)
    public ICodeCompletionSession<ICodeCompletionContext> sesssion;

    @Parameter(2)
    public CodeCompletionAction action;

    @Parameter(3)
    public Character character;

    @Parameter(4)
    public int offset;

    @Parameter(5)
    public boolean isContinuousCodeCompletion;

    @Parameter(6)
    public CodeCompletionAction expectedAction;

    @Parameter(7)
    public Runnable verifySession;

    @SuppressWarnings("unchecked")
    @Test
    @Parameters()
    public void shouldHandle()
    {
        // Given
        reset(DefaultSesssion);
        when(DefaultSesssion.finish()).thenReturn(CodeCompletionAction.TEST);
        when(DefaultSesssion.rollback(DEFAULT_OFFSET)).thenReturn(CodeCompletionAction.TEST);
        when(DefaultSesssion.accept(HintPart.TOKEN, DEFAULT_OFFSET)).thenReturn(CodeCompletionAction.TEST);
        when(DefaultSesssion.accept(HintPart.LINE, DEFAULT_OFFSET)).thenReturn(CodeCompletionAction.TEST);
        when(DefaultSesssion.accept(HintPart.LINES, DEFAULT_OFFSET)).thenReturn(CodeCompletionAction.TEST);
        when(DefaultSesssion.acceptChar(DEFAULT_OFFSET, 'a')).thenReturn(CodeCompletionAction.TEST);
        var<ICodeCompletionContext> handler = createInstance();

        // When
        var actualAction = handler.handle(sesssion, action, character, offset, isContinuousCodeCompletion);

        // Then
        Assert.assertEquals(expectedAction, actualAction);
        verifySession.run();
    }

    private CodeCompletionActionHandler<ICodeCompletionContext> createInstance()
    {
        return new CodeCompletionActionHandler<>();
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "SUGGEST", null, CodeCompletionAction.SUGGEST, ' ', -1, false, CodeCompletionAction.SUGGEST, verifyActions(() -> { /**/ })},
                { "SUGGEST when continuous", null, CodeCompletionAction.SUGGEST, ' ', -1, true, CodeCompletionAction.SUGGEST, verifyActions(() -> { /**/ })},
                { "SUGGEST when session is not null", DefaultSesssion, CodeCompletionAction.SUGGEST, ' ', DEFAULT_OFFSET, false, CodeCompletionAction.SUGGEST, verifyActions(() -> { /**/ })},

                { "FINISH", DefaultSesssion, CodeCompletionAction.FINISH, ' ', DEFAULT_OFFSET, false, CodeCompletionAction.TEST, verifyActions(() -> { verify(DefaultSesssion).finish(); })},
                { "ROLLBACK_PART", DefaultSesssion, CodeCompletionAction.ROLLBACK_PART, ' ', DEFAULT_OFFSET, false, CodeCompletionAction.TEST, verifyActions(() -> { verify(DefaultSesssion).rollback(DEFAULT_OFFSET); })},
                { "ACCEPT", DefaultSesssion, CodeCompletionAction.ACCEPT, ' ', DEFAULT_OFFSET, false, CodeCompletionAction.TEST, verifyActions(() -> { verify(DefaultSesssion).accept(HintPart.LINES, DEFAULT_OFFSET); })},
                { "ACCEPT_PART", DefaultSesssion, CodeCompletionAction.ACCEPT_PART, ' ', DEFAULT_OFFSET, false, CodeCompletionAction.TEST, verifyActions(() -> { verify(DefaultSesssion).accept(HintPart.TOKEN, DEFAULT_OFFSET); })},
                { "ACCEPT_LINE", DefaultSesssion, CodeCompletionAction.ACCEPT_LINE, ' ', DEFAULT_OFFSET, false, CodeCompletionAction.TEST, verifyActions(() -> { verify(DefaultSesssion).accept(HintPart.LINE, DEFAULT_OFFSET); })},
                { "ACCEPT_CHAR", DefaultSesssion, CodeCompletionAction.ACCEPT_CHAR, 'a', DEFAULT_OFFSET, false, CodeCompletionAction.TEST, verifyActions(() -> { verify(DefaultSesssion).acceptChar(DEFAULT_OFFSET, 'a'); })},

                { "ASK_NEW when session is null and continuous and some non CONTROL char", null, CodeCompletionAction.ACCEPT, 'a', DEFAULT_OFFSET, true, CodeCompletionAction.ASK_NEW, verifyActions(() -> { verify(DefaultSesssion, never()).accept(HintPart.LINES, DEFAULT_OFFSET); })},
                { "ASK_NEW when session is null and continuous and \n", null, CodeCompletionAction.ACCEPT, '\n', DEFAULT_OFFSET, true, CodeCompletionAction.ASK_NEW, verifyActions(() -> { verify(DefaultSesssion, never()).accept(HintPart.LINES, DEFAULT_OFFSET); })},
                { "ASK_NEW when session is null and continuous and \r", null, CodeCompletionAction.ACCEPT, '\r', DEFAULT_OFFSET, true, CodeCompletionAction.ASK_NEW, verifyActions(() -> { verify(DefaultSesssion, never()).accept(HintPart.LINES, DEFAULT_OFFSET); })},
                { "ASK_NEW when session is null and continuous and \t", null, CodeCompletionAction.ACCEPT, '\t', DEFAULT_OFFSET, true, CodeCompletionAction.ASK_NEW, verifyActions(() -> { verify(DefaultSesssion, never()).accept(HintPart.LINES, DEFAULT_OFFSET); })},

                { "SKIP when session is null and continuous and . char", null, CodeCompletionAction.ACCEPT, '.', DEFAULT_OFFSET, true, CodeCompletionAction.SKIP, verifyActions(() -> { verify(DefaultSesssion, never()).accept(HintPart.LINES, DEFAULT_OFFSET); })},
                { "SKIP when session is null and not continuous and \n", null, CodeCompletionAction.ACCEPT, '\n', DEFAULT_OFFSET, false, CodeCompletionAction.SKIP, verifyActions(() -> { verify(DefaultSesssion, never()).accept(HintPart.LINES, DEFAULT_OFFSET); })},
                { "SKIP when session is null and not continuous and \r", null, CodeCompletionAction.ACCEPT, '\r', DEFAULT_OFFSET, false, CodeCompletionAction.SKIP, verifyActions(() -> { verify(DefaultSesssion, never()).accept(HintPart.LINES, DEFAULT_OFFSET); })},
                { "SKIP when session is null and not continuous and \t", null, CodeCompletionAction.ACCEPT, '\t', DEFAULT_OFFSET, false, CodeCompletionAction.SKIP, verifyActions(() -> { verify(DefaultSesssion, never()).accept(HintPart.LINES, DEFAULT_OFFSET); })},
            });
        // @formatter:on
    }

    private static Runnable verifyActions(Runnable action)
    {
        return action;
    }
}
