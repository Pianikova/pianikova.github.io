/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Minimal read-only {@link CompareEditorInput} for a two-way text compare of two in-memory
 * {@link ITypedElement}s (current vs proposed content of an {@code Edit} preview).
 */
public class DiffCompareEditorInput
    extends CompareEditorInput
{
    private final ITypedElement left;
    private final ITypedElement right;

    /**
     * @param left the left (current) side, never {@code null}
     * @param right the right (proposed) side, never {@code null}
     * @param configuration the compare configuration (labels, editability), never {@code null}
     */
    public DiffCompareEditorInput(ITypedElement left, ITypedElement right, CompareConfiguration configuration)
    {
        super(configuration);
        this.left = left;
        this.right = right;
    }

    @Override
    protected Object prepareInput(IProgressMonitor monitor)
    {
        return new DiffNode(left, right);
    }
}
