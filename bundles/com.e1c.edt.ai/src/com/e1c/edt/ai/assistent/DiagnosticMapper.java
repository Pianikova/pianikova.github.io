/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLHandshakeException;

import com.e1c.edt.ai.ServiceState;

/**
 * @author Bogdan Sushkov
 *
 */
public class DiagnosticMapper
    implements IDiagnosticMapper
{

    @SuppressWarnings("nls")
    @Override
    public DiagnosticResult map(String stage, int responseStatus, Throwable tRaw,
        Map<String, String> extraInfo)
    {
        Throwable t = unwrap(tRaw);
        final String st = stacktrace(t);

        Map<String, String> facts = new HashMap<>();
        if (extraInfo != null)
        {
            facts.putAll(extraInfo);
        }
        facts.put("stage", stage);

        if (t != null)
        {
            facts.put("exception", t.getClass().getName());

            // 1) HTTP
            if (t instanceof AIClientException)
            {
                AIClientException exception = (AIClientException)t;
                int status = exception.getStatusCode();
                facts.put("httpStatus", String.valueOf(status));
                return mapHttpStatus(status, t, facts);
            }

            // 2) PKIX / SSLHandshakeException
            if (isPKIXValidationException(t))
            {
                return DiagnosticResult.error(Messages.DiagnosticMapper_PKIX, ServiceState.SSL_ERROR, facts, t, st);
            }

            // 3) Timeout
            if (hasCause(t, HttpTimeoutException.class))
            {
                return DiagnosticResult.error(Messages.DiagnosticMapper_Timeout, ServiceState.NONE, facts, t, st);
            }

            // 4) DNS
            if (hasCause(t, UnknownHostException.class))
            {
                return DiagnosticResult.error(Messages.DiagnosticMapper_DNS, ServiceState.NONE, facts, t, st);
            }
        }

        return mapHttpStatus(responseStatus, t, facts);
    }


    /**
     * This method maps HTTP status codes to diagnostic messages.
     *
     * @param status - HTTP status code
     * @param t      - exception
     * @param facts  - facts
     * @return DiagnosticResult {@link DiagnosticResult}
     */
    private DiagnosticResult mapHttpStatus(int status, Throwable t, Map<String, String> facts)
    {
        final String st = stacktrace(t);

        final String msg;
        ServiceState remediation = ServiceState.TOKEN_ERROR;

        if (status >= 500 && status <= 599)
        {
            msg = Messages.DiagnosticMapper_500;
            remediation = ServiceState.SERVER_ERROR;
        }
        else
        {
            switch (status)
            {
            case 401:
                msg = Messages.DiagnosticMapper_401;
                break;
            case 403:
                msg = Messages.DiagnosticMapper_403;
                break;
            case 407:
                msg = Messages.DiagnosticMapper_407;
                break;
            default:
                msg = Messages.DiagnosticMapper_Unknown;
                remediation = ServiceState.NONE;
            }
        }

        return DiagnosticResult.error(msg, remediation, facts, t, st);
    }

    /**
     * Get true Throwable from one that has been wrapped in CompletableFuture
     *
     * @param tRaw
     * @return
     */
    private Throwable unwrap(Throwable throwable)
    {
        if (throwable == null)
        {
            return null;
        }

        while ((throwable instanceof CompletionException || throwable instanceof ExecutionException)
            && throwable.getCause() != null)
        {
            throwable = throwable.getCause();
        }
        return throwable;
    }

    private boolean isPKIXValidationException(Throwable t)
    {
        String message = t.getMessage();
        if (message != null && message.contains("PKIX")) //$NON-NLS-1$
        {
            return true;
        }

        return hasCause(t, SSLHandshakeException.class);
    }

    private boolean hasCause(Throwable t, Class<? extends Throwable> classT)
    {
        while (t != null)
        {
            if (classT.isInstance(t))
            {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private String stacktrace(Throwable t)
    {
        if (t == null)
        {
            return "Stacktrace is not available"; //$NON-NLS-1$
        }
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}
