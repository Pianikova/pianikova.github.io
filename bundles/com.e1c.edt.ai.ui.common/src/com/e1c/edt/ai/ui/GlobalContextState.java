/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Map;

/**
 * Persisted per-project global-context sync state. Mirrors the on-disk format described in the Context Manager
 * API: <code>{ "files": { "&lt;path&gt;": { "time": &lt;localTimeStamp&gt;, "hash": "MD5:..." } } }</code>.
 */
class GlobalContextState
{
    public Map<String, GlobalContextFileState> files;
}
