package com.liferpg.adventure.domain

import java.time.LocalDate

/**
 * Jalali (Persian / Solar Hijri) calendar helpers. Port of the well known jalaali algorithm.
 * Used for Persian date display, the Persian habit calendar and as the definition of a "month"
 * for monthly quests (so a month always matches what a Persian user sees on their calendar).
 */
object Jalali {
    data class Date(val year: Int, val month: Int, val day: Int)

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    val MONTH_NAMES_FA = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    val WEEKDAYS_SHORT_FA_SAT_FIRST = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    private class Cal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): Cal {
        require(jy >= BREAKS[0] && jy < BREAKS[BREAKS.size - 1]) { "Invalid Jalali year $jy" }
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        for (i in 1 until BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + ((n % 33) + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = ((n + 1) % 33 - 1) % 4
        if (leap == -1) leap = 4
        return Cal(leap, gy, march)
    }

    fun isLeap(jy: Int): Boolean = jalCal(jy).leap == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeap(jy)) 30 else 29
    }

    fun toEpochDay(jy: Int, jm: Int, jd: Int): Long {
        val c = jalCal(jy)
        val base = LocalDate.of(c.gy, 3, c.march).toEpochDay()
        return base + (jm - 1) * 31L - (jm / 7) * (jm - 7L) + jd - 1
    }

    fun fromEpochDay(epochDay: Long): Date {
        val gy = LocalDate.ofEpochDay(epochDay).year
        var jy = gy - 621
        val c = jalCal(jy)
        val first = LocalDate.of(c.gy, 3, c.march).toEpochDay()
        var k = (epochDay - first).toInt()
        if (k >= 0) {
            if (k <= 185) {
                return Date(jy, 1 + k / 31, k % 31 + 1)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (c.leap == 1) k += 1
        }
        return Date(jy, 7 + k / 30, k % 30 + 1)
    }

    fun format(epochDay: Long): String {
        val d = fromEpochDay(epochDay)
        return "${d.day} ${MONTH_NAMES_FA[d.month - 1]} ${d.year}"
    }
}
