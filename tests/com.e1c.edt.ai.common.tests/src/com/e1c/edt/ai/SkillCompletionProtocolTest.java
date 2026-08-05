/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.e1c.edt.ai.assistent.model.SkillCompletionPolicy;

/**
 * Tests for the declarative skill completion protocol.
 */
@SuppressWarnings("nls")
public class SkillCompletionProtocolTest
{
    @Test
    public void appendsMarkerInstructionOnlyForOptedInSkills()
    {
        var policy = new SkillCompletionPolicy("#END#", true);

        var prompt = ConversationFacade.withCompletionProtocol("Do the task", policy);

        assertTrue(prompt.startsWith("Do the task\n\n"));
        assertTrue(prompt.contains("отдельной последней строкой `#END#`"));
        assertEquals("Do the task", ConversationFacade.withCompletionProtocol("Do the task", null));
    }

    @Test
    public void stripsMarkerOnlyFromFinalLine()
    {
        assertEquals("Commit message", ConversationFacade.stripCompletionMarker(
            "Commit message\r\n#END#\r\n", "#END#"));
        assertNull(ConversationFacade.stripCompletionMarker("Commit message", "#END#"));
        assertNull(ConversationFacade.stripCompletionMarker("Commit #END#", "#END#"));
        assertNull(ConversationFacade.stripCompletionMarker("#END#\nMore text", "#END#"));
    }

}
