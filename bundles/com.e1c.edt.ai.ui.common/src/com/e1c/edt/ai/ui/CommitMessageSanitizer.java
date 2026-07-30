/*
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Strips everything that is not the commit message from the model answer of the {@code git-commit}
 * skill.
 * <p>
 * The answer is written into the commit message field verbatim, so a preamble ("Now I have enough
 * information…", a list of the changed files, a character count of the summary) or a Markdown code
 * fence around the message lands in the commit as is. The skill forbids both, but a prompt rule is
 * a probability, not a guarantee — this is the guarantee.
 * <p>
 * The mandated message layout gives a reliable anchor: a summary line, an empty line, then numbered
 * {@code N) <type> in <area>: …} sections. Everything above the summary is dropped. When no
 * numbered section is found the layout is unknown, so the text is left alone apart from fences:
 * mangling an unrecognized answer is worse than passing it through.
 *
 * @author Skill Test
 */
public final class CommitMessageSanitizer
{
    /** Start of a details section — the anchor the summary is found by. */
    private static final Pattern DETAILS_LINE = Pattern.compile("^\\s*\\d+\\)\\s+\\S"); //$NON-NLS-1$

    /** An opening or closing Markdown fence, with or without a language tag. */
    private static final Pattern FENCE_LINE = Pattern.compile("^\\s*(`{3,}|~{3,})\\s*\\w*\\s*$"); //$NON-NLS-1$

    /**
     * Returns the commit message contained in {@code answer}, without a preamble or fencing.
     *
     * @param answer raw text of the model answer, may be {@code null}
     * @return the commit message, or {@code answer} itself when it is {@code null}, blank or has no
     * recognizable message layout
     */
    public static String sanitize(String answer)
    {
        if (answer == null || answer.isBlank())
        {
            return answer;
        }

        var lines = List.of(answer.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        var body = unwrapFence(lines);
        var summary = findSummaryLine(body);
        var message = String.join("\n", summary < 0 ? body : body.subList(summary, body.size())).trim(); //$NON-NLS-1$

        // Never hand back less than nothing: if the cleanup ate the whole answer, the layout was not
        // what we assumed, and the raw answer is still more useful than an empty commit message.
        return message.isBlank() ? answer : message;
    }

    /**
     * Returns the content of the fenced block that holds the message, or all lines with stray fence
     * markers removed when the message is not fenced.
     */
    private static List<String> unwrapFence(List<String> lines)
    {
        var fences = new ArrayList<Integer>();
        for (var i = 0; i < lines.size(); i++)
        {
            if (FENCE_LINE.matcher(lines.get(i)).matches())
            {
                fences.add(Integer.valueOf(i));
            }
        }

        for (var i = 0; i + 1 < fences.size(); i += 2)
        {
            var from = fences.get(i).intValue() + 1;
            var to = fences.get(i + 1).intValue();
            var block = lines.subList(from, to);
            if (block.stream().anyMatch(line -> DETAILS_LINE.matcher(line).find()))
            {
                return block;
            }
        }

        if (fences.isEmpty())
        {
            return lines;
        }

        var withoutFences = new ArrayList<String>(lines.size());
        for (var line : lines)
        {
            if (!FENCE_LINE.matcher(line).matches())
            {
                withoutFences.add(line);
            }
        }

        return withoutFences;
    }

    /**
     * Returns the index of the summary line — the nearest non-blank line above the first details
     * section — or {@code -1} when there is no details section to anchor on.
     */
    private static int findSummaryLine(List<String> lines)
    {
        var details = -1;
        for (var i = 0; i < lines.size(); i++)
        {
            if (DETAILS_LINE.matcher(lines.get(i)).find())
            {
                details = i;
                break;
            }
        }

        if (details < 0)
        {
            return -1;
        }

        for (var i = details - 1; i >= 0; i--)
        {
            if (!lines.get(i).isBlank())
            {
                return i;
            }
        }

        // Details with no summary above them: keep what there is, do not invent a summary.
        return details;
    }

    private CommitMessageSanitizer()
    {
        // Utility class
    }
}
