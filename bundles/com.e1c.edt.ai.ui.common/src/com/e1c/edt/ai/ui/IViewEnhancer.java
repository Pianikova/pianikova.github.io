/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.ui.IWorkbenchPart;

public interface IViewEnhancer
{
    Optional<String> getViewId();

    void setup(IWorkbenchPart view);
}
