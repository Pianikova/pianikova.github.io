/**
 *
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IResourceChangeListener;

public interface IResourceListener
    extends IResourceChangeListener
{
    void initialize();
}
