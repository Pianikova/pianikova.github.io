/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.navigator;

/**
 * Top-level "Workmate" node shown at the root of the tree (next to projects). Its children are the
 * user- and workspace-level scopes.
 */
public final class WorkmateRootNode
{
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof WorkmateRootNode;
    }

    @Override
    public int hashCode()
    {
        return WorkmateRootNode.class.hashCode();
    }
}
