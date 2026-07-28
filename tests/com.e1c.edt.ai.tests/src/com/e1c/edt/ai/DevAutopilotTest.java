/*
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai;

import org.junit.Assert;
import org.junit.Test;

public class DevAutopilotTest
{
    @Test
    public void shouldRecognizeMutationTools()
    {
        Assert.assertTrue(DevAutopilot.isMutationTool("jshell")); //$NON-NLS-1$
        Assert.assertTrue(DevAutopilot.isMutationTool("1c_editmetadata")); //$NON-NLS-1$
        Assert.assertFalse(DevAutopilot.isMutationTool("getmarkers")); //$NON-NLS-1$
        Assert.assertFalse(DevAutopilot.isMutationTool(null));
    }
}
