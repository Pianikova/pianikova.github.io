/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

/**
 * Reverts a single {@code Edit} tool invocation by applying the inverse text replacement
 * to the file's current content.
 * <p>
 * The implementation is fully stateless: every input it needs is already available in the
 * original {@code Edit} tool call recorded in the chat history. Subsequent edits to the same
 * file are tolerated as long as the post-edit fragment is still uniquely findable; otherwise
 * the rollback refuses without modifying the file, so intermediate work is never silently lost.
 */
public interface IEditRollback
{
    /**
     * Applies the inverse of an Edit operation: searches for {@code newContent} in the current
     * file and replaces it with {@code oldContent}, using the same matching rules as the original
     * Edit (single occurrence vs. all occurrences).
     *
     * @param path absolute file path (same as the original Edit call)
     * @param oldContent the fragment that was replaced; will be restored on success
     * @param newContent the fragment that replaced {@code oldContent}; must still be present in
     *            the current file (uniquely if {@code replaceAll} is {@code false})
     * @param replaceAll same semantics as in the Edit tool — invert all matches or just one
     * @return {@code true} on successful revert; {@code false} when arguments are invalid, the
     *         file is missing, or the post-edit fragment can no longer be located unambiguously
     */
    boolean rollback(String path, String oldContent, String newContent, boolean replaceAll);
}
