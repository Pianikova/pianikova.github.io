/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Map;

import org.e1c.edt.ai.CodeCompletionAction;
import org.eclipse.swt.events.VerifyEvent;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class UserActions implements IUserActions
{
 // @formatter:off
    private static final Map<String, CodeCompletionAction> ACTION_MAP = Map.of(
        IHotKeys.SUGGEST, CodeCompletionAction.SUGGEST,
        IHotKeys.FINISH, CodeCompletionAction.FINISH,
        IHotKeys.ROLLBACK_PART, CodeCompletionAction.ROLLBACK_PART,
        IHotKeys.ACCEPT_LINE, CodeCompletionAction.ACCEPT_LINE,
        IHotKeys.ACCEPT_PART, CodeCompletionAction.ACCEPT_PART,
        IHotKeys.ACCEPT, CodeCompletionAction.ACCEPT);
 // @formatter:on

    private final IHotKeys hotKeys;

    @Inject
    public UserActions(IHotKeys hotKeys)
    {
        Preconditions.checkNotNull(hotKeys);
        this.hotKeys = hotKeys;
    }

    @Override
    public String getCodeCompletionLabels(char separator)
    {
        var labels = new StringBuilder();
        labels.append(hotKeys.getBinding(IHotKeys.ACCEPT_PART).getKeySequence().format());
        labels.append(separator);
        labels.append(hotKeys.getBinding(IHotKeys.ACCEPT_LINE).getKeySequence().format());
        labels.append(separator);
        labels.append(hotKeys.getBinding(IHotKeys.ACCEPT).getKeySequence().format());
        labels.append(separator);
        labels.append(hotKeys.getBinding(IHotKeys.ROLLBACK_PART).getKeySequence().format());
        labels.append(separator);
        labels.append(hotKeys.getBinding(IHotKeys.FINISH).getKeySequence().format());
        return labels.toString();
    }

    @Override
    public CodeCompletionAction getAction(VerifyEvent event)
    {
        Preconditions.checkNotNull(event);
        for (var action : ACTION_MAP.entrySet())
        {
            if (hotKeys.isTriggered(action.getKey(), event))
            {
                return action.getValue();
            }
        }

        return CodeCompletionAction.ACCEPT_CHAR;
    }
}
