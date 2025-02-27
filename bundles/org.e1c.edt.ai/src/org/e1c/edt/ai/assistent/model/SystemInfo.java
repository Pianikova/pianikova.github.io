/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class SystemInfo
{
    @SerializedName("os_name")
    public String osName;

    @SerializedName("os_version")
    public String osVersion;

    public String arch;

    @SerializedName("available_processors")
    public Integer availableProcessors;

    @SerializedName("processor_name")
    public String processorName;

    @SerializedName("total_physical_memory_size")
    public Long totalPhysicalMemorySize;
}
