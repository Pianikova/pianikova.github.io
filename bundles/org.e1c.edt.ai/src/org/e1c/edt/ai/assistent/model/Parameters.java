/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class Parameters
{
    @SerializedName("prefix_length")
    public Integer prefixLength;

    @SerializedName("suffix_length")
    public Integer suffixLength;

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

    @SerializedName("chat_url")
    public String chatUrl;

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

        if (params.chatUrl != null)
        {
            chatUrl = params.chatUrl;
        }

        return this;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(bestOf, decoderInputDetails, details, doSample, frequencyPenalty, maxNewTokens,
            repetitionPenalty, returnFullText, seed, stop, temperature, topK, topNTokens, topP, truncate, typicalP,
            watermark, tokenHealing, returnLine, trimStop, chatUrl);
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
        return Objects.equals(bestOf, other.bestOf) && Objects.equals(decoderInputDetails, other.decoderInputDetails)
            && Objects.equals(details, other.details) && Objects.equals(doSample, other.doSample)
            && Objects.equals(frequencyPenalty, other.frequencyPenalty)
            && Objects.equals(maxNewTokens, other.maxNewTokens)
            && Objects.equals(repetitionPenalty, other.repetitionPenalty) && returnFullText == other.returnFullText
            && Objects.equals(seed, other.seed) && Objects.equals(stop, other.stop)
            && Objects.equals(temperature, other.temperature) && Objects.equals(topK, other.topK)
            && Objects.equals(topNTokens, other.topNTokens) && Objects.equals(topP, other.topP)
            && Objects.equals(truncate, other.truncate) && Objects.equals(typicalP, other.typicalP)
            && Objects.equals(watermark, other.watermark) && Objects.equals(tokenHealing, other.tokenHealing)
            && Objects.equals(returnLine, other.returnLine) && Objects.equals(trimStop, other.trimStop)
            && Objects.equals(chatUrl, other.chatUrl);
    }
}