/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Result of JShell code execution.
 */
public class JShellExecutionResult
{
    // Small/important fields first; the session id and guidance must survive truncation.
    // Note: this type intentionally does not extend SessionResult. Gson serializes subclass
    // fields before superclass fields, which would push these small fields below the large
    // std_out/std_err/error lists. Declaring them here keeps them at the front of the JSON.
    @SerializedName("repl_session_id")
    public String sessionId;

    @SerializedName("available_bindings")
    public List<String> availableBindings;

    @SerializedName("required_next_step")
    public String requiredNextStep;

    // Large fields last so they are dropped first if the response is truncated.
    @SerializedName("std_out")
    public String stdOut;

    @SerializedName("std_err")
    public String stdErr;

    @SerializedName("compilation_errors")
    public List<CompilationError> compilationErrors;

    @SerializedName("runtime_errors")
    public List<RuntimeError> runtimeErrors;

    @SerializedName("suggested_reflection_queries")
    public List<String> suggestedReflectionQueries;
}
