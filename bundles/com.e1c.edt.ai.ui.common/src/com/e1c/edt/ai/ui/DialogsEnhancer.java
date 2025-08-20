/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.ArrayList;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;

public class DialogsEnhancer
    implements IDialogsInjector, IPartListener2
{
    private final IDispatcher dispatcher;
    private final ArrayList<IViewEnhancer> viewEnhancers = new ArrayList<>();

    @Inject
    public DialogsEnhancer(IDispatcher dispatcher, IStagingViewEnhancer stagingViewEnhancer)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(stagingViewEnhancer);
        this.dispatcher = dispatcher;
        viewEnhancers.add(stagingViewEnhancer);
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
        for (var viewEnhancer : viewEnhancers)
        {
            var id = partRef.getId();
            if (id == null || id.isBlank())
            {
                continue;
            }

            var viewPart = Suppliers.memoize(() -> partRef.getPart(false));
            viewEnhancer.getViewId().ifPresent(viewId -> {
                if (id.equals(viewId))
                {
                    viewEnhancer.setup(viewPart.get());
                }
            });
        }
    }
}
