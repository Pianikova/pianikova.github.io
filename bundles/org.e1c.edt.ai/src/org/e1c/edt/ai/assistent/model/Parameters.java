/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class Parameters
{
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
    public boolean returnFullText;

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

    @Override
    public int hashCode()
    {
        return Objects.hash(bestOf, decoderInputDetails, details, doSample, frequencyPenalty, maxNewTokens,
            repetitionPenalty, returnFullText, seed, stop, temperature, topK, topNTokens, topP, truncate, typicalP,
            watermark, tokenHealing, returnLine, trimStop);
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
            && Objects.equals(returnLine, other.returnLine) && Objects.equals(trimStop, other.trimStop);
    }
}