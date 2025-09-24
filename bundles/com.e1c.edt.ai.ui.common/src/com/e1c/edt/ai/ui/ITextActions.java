/**
 * 
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.assistent.model.VisualContext;

public interface ITextActions
{
    IObservable<TextImprovements> ceateTextImprovementsSource(VisualContext context, TextAction action,
        ICancellationToken cancellationToken);
}
