/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillExecutionResult
{   
    private final String prompt;
    
    public SkillExecutionResult(String prompt)
    {
        this.prompt = prompt;
    }
    
    public String getPrompt()
    {
        return prompt;
    }    
}
