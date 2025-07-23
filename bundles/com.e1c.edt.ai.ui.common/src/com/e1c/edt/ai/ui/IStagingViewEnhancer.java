/**
 *
 */
package com.e1c.edt.ai.ui;

import org.eclipse.egit.ui.internal.staging.StagingView;

@SuppressWarnings("restriction")
public interface IStagingViewEnhancer
{
    String getViewId();

    void setup(StagingView stagingView);
}
