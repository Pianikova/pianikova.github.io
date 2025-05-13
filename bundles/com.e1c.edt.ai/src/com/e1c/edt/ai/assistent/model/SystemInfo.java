/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * Информация о системе пользователя.
 */
public class SystemInfo
{
    /**
     * Имя операционной системы. Например, "Windows 11.
     */
    @SerializedName("os_name")
    public String osName;

    /**
     * Версия операционной системы. Например, "11.0".
     */
    @SerializedName("os_version")
    public String osVersion;

    /**
     * Архитектура операционной системы. Например, "amd64".
     */
    public String arch;

    /**
     * Количество процессоров. Например, "24".
     */
    @SerializedName("available_processors")
    public Integer availableProcessors;

    /**
     * Название процессора. Например, "AMD64 Family 25 Model 33 Stepping 2, AuthenticAMD".
     */
    @SerializedName("processor_name")
    public String processorName;

    /**
     * Объем оперативной памяти. Например, "34258628608".
     */
    @SerializedName("total_physical_memory_size")
    public Long totalPhysicalMemorySize;
}
