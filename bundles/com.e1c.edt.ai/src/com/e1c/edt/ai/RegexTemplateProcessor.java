/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;

/**
 * Обрабатывает текстовые шаблоны с использованием регулярных выражений для поиска и замены плейсхолдеров.
 *
 * <p>Класс предоставляет функциональность для нахождения и замены плейсхолдеров в тексте.
 * Плейсхолдеры идентифицируются с помощью регулярного выражения.</p>
 *
 * @author Bogdan Sushkov
 */
public class RegexTemplateProcessor
{
    @Inject
    public RegexTemplateProcessor()
    {
    }

    /**
     * Находит все уникальные плейсхолдеры в тексте и возвращает их содержимое в порядке появления в тексте.
     * @see {@link Matcher#group(int)}
     *
     * @param text текст для поиска плейсхолдеров
     * @param pattern регулярное выражение для поиска
     * @return множество найденных плейсхолдеров
     */
    public Set<String> find(String text, Pattern pattern)
    {
        return find(text, pattern, 1);
    }

    /**
     * Находит все уникальные плейсхолдеры в тексте.
     *
     * @param text текст для поиска плейсхолдеров
     * @param pattern регулярное выражение для поиска
     * @param groupIndex индекс группы захвата в регулярном выражении
     * @return множество найденных плейсхолдеров
     */
    public Set<String> find(String text, Pattern pattern, int groupIndex)
    {
        var result = new LinkedHashSet<String>();
        var matcher = pattern.matcher(text);
        while (matcher.find())
        {
            result.add(matcher.group(groupIndex));
        }
        return result;
    }

    /**
     * Заменяет все вхождения, найденные по регулярному выражению, с использованием функции-поставщика значений.
     *
     * @param text текст для замены
     * @param pattern регулярное выражение для поиска
     * @param replacementProvider функция для получения значения замены на основе найденного совпадения
     * @return текст с замененными значениями
     */
    public String replace(String text, Pattern pattern,
        Function<MatchResult, String> replacementProvider)
    {
        var matcher = pattern.matcher(text);
        var result = new StringBuilder();
        while (matcher.find())
        {
            String replacement = replacementProvider.apply(matcher.toMatchResult());
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}

