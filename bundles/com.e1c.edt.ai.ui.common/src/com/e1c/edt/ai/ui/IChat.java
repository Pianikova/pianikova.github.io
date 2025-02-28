/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;

public interface IChat
{
    void reviewCode(AIContext ctx, String codeSnippet);

    void explainCode(AIContext ctx, String codeSnippet);

    void fixCode(AIContext ctx, String codeSnippet, String details);

    void generateDocComments(AIContext ctx, String method);

    void askQuestion(AIContext ctx, String userQuestion);
}
