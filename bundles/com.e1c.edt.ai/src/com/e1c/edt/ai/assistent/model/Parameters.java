/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import org.osgi.framework.Version;

import com.e1c.edt.ai.IDefaultSettings;
import com.google.gson.annotations.SerializedName;

public class Parameters
{
    public Parameters(IDefaultSettings defaultSettings)
    {
        try
        {
            url = new URL(defaultSettings.getUrl());
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("Invalid default url."); //$NON-NLS-1$
        }
    }

    @SerializedName("prefix_length")
    public Integer prefixLength;

    @SerializedName("suffix_length")
    public Integer suffixLength;

    @SerializedName("form_length")
    public Integer formLength;

    @SerializedName("meta_length")
    public Integer metaLength;

    @SerializedName("best_of")
    public Integer bestOf;

    @SerializedName("decoder_input_details")
    public Boolean decoderInputDetails;

    public Boolean details;

    @SerializedName("do_sample")
    public Boolean doSample;

    @SerializedName("max_new_tokens")
    public Integer maxNewTokens;

    @SerializedName("repetition_penalty")
    public Double repetitionPenalty;

    @SerializedName("frequency_penalty")
    public Double frequencyPenalty;

    @SerializedName("return_full_text")
    public Boolean returnFullText;

    public Boolean seed;

    public List<String> stop = List.of();

    public Double temperature;

    @SerializedName("top_k")
    public Integer topK;

    @SerializedName("top_n_tokens")
    public Integer topNTokens;

    @SerializedName("top_p")
    public Double topP;

    public Boolean truncate;

    @SerializedName("typical_p")
    public Double typicalP;

    public Boolean watermark;

    @SerializedName("token_healing")
    public TokenHealing tokenHealing;

    @SerializedName("return_line")
    public Boolean returnLine;

    @SerializedName("trim_stop")
    public Boolean trimStop;

    @SerializedName("url")
    public URL url;

    @SerializedName("chat_url")
    public URL chatUrl;

    @SerializedName("local_functions_length")
    public Integer localFunctionsLength;

    @SerializedName("external_functions_length")
    public Integer externalFunctionsLength;

    @SerializedName("min_delay")
    public Integer minDelay = 300;

    @SerializedName("timeout")
    public Integer timeout = 15000;

    @SerializedName("global_context")
    public Boolean globalСontext = false;

    @SerializedName("extended_context")
    public Boolean extendedСontext = false;

    @SerializedName("trace")
    public Boolean trace = false;

    public Verbosity verbosity = Verbosity.DEFAULT;

    @SerializedName("script_language")
    public String scriptLanguage = ""; //$NON-NLS-1$

    @SerializedName("configuration_name")
    public String configurationName = ""; //$NON-NLS-1$

    @SerializedName("version")
    public Version version = Version.emptyVersion;

    @SerializedName("vendor")
    public String vendor = ""; //$NON-NLS-1$

    public Parameters merge(Parameters params)
    {
        if (params.prefixLength != null)
        {
            prefixLength = params.prefixLength;
        }

        if (params.suffixLength != null)
        {
            suffixLength = params.suffixLength;
        }

        if (params.formLength != null)
        {
            formLength = params.formLength;
        }

        if (params.metaLength != null)
        {
            metaLength = params.metaLength;
        }

        if (params.bestOf != null)
        {
            bestOf = params.bestOf;
        }

        if (params.bestOf != null)
        {
            bestOf = params.bestOf;
        }

        if (params.decoderInputDetails != null)
        {
            decoderInputDetails = params.decoderInputDetails;
        }

        if (params.details != null)
        {
            details = params.details;
        }

        if (params.doSample != null)
        {
            doSample = params.doSample;
        }

        if (params.maxNewTokens != null)
        {
            maxNewTokens = params.maxNewTokens;
        }

        if (params.repetitionPenalty != null)
        {
            repetitionPenalty = params.repetitionPenalty;
        }

        if (params.frequencyPenalty != null)
        {
            frequencyPenalty = params.frequencyPenalty;
        }

        if (params.returnFullText != null)
        {
            returnFullText = params.returnFullText;
        }

        if (params.seed != null)
        {
            seed = params.seed;
        }

        if (params.stop != null && params.stop.size() > 0)
        {
            stop = params.stop;
        }

        if (params.temperature != null)
        {
            temperature = params.temperature;
        }

        if (params.topK != null)
        {
            topK = params.topK;
        }

        if (params.topNTokens != null)
        {
            topNTokens = params.topNTokens;
        }

        if (params.topP != null)
        {
            topP = params.topP;
        }

        if (params.truncate != null)
        {
            truncate = params.truncate;
        }

        if (params.typicalP != null)
        {
            typicalP = params.typicalP;
        }

        if (params.watermark != null)
        {
            watermark = params.watermark;
        }

        if (params.tokenHealing != null)
        {
            tokenHealing = params.tokenHealing;
        }

        if (params.returnLine != null)
        {
            returnLine = params.returnLine;
        }

        if (params.trimStop != null)
        {
            trimStop = params.trimStop;
        }

        if (params.url != null)
        {
            url = params.url;
        }

        if (params.chatUrl != null)
        {
            chatUrl = params.chatUrl;
        }

        if (params.localFunctionsLength != null)
        {
            localFunctionsLength = params.localFunctionsLength;
        }

        if (params.externalFunctionsLength != null)
        {
            externalFunctionsLength = params.externalFunctionsLength;
        }

        if (params.minDelay != null)
        {
            minDelay = params.minDelay;
        }

        if (params.timeout != null)
        {
            timeout = params.timeout;
        }

        if (params.globalСontext != null)
        {
            globalСontext = params.globalСontext;
        }

        if (params.extendedСontext != null)
        {
            extendedСontext = params.extendedСontext;
        }

        if (params.trace != null)
        {
            trace = params.trace;
        }

        if (params.verbosity != null)
        {
            verbosity = params.verbosity;
        }

        if (params.scriptLanguage != null)
        {
            scriptLanguage = params.scriptLanguage;
        }

        if (params.configurationName != null)
        {
            configurationName = params.configurationName;
        }

        if (params.version != null)
        {
            version = params.version;
        }

        if (params.vendor != null)
        {
            vendor = params.vendor;
        }

        return this;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(bestOf, url, chatUrl, decoderInputDetails, details, doSample, formLength, frequencyPenalty,
            maxNewTokens, metaLength, prefixLength, repetitionPenalty, returnFullText, returnLine, seed, stop,
            suffixLength, temperature, tokenHealing, topK, topNTokens, topP, trimStop, truncate, typicalP, watermark,
            localFunctionsLength, externalFunctionsLength, minDelay, timeout, globalСontext, extendedСontext, trace,
            verbosity, scriptLanguage, configurationName, version, vendor);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Parameters other = (Parameters)obj;
        return Objects.equals(bestOf, other.bestOf) && Objects.equals(url, other.url)
            && Objects.equals(chatUrl, other.chatUrl)
            && Objects.equals(decoderInputDetails, other.decoderInputDetails) && Objects.equals(details, other.details)
            && Objects.equals(doSample, other.doSample) && Objects.equals(formLength, other.formLength)
            && Objects.equals(frequencyPenalty, other.frequencyPenalty)
            && Objects.equals(maxNewTokens, other.maxNewTokens) && Objects.equals(metaLength, other.metaLength)
            && Objects.equals(prefixLength, other.prefixLength)
            && Objects.equals(repetitionPenalty, other.repetitionPenalty)
            && Objects.equals(returnFullText, other.returnFullText) && Objects.equals(returnLine, other.returnLine)
            && Objects.equals(seed, other.seed) && Objects.equals(stop, other.stop)
            && Objects.equals(suffixLength, other.suffixLength) && Objects.equals(temperature, other.temperature)
            && Objects.equals(tokenHealing, other.tokenHealing) && Objects.equals(topK, other.topK)
            && Objects.equals(topNTokens, other.topNTokens) && Objects.equals(topP, other.topP)
            && Objects.equals(trimStop, other.trimStop) && Objects.equals(truncate, other.truncate)
            && Objects.equals(typicalP, other.typicalP) && Objects.equals(watermark, other.watermark)
            && Objects.equals(localFunctionsLength, other.localFunctionsLength)
            && Objects.equals(externalFunctionsLength, other.externalFunctionsLength)
            && Objects.equals(minDelay, other.minDelay) && Objects.equals(timeout, other.timeout)
            && Objects.equals(globalСontext, other.globalСontext)
            && Objects.equals(extendedСontext, other.extendedСontext) && Objects.equals(trace, other.trace)
            && Objects.equals(verbosity, other.verbosity) && Objects.equals(scriptLanguage, other.scriptLanguage)
            && Objects.equals(configurationName, other.configurationName) && Objects.equals(version, other.version)
            && Objects.equals(vendor, other.vendor);
    }
}