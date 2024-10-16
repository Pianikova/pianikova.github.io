/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import org.osgi.framework.Version;

import com.google.gson.annotations.SerializedName;

public class SessionRequest
{
    @SerializedName("service_parameters")
    public Parameters serviceParameters;
    @SerializedName("user_parameters")
    public UserParameters userParameters;

    public void setUserParameters(Version pluginVersion, String edtVersion)
    {

        if (edtVersion != null && pluginVersion != null)
        {
            this.userParameters = new UserParameters(edtVersion, pluginVersion.toString());
        }
    }

    static class UserParameters
    {
        @SerializedName("edt_version")
        public String edtVersion;
        @SerializedName("plugin_version")
        public String pluginVersion;

        public UserParameters(String edtVersion, String pluginVersion)
        {
            this.edtVersion = edtVersion;
            this.pluginVersion = pluginVersion;
        }
    }
}
