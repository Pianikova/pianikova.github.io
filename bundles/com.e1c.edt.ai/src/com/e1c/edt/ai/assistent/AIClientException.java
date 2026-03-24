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
    private final int statusCode;
    private final String body;

    public AIClientException(String message, int code, String body, Throwable exception)
    {
        super(message, exception);
        this.statusCode = code;
        this.body = body;
    }

    public AIClientException(String message, int code, Throwable exception)
    {
        this(message, code, null, exception);
    }

    public AIClientException(String message, Throwable exception)
    {
        this(message, 0, null, exception);
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getBody()
    {
        return body;
    }
}
