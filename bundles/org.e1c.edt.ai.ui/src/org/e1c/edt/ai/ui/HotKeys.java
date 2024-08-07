/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

import com.google.common.base.Preconditions;

public class HotKeys
    implements IHotKeys
{
    private final HashMap<String, KeyBinding> _keyBindigs = new HashMap<>();

    @Override
    public boolean isTriggered(String bindingId, KeyEvent event)
    {
        Preconditions.checkNotNull(bindingId);
        Preconditions.checkNotNull(event);

        ensureBindingsExists();
        var binding = _keyBindigs.get(bindingId);
        Preconditions.checkArgument(binding != null, "Cannot find binding " + bindingId); //$NON-NLS-1$

        var bindingKeyStrokes = binding.getKeySequence().getKeyStrokes();
        if (bindingKeyStrokes == null)
        {
            return false;
        }

        var eventKeyStrokes = generatePossibleKeyStrokes(event);
        if (eventKeyStrokes == null)
        {
            return false;
        }

        return Arrays.asList(bindingKeyStrokes).equals(eventKeyStrokes);
    }

    @Override
    public KeyBinding getBinding(String bindingId)
    {
        var binding = _keyBindigs.get(bindingId);
        Preconditions.checkArgument(binding != null, "Cannot find binding " + bindingId); //$NON-NLS-1$
        return binding;
    }

    private static List<KeyStroke> generatePossibleKeyStrokes(KeyEvent event)
    {
        var keyStrokes = new ArrayList<KeyStroke>(3);
        if ((event.stateMask == 0) && (event.keyCode == 0) && (event.character == 0))
        {
            return keyStrokes;
        }

        var firstAccelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(event);
        keyStrokes.add(SWTKeySupport.convertAcceleratorToKeyStroke(firstAccelerator));
        return keyStrokes;
    }

    private void ensureBindingsExists()
    {
        synchronized (_keyBindigs)
        {
            if (_keyBindigs.size() > 0)
            {
                return;
            }

            var bindingService = PlatformUI.getWorkbench().getAdapter(IBindingService.class);
            for (var binding : bindingService.getBindings())
            {
                if (!(binding instanceof KeyBinding))
                {
                    continue;
                }

                var command = binding.getParameterizedCommand();
                if (command == null)
                {
                    continue;
                }

                var id = command.getId();
                if (id == null || !id.startsWith(PREFIX))
                {
                    continue;
                }

                _keyBindigs.put(id, (KeyBinding)binding);
            }
        }
    }
}
