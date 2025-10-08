/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;

public class VisualContext
    extends VisualGroup
{
    public List<VisualGroup> groups;

    public boolean isEmpty()
    {
        return (groups == null || groups.isEmpty()) && (fields == null || fields.isEmpty());
    }
}
