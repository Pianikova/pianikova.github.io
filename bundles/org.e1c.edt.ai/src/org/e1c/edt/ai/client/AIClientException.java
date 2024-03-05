/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client;

/**
 * This class specifies exceptions wich can be occured during executing API.
 * @author Bogdan Sushkov
 *
 */
public class AIClientException
    extends RuntimeException
{
    /**
     * Exception constructor
     * @param message
     * @param exception
     */
    public AIClientException(String message, Throwable exception)
    {
        super(message, exception);
    }
}
