/**
 *
 */
package com.e1c.edt.ai.tools;

import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.ICancellationToken;

public interface ICancellationProgressMonitor
    extends IProgressMonitor
{
    public void setCancellationToken(ICancellationToken cancellationToken);
}
