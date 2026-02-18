/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.List;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.McpCallToolsResult;

public interface IChat
{
    void reviewCode(AIContext ctx, String codeSnippet);

    void explainCode(AIContext ctx, String codeSnippet);

    void fixCode(AIContext ctx, String codeSnippet, String details);

    void generateDocComments(AIContext ctx, String method);

    void askQuestion(AIContext ctx, String userQuestion);

    void addCode(AIContext ctx, String codeSnippet);

    public void addFiles(List<IFileDocument> documents);

    void addToolsResult(String chatId, String messageId, McpCallToolsResult result);

    void continueChat(String chatId, String text);
}
