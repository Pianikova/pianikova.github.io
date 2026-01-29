/**
 *
 */
package com.e1c.edt.ai;

import org.eclipse.core.runtime.IProgressMonitor;

public interface ICancellationProgressMonitor
    extends IProgressMonitor
{
    public void setCancellationToken(ICancellationToken cancellationToken);
}
