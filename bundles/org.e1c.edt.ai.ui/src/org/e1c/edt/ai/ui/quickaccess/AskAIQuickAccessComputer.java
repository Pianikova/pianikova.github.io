/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.quickaccess;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.quickaccess.IQuickAccessComputerExtension;
import org.eclipse.ui.quickaccess.QuickAccessElement;


/**
 * Generates a computed element based on user input. The element will appear
 * in the QuickAccess menu.
 *
 * @author Bogdan Sushkov
 */
public class AskAIQuickAccessComputer
    implements IQuickAccessComputerExtension
{
    private AskAIQuickAccessElement myElement;

    public AskAIQuickAccessComputer()
    {
        myElement = new AskAIQuickAccessElement();
    }

    @Override
    public QuickAccessElement[] computeElements()
    {
        AskAIQuickAccessElement myElement = new AskAIQuickAccessElement();
        return new QuickAccessElement[] { myElement };
    }

    @Override
    public void resetState()
    {
        // Empty method
    }

    @Override
    public boolean needsRefresh()
    {
        return true;
    }

    @Override
    public QuickAccessElement[] computeElements(String query, IProgressMonitor monitor)
    {
        myElement.setText(query);
        return new QuickAccessElement[] { myElement };
    }
}