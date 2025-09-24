/**
 *
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.VisualContext;

public interface IVisualContextProvider
{
    VisualContext create(Object control, ICancellationToken cancellationToken);
}
