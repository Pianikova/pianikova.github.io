/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.util.List;

/**
 * @author Nikolay Pyanikov
 *
 */
public interface IEntitiesWalker
{

    boolean walk(String path, List<Integer> span, IEntityVisitor visitor);

}
