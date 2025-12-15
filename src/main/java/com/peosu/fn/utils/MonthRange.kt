package com.peosu.fn.utils

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MonthRange(val pattern: Regex) {
    MonthYearToMonthYear("([a-z]+)\\W*(\\d{4})\\W+([a-z]+)\\W(\\d{4})".toRegex(RegexOption.IGNORE_CASE)),
    MonthYearRange("([a-z]+)\\W+(\\d+)\\W+(\\d{4})".toRegex(RegexOption.IGNORE_CASE)),
    MonthRangeYear("([a-z]+)\\W+([a-z]+)\\W+(\\d{4})".toRegex(RegexOption.IGNORE_CASE)),
    SingleMonthYear("([a-z]+)\\W*(\\d{4})".toRegex(RegexOption.IGNORE_CASE));

    companion object {
        fun parse(dateRange: String): List<LocalDate>? {
            return fromString(dateRange)?.parseDates(dateRange)
        }

        private fun fromString(input: String): MonthRange? {
            return entries.firstOrNull { it.pattern.containsMatchIn(input) }
        }
    }
    private fun parseDate(month:String, year:String): LocalDate{
        return DateTimeFormatter.ofPattern("MMM yyyy", Locale.US).let{ formatter->
                YearMonth.parse("${ucWords(month.take(3))} $year",formatter).atDay(1)
        }
    }
    private fun getDates(data:List<String>, rangeType: MonthRange): List<LocalDate> {
        return when(rangeType){
            MonthYearToMonthYear -> data.let {(month1,year1,month2,year2)  ->
                generateMonths(parseDate(month1,year1), parseDate(month2,year2))}

            MonthYearRange -> data.let {(month,year1,year2)  ->
                generateMonths(parseDate(month,year1), parseDate(month,year2))}

            MonthRangeYear -> data.let{ (month1,month2, year) ->
                generateMonths(parseDate(month1,year), parseDate(month2,year))}

            SingleMonthYear -> data.let { (month, year) ->
                listOf(parseDate(month,year)) }
        }
    }


    private fun ucWords(string:String): String =
        string.lowercase().split("\\s+".toRegex()).joinToString(" ") {s->
            s.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }


    private fun generateMonths(start:LocalDate, end:LocalDate): List<LocalDate> {
        return generateSequence (start){month-> month.plusMonths(1) }
            .takeWhile { it<=end }.toList()
    }

    private fun parseDates(monthRange: String): List<LocalDate>? {
        return this.pattern.find(monthRange)?.destructured?.toList()?.let { getDates(it, this) }
    }
}
