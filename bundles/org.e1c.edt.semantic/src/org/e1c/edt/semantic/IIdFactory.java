/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.net.MalformedURLException;

import org.eclipse.xtext.nodemodel.ICompositeNode;

public interface IIdFactory
{
    String create(String path, ICompositeNode node) throws MalformedURLException;
}
