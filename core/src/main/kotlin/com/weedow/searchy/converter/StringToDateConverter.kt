package com.weedow.searchy.converter

import com.weedow.searchy.utils.klogger
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.*

/**
 * A converter that converts from String to [Date].
 *
 * It uses the default JVM [Locale] and a default list of patterns defined by [DATE_FORMATS][StringToDateConverter.Companion.DATE_FORMATS].
 *
 * It is possible to modify the default behavior by instantiating the converter with a specific [Locale] and/or a specific patterns list.
 *
 * @param patterns list of patterns to be used while converting. Default is [DATE_FORMATS][StringToDateConverter.Companion.DATE_FORMATS]
 * @param locale [Locale] to be used while converting. Default is JVM [Locale]
 */
@ReadingConverter
class StringToDateConverter(
    private vararg val patterns: String = DATE_FORMATS,
    private val locale: Locale = Locale.getDefault(Locale.Category.FORMAT)
) : Converter<String, Date> {

    companion object {
        private val DATE_FORMATS = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX][Z]",
            "yyyy-MM-dd",
            "yyyyMMdd",
            "dd/MM/yyyy"
        )

        private val log by klogger()
    }

    private val dateTimeFormatter: DateTimeFormatter

    init {
        val joinedPatterns = patterns.joinToString("][", "[", "]")
        if (log.isDebugEnabled) log.debug("initializing DateTimeFormatter with the locale $locale and the following patterns: $joinedPatterns")

        dateTimeFormatter = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ofPattern(joinedPatterns, locale))
            .toFormatter(locale)
    }

    /**
     * Converts the given [String] to [Date].
     *
     * @param source String to be converted
     * @return the Date instance
     */
    override fun convert(source: String): Date {
        val temporalAccessor = dateTimeFormatter.parse(source)
        val localDateTime = try {
            val offsetDateTime = OffsetDateTime.from(temporalAccessor)
            offsetDateTime.toLocalDateTime()
        } catch (e: DateTimeException) {
            try {
                LocalDateTime.from(temporalAccessor)
            } catch (e: DateTimeException) {
                val localDate = LocalDate.from(temporalAccessor)
                localDate.atStartOfDay()
            }
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())
    }

}