/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;

import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.Images;

/**
 * Adds the {@code ai.png} marker to the {@code WORKMATE.md} and {@code SKILL.md} files in the
 * navigator so they are recognisable as AI configuration files.
 */
public class WorkmateFileDecorator
    extends LabelProvider
    implements ILightweightLabelDecorator
{
    private static final String WORKMATE_MD = "WORKMATE.md"; //$NON-NLS-1$
    private static final String SKILL_MD = "SKILL.md"; //$NON-NLS-1$

    @Override
    public void decorate(Object element, IDecoration decoration)
    {
        if (!(element instanceof IFile))
        {
            return;
        }
        if (BaseActivator.getDefault() == null)
        {
            return;
        }
        var name = ((IFile)element).getName();
        if (WORKMATE_MD.equals(name) || SKILL_MD.equals(name))
        {
            try
            {
                var descriptor = BaseActivator.getImageDescriptor(Images.AI);
                if (descriptor != null)
                {
                    decoration.addOverlay(descriptor, IDecoration.TOP_RIGHT);
                }
            }
            catch (Exception e)
            {
                // image registry not ready yet — skip the overlay
            }
        }
    }
}
