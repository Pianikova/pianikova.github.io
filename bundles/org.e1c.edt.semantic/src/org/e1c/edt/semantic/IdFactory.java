/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.xtext.nodemodel.ICompositeNode;

public class IdFactory
    implements IIdFactory
{
    @Override
    @SuppressWarnings("nls")
    public String create(String path, ICompositeNode node) throws MalformedURLException
    {
        var requestPathUrl = new URL("file", "", -1, path);
        var start = node.getTotalOffset();
        var finish = node.getTotalEndOffset();
        return requestPathUrl.toString() + "?start=" + start + "&finish=" + finish;
    }
}
