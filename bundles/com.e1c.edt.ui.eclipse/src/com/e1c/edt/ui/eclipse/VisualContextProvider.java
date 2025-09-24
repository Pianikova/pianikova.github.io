/**
 *
 */
package com.e1c.edt.ui.eclipse;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IVisualContextProvider;
import com.e1c.edt.ai.assistent.model.VisualContext;

public class VisualContextProvider
    implements IVisualContextProvider
{
    @Override
    public VisualContext create(Object control, ICancellationToken cancellationToken)
    {
        return new VisualContext();
    }

}
