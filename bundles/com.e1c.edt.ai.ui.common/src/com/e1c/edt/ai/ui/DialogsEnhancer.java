/**
 *
 */
package com.e1c.edt.ai.ui;

import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@SuppressWarnings("restriction")
public class DialogsEnhancer
    implements IDialogsInjector, IPartListener2
{
    private final IDispatcher dispatcher;
    private final IStagingViewEnhancer stagingViewEnhancer;

    @Inject
    public DialogsEnhancer(IDispatcher dispatcher, IStagingViewEnhancer stagingViewEnhancer)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(stagingViewEnhancer);
        this.dispatcher = dispatcher;
        this.stagingViewEnhancer = stagingViewEnhancer;
    }

    @Override
    public void initialize()
    {
        dispatcher.dispatchAsync(() -> {
            var workbench = PlatformUI.getWorkbench();
            dispatcher.dispatchAsync(() -> {
                var window = workbench.getActiveWorkbenchWindow();
                if (window == null)
                {
                    return;
                }

                var page = window.getActivePage();
                if (page == null)
                {
                    return;
                }

                for (var veiwRef : page.getViewReferences())
                {
                    setup(veiwRef);
                }

                page.addPartListener(this);
            });
        });
    }

    @Override
    public void partOpened(IWorkbenchPartReference partRef)
    {
        setup(partRef);
    }

    private void setup(IWorkbenchPartReference partRef)
    {
        if (stagingViewEnhancer.getViewId().equalsIgnoreCase(partRef.getId()))
        {
            var viewPart = partRef.getPart(false);
            if (viewPart instanceof StagingView)
            {
                stagingViewEnhancer.setup((StagingView)viewPart);
            }

            return;
        }
    }
}
