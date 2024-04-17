/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

public interface IChat
{
    void reviewCode(String codeSnippet);

    void explainCode(String codeSnippet);

    void fixCode(String codeSnippet);

    void generateDocComments(String method);

    void askQuestion(String userQuestion);

}
