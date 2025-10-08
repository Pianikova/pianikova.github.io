/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

/**
 * This class specifies exceptions wich can be occured during executing API.
 * @author Bogdan Sushkov
 *
 */
public class AIClientException
    extends RuntimeException
{
    private static final long serialVersionUID = 8033741557264378457L;

    public AIClientException(String message, Throwable exception)
    {
        super(message, exception);
    }
}
