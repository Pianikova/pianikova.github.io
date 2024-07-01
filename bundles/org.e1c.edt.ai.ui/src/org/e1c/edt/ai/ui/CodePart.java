/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.Range;
import org.eclipse.xtext.nodemodel.ILeafNode;

import com.google.common.base.Preconditions;

public class CodePart
{
    private final Range range;
    private final CodePartType type;
    private final Iterable<ILeafNode> nodes;

    public CodePart(Range range, CodePartType type, Iterable<ILeafNode> nodes)
    {
        Preconditions.checkNotNull(range);
        Preconditions.checkNotNull(nodes);
        this.range = range;
        this.type = type;
        this.nodes = nodes;
    }

    public Range getRange()
    {
        return range;
    }

    public CodePartType getType()
    {
        return type;
    }

    public String getCode()
    {
        var code = new StringBuilder();
        for (var node : nodes)
        {
            code.append(node.getText());
        }

        return code.toString();
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "CodePart [type=" + type + ", code=" + getCode() + "]";
    }
}
