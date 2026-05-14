/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillExecutionException
    extends RuntimeException
{
    private final SkillErrorCode code;

    public SkillExecutionException(SkillErrorCode code, String message)
    {
        super(message);
        this.code = code;
    }

    public SkillExecutionException(SkillErrorCode code, String message, Throwable cause)
    {
        super(message, cause);
        this.code = code;
    }

    public SkillErrorCode getCode()
    {
        return code;
    }
}
