/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

/**
 * @author Nikolay Pyanikov
 *
 */
public interface IProgramingLanguage
{

    Optional<String> getFromPath(String filePath);

}
