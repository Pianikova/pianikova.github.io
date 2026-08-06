/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;

import com.e1c.edt.ai.ToolException;

/**
 * Catches a defect class that neither of this tool's existing checks sees: a mandatory single-valued
 * containment left unset on an object this tool just built by hand.
 * <p>
 * 1C's own marker checker ({@code MdValidationChecker}, surfaced through {@code GetMarkers}) validates
 * 1C business rules, not raw EMF structure, so it reports zero markers for, say, a {@code Form} missing
 * its mandatory {@code commandInterface}. EDT's own generators always populate that reference, so no
 * code path there has ever needed to guard against its absence; the first null-check-free reader
 * ({@code FormCommandInterfaceMapping}) crashes with a raw {@code NullPointerException} the first time a
 * human opens such a form.
 * <p>
 * This is deliberately a whitelist of specific, reproduced crashes rather than a blanket scan of every
 * EMF-required ({@code [1]}) containment: this metamodel marks many single-valued containments that way
 * (for example {@code Visible.userVisible}, inherited by nearly every form item) yet real, valid,
 * EDT-generated forms routinely leave them unset. An earlier version of this check walked the whole
 * containment tree flagging any unset {@code [1]} reference and broke {@code addFormAttribute} on an
 * ordinary, already-valid production form with false positives about {@code ExtendedTooltip.userVisible}
 * and {@code ContextMenu.userVisible} — cardinality alone cannot tell a real defect from this metamodel's
 * normal usage. Add an entry here only after reproducing a concrete crash caused by the feature being
 * unset, never from reading the {@code .xcore} cardinality alone.
 */
final class MetadataStructuralValidator
{
    private static final Map<String, Set<String>> REQUIRED_WHEN_UNSET =
        Map.of("Form", Set.of("commandInterface")); //$NON-NLS-1$ //$NON-NLS-2$

    private MetadataStructuralValidator()
    {
    }

    /**
     * Call at the end of any construction path that assembles a model object by hand instead of going
     * through an EDT generator or wizard, right before the surrounding transaction would otherwise
     * commit: it is cheaper to reject the mutation here than to let a structurally broken resource reach
     * disk and crash whichever EDT UI opens it first.
     *
     * @param root the object this tool just created or changed
     */
    static void requireSound(EObject root)
    {
        var required = REQUIRED_WHEN_UNSET.get(root.eClass().getName());
        if (required == null)
        {
            return;
        }
        var missing = new ArrayList<String>();
        for (var name : required)
        {
            var feature = root.eClass().getEStructuralFeature(name);
            if (feature != null && !root.eIsSet(feature))
            {
                missing.add(root.eClass().getName() + "." + name); //$NON-NLS-1$
            }
        }
        if (!missing.isEmpty())
        {
            throw new ToolException("This operation left " + root.eClass().getName() //$NON-NLS-1$
                + " structurally invalid: " + String.join(", ", missing) //$NON-NLS-1$ //$NON-NLS-2$
                + " must be set. This is a bug in the tool itself, not something to work around by editing" //$NON-NLS-1$
                + " the resource directly or retrying with different parameters."); //$NON-NLS-1$
        }
    }
}
