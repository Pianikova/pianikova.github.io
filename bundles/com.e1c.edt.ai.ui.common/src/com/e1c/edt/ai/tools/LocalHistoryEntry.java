/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;

class LocalHistoryEntry
{
	@SerializedName("index")
	public Integer index;

	@SerializedName("revision_id")
	public String revisionId;

	@SerializedName("timestamp")
	public long timestamp;

	@SerializedName("formatted_time")
	public String formattedTime;

	@SerializedName("file_size")
	public long fileSize;

	@SerializedName("location")
	public String location;

	@SerializedName("is_current")
	public boolean isCurrent;

	@SerializedName("is_oldest")
	public boolean isOldest;
}
