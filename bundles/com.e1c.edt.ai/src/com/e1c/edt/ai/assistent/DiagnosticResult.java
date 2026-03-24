/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.e1c.edt.ai.ServiceState;

/**
 * Represents possible results of diagnostic execution. Diagnostic results consists of severity, message, remidiation if result is error, facts if needed,
 * @author Bogdan Sushkov
 *
 */
public class DiagnosticResult
{
    private final DiagnosticSeverity severity;
    private final String message;
    private final ServiceState knownProblem;
    private final Map<String, String> facts;
    private final String log;
    private final Throwable technical;

    private DiagnosticResult(DiagnosticSeverity severity, String message, ServiceState knownProblem,
        Map<String, String> facts,
        Throwable technical, String log)
    {
        this.severity = severity;
        this.message = message;
        this.knownProblem = Objects.requireNonNullElse(knownProblem, ServiceState.NONE);
        this.facts = facts == null ? Collections.emptyMap() : Collections.unmodifiableMap(facts);
        this.log = log == null ? "" : log; //$NON-NLS-1$
        this.technical = technical;
    }

    /**
     * Creates <code>ok</code>-diagnostic result with specified message.
     * @param message diagnostic message
     * @return ok diagnostic result
     */
    public static DiagnosticResult ok(String message)
    {
        return new DiagnosticResult(DiagnosticSeverity.OK, message, null, null, null, ""); //$NON-NLS-1$
    }

    /**
     * Creates <code>default</code>-diagnostic result. Represents an empty result and should be used as the result of a diagnostic check which will not be executed.
     * @return default diagnostic result
     */
    public static DiagnosticResult defaultResult()
    {
        return new DiagnosticResult(DiagnosticSeverity.DEFAULT, "", ServiceState.NONE, null, null, ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Creates <code>error</code>-diagnostic result with specified message and technical details.
     * @param message diagnostic message
     * @param remidiation steps to fix the issue
     * @param facts some facts about the issue, can be <code>null</code>
     * @param technical throwable with technical details, can be <code>null</code>
     * @return diagnostic result
     */
    public static DiagnosticResult error(String message, ServiceState knowmProblem, Map<String, String> facts,
        Throwable technical)
    {
        String log = technical != null ? technical.toString() : ""; //$NON-NLS-1$
        return new DiagnosticResult(DiagnosticSeverity.ERROR, message, knowmProblem, facts, technical, log);
    }

    /**
     * Creates <code>error</code>-diagnostic result with specified message, technical details and log.
     * @param message diagnostic message
     * @param remidiation steps to fix the issue
     * @param facts some facts about the issue, can be <code>null</code>
     * @param technical throwable with technical details, can be <code>null</code>
     * @param log technical log, can be <code>null</code>
     * @return error diagnostic result
     */
    public static DiagnosticResult error(String message, ServiceState knowmProblem, Map<String, String> facts,
        Throwable technical, String log)
    {
        return new DiagnosticResult(DiagnosticSeverity.ERROR, message, knowmProblem, facts, technical, log);
    }

    /**
     *
     * @return the severity
     */
    public DiagnosticSeverity getSeverity()
    {
        return severity;
    }

    /**
     * Human readable message for the result of the diagnostic.
     * @return the message
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Human readable remidiation for fixing error if any occures while diagnostic.
     * @return the remidiation
     */
    public ServiceState getRemidiation()
    {
        return knownProblem;
    }

    /**
     * Facts that can be used to build a diagnostic report.
     * @return the facts
     */
    public Map<String, String> getFacts()
    {
        return facts;
    }

    /**
     * @return the technical - throwable wich caused diagnostic error
     */
    public Throwable getTechnical()
    {
        return technical;
    }

    /**
     * @return the log
     */
    public String getLog()
    {
        return log;
    }

}
