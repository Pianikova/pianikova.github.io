/**
 *
 */
package com.e1c.edt.ai.assistent;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.ToolInvokeRequest;
import com.e1c.edt.ai.assistent.model.ToolInvokeResponse;

public interface ITools
{
    IObservable<ToolInvokeResponse> createInvokeSource(ProjectId projectId, ToolInvokeRequest request,
        ICancellationToken cancellationToken);
}
