/**
 *
 */
package com.e1c.edt.ai.assistent;

public class RetryAttempt
{
    public final Throwable cause;
    public final int attempt;

    public RetryAttempt(Throwable cause, int attempt)
    {
        this.cause = cause;
        this.attempt = attempt;
    }
}
