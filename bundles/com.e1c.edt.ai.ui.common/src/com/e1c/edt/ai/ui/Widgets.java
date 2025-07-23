/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Stack;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class Widgets implements IWidgets
{
    @Override
    public Stream<Control> getChildren(Composite target)
    {
        return StreamSupport
            .stream(Spliterators.spliteratorUnknownSize(new ControlIterator(target), Spliterator.IMMUTABLE),
            false);
    }

    private class ControlIterator
        implements Iterator<Control>
    {
        private final Stack<Control> controls = new Stack<>();

        public ControlIterator(Composite target)
        {
            controls.push(target);
        }

        @Override
        public boolean hasNext()
        {
            return !controls.isEmpty();
        }

        @Override
        public Control next()
        {
            var control = controls.pop();
            if (control instanceof Composite)
            {
                var nextComposite = (Composite)control;
                for (var child : nextComposite.getChildren())
                {
                    controls.push(child);
                }
            }

            return control;
        }
    }
}
