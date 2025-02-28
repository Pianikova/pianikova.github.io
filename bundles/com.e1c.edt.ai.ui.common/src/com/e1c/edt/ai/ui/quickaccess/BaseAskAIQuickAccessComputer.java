/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.quickaccess;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.quickaccess.IQuickAccessComputer;
import org.eclipse.ui.quickaccess.IQuickAccessComputerExtension;
import org.eclipse.ui.quickaccess.QuickAccessElement;


/**
 * Generates a computed element based on user input. The element will appear
 * in the QuickAccess menu.
 *
 * @author Bogdan Sushkov
 */
public class BaseAskAIQuickAccessComputer
    implements IQuickAccessComputer, IQuickAccessComputerExtension
{

    @Override
    public QuickAccessElement[] computeElements()
    {
        return new QuickAccessElement[0];
    }

    @Override
    public void resetState()
    {
        // stateless, nothing to do
    }

    @Override
    public boolean needsRefresh()
    {
        return false;
    }

    @Override
    public QuickAccessElement[] computeElements(String query, IProgressMonitor monitor)
    {
        AskAIQuickAccessElement myElement = new AskAIQuickAccessElement(query);
        return new QuickAccessElement[] { myElement };
    }
}