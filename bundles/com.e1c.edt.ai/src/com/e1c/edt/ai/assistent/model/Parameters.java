/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.e1c.edt.ai.IDefaultSettings;
import com.google.gson.annotations.SerializedName;

// {
/**
 * Параметры сессии.
 */
public class Parameters
{
// }
    public Parameters()
    {
        //
    }

    public Parameters(IDefaultSettings defaultSettings)
    {
        try
        {
            url = new URL(defaultSettings.getUrl());
            updateUrl = Optional.ofNullable(defaultSettings.getUpdateUrl());
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("Invalid default url."); //$NON-NLS-1$
        }
    }

// {
    /**
     * Максимальная длина префикса (токенов). Например, 2160.
     */
    @SerializedName("prefix_length")
    public Optional<Integer> prefixLength;

    /**
     * Максимальная длина суффикса (токенов). Например, 1080.
     */
    @SerializedName("suffix_length")
    public Optional<Integer> suffixLength;

    /**
     * Общая длина формы (символов). Например, 3240.
     */
    @SerializedName("form_length")
    public Optional<Integer> formLength;

    /**
     * Длина метаданных (символов). Например, 2160.
     */
    @SerializedName("meta_length")
    public Optional<Integer> metaLength;

    /**
     * Количество лучших результатов для выбора. Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("best_of")
    public Optional<Integer> bestOf;

    /**
     * Включать детали входного декодера. Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("decoder_input_details")
    public Optional<Boolean> decoderInputDetails;

    /**
     * Включать подробные логи. Нужно определить только для изменения стандартных настроек модели.
     */
    public Optional<Boolean> details;

    /**
     * Использовать выборку. Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("do_sample")
    public Optional<Boolean> doSample;

    /**
     * Максимальное количество новых генерируемых токенов. Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("max_new_tokens")
    public Optional<Integer> maxNewTokens;

    /**
     * Штраф за повторения (числовое значение). Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("repetition_penalty")
    public Optional<Double> repetitionPenalty;

    /**
     * Штраф за частотность слов (числовое значение). Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("frequency_penalty")
    public Optional<Double> frequencyPenalty;

    /**
     * Возвращать полный текст или только сгенерированную часть. Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("return_full_text")
    public Optional<Boolean> returnFullText;

    /**
     * Использовать фиксированное начальное значение для воспроизводимости. Нужно определить только для изменения стандартных настроек модели.
     */
    public Optional<Boolean> seed;

    /**
     * Список стоп-слов или фраз для остановки генерации. Нужно определить только для изменения стандартных настроек модели.
     */
    public List<String> stop = List.of();

    /**
     * Температура выборки (от 0 до 1). Нужно определить только для изменения стандартных настроек модели.
     */
    public Optional<Double> temperature;

    /**
     * Количество наиболее вероятных токенов для выборки. Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("top_k")
    public Optional<Integer> topK;

    /**
     * Количество токенов для выборки. Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("top_n_tokens")
    public Optional<Integer> topNTokens;

    /**
     * Параметр для выборочной генерации (от 0 до 1). Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("top_p")
    public Optional<Double> topP;

    /**
     * Включать усечение. Нужно определить только для изменения стандартных настроек модели.
     */
    public Optional<Boolean> truncate;

    /**
     * Параметр типичности (числовое значение). Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("typical_p")
    public Optional<Double> typicalP;

    /**
     * Включать водяные знаки. Нужно определить только для изменения стандартных настроек модели.
     */
    public Optional<Boolean> watermark;

    /**
     * Метод исправления токенов (None/guidance/streaming). Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("token_healing")
    public Optional<TokenHealing> tokenHealing;

    /**
     * Возвращать текст построчно. Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("return_line")
    public Optional<Boolean> returnLine;

    /**
     * Обрезать текст после стоп-слов. Нужно определить только для изменения стандартных настроек модели.
     */
    @SerializedName("trim_stop")
    public Optional<Boolean> trimStop;

    /**
     * URL для запросов. Например, "https://code.1c.ai/api/v1/".
     */
    @SerializedName("url")
    public URL url;

    /**
     * URL для чата. Например, "https://code.1c.ai/chat/".
     */
    @SerializedName("chat_url")
    public Optional<URL> chatUrl;

    /**
     * URL для обновлений. Например, "https://code.1c.ai/plugin/".
     */
    @SerializedName("update_url")
    public Optional<String> updateUrl;

    /**
     * Максимальная длина данных локальных функций. Например, 2590.
     */
    @SerializedName("local_functions_length")
    public Optional<Integer> localFunctionsLength;

    /**
     * Длина внешних функций. Например, 2160.
     */
    @SerializedName("external_functions_length")
    public Optional<Integer> externalFunctionsLength;

    /**
     * Длина метаданных (символов). Например, 2160.
     */
    @SerializedName("global_meta_length")
    public Optional<Integer> globalMetaLength;

    /**
     * Длина буфера обмена. Например, 2160
     */
    @SerializedName("clipboard_length")
    public Optional<Integer> clipboardLength;

    /**
     * Минимальная задержка миллисекунд. Например, 300.
     */
    @SerializedName("min_delay")
    public Optional<Integer> minDelay;

    /**
     * Время ожидания ответа миллисекунд. Например, 15000.
     */
    @SerializedName("timeout")
    public Optional<Integer> timeout;

    /**
     * Определяет передавать ли глобальный контекст. Например, true.
     */
    @SerializedName("global_context")
    public Optional<Boolean> globalContext;

    /**
     * Определяет передавать ли расширенный контекст. Например, true.
     */
    @SerializedName("extended_context")
    public Optional<Boolean> extendedContext;

    /**
     * Уровень детализации логов (error/warning/info/trace/debug). Например, warning.
     */
    public Verbosity verbosity;

    /**
     * Переопределяет пут к ресурсам. Например, "C:/Users/user/resources".
     */
    @SerializedName("resources")
    public Optional<String> resources;

    /**
     * Переопределяет размер контекста для git diff. Например, 16.
     */
    @SerializedName("git_diff_context_lines")
    public Optional<Integer> gitDiffContextLines;

    /**
     * Переопределяет тип экземпляра. Например, "A".
     */
    @SerializedName("instance_type")
    public Optional<String> instanceType;
    // }

    public transient boolean fromCache;

    public Parameters merge(Parameters params)
    {
        if (params == null)
        {
            return this;
        }

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

        if (params.globalMetaLength != null)
        {
            globalMetaLength = params.globalMetaLength;
        }

        if (params.clipboardLength != null)
        {
            clipboardLength = params.clipboardLength;
        }

        if (params.minDelay != null)
        {
            minDelay = params.minDelay;
        }

        if (params.timeout != null)
        {
            timeout = params.timeout;
        }

        if (params.globalContext != null)
        {
            globalContext = params.globalContext;
        }

        if (params.extendedContext != null)
        {
            extendedContext = params.extendedContext;
        }

        if (params.verbosity != null)
        {
            verbosity = params.verbosity;
        }

        if (params.instanceType != null)
        {
            instanceType = params.instanceType;
        }

        return this;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(prefixLength, suffixLength, formLength, metaLength, bestOf, decoderInputDetails, details,
            doSample, maxNewTokens, repetitionPenalty, frequencyPenalty, returnFullText, seed, stop, temperature, topK,
            topNTokens, topP, truncate, typicalP, watermark, tokenHealing, returnLine, trimStop, url, chatUrl,
            updateUrl, localFunctionsLength, externalFunctionsLength, globalMetaLength, clipboardLength, minDelay,
            timeout, globalContext, extendedContext, verbosity, resources, gitDiffContextLines, instanceType);
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
        return Objects.equals(prefixLength, other.prefixLength) && Objects.equals(suffixLength, other.suffixLength)
            && Objects.equals(formLength, other.formLength) && Objects.equals(metaLength, other.metaLength)
            && Objects.equals(bestOf, other.bestOf) && Objects.equals(decoderInputDetails, other.decoderInputDetails)
            && Objects.equals(details, other.details) && Objects.equals(doSample, other.doSample)
            && Objects.equals(maxNewTokens, other.maxNewTokens)
            && Objects.equals(repetitionPenalty, other.repetitionPenalty)
            && Objects.equals(frequencyPenalty, other.frequencyPenalty)
            && Objects.equals(returnFullText, other.returnFullText) && Objects.equals(seed, other.seed)
            && Objects.equals(stop, other.stop) && Objects.equals(temperature, other.temperature)
            && Objects.equals(topK, other.topK) && Objects.equals(topNTokens, other.topNTokens)
            && Objects.equals(topP, other.topP) && Objects.equals(truncate, other.truncate)
            && Objects.equals(typicalP, other.typicalP) && Objects.equals(watermark, other.watermark)
            && Objects.equals(tokenHealing, other.tokenHealing) && Objects.equals(returnLine, other.returnLine)
            && Objects.equals(trimStop, other.trimStop) && Objects.equals(url, other.url)
            && Objects.equals(chatUrl, other.chatUrl) && Objects.equals(updateUrl, other.updateUrl)
            && Objects.equals(localFunctionsLength, other.localFunctionsLength)
            && Objects.equals(externalFunctionsLength, other.externalFunctionsLength)
            && Objects.equals(globalMetaLength, other.globalMetaLength)
            && Objects.equals(clipboardLength, other.clipboardLength) && Objects.equals(minDelay, other.minDelay)
            && Objects.equals(timeout, other.timeout) && Objects.equals(globalContext, other.globalContext)
            && Objects.equals(extendedContext, other.extendedContext) && Objects.equals(verbosity, other.verbosity)
            && Objects.equals(resources, other.resources)
            && Objects.equals(gitDiffContextLines, other.gitDiffContextLines)
            && Objects.equals(instanceType, other.instanceType);
// {
    }
// }
}