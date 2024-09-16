package org.e1c.edt.semantic;

import org.eclipse.ui.IStartup;

import com.google.inject.Inject;

public class Startup
    implements IStartup
{
    @Inject
    IEndpointViewModel endpointViewModel;

    public Startup()
    {
        Activator.injectMembers(this);
    }

    @Override
    public void earlyStartup()
    {
        endpointViewModel.restore();
    }
}
