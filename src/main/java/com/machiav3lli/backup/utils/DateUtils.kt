/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2024  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.utils

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale

fun LocalDateTime.getFormattedDate(withTime: Boolean): String {
    val dtf = if (withTime) DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    else DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    return format(dtf)
}

const val ISO_LIKE_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"
const val ISO_LIKE_DATE_TIME_MIN_PATTERN = "yyyy-MM-dd HH:mm"
const val ISO_LIKE_DATE_TIME_MS_PATTERN = "yyyy-MM-dd HH:mm:ss:SSS"
const val FILE_DATE_TIME_MS_PATTERN = "yyyy-MM-dd-HH-mm-ss-SSS"
const val FILE_DATE_TIME_PATTERN = "yyyy-MM-dd-HH-mm-ss"
const val DATE_TIME_AS_VERSION_CODE_PATTERN = "yyMMddHH"

val ISO_DATE_TIME_FORMAT
    get() = SimpleDateFormat(
        ISO_LIKE_DATE_TIME_PATTERN,
        Locale.getDefault()
    )

val ISO_DATE_TIME_FORMAT_MIN
    get() = SimpleDateFormat(
        ISO_LIKE_DATE_TIME_MIN_PATTERN,
        Locale.getDefault()
    )

val ISO_DATE_TIME_FORMAT_MS
    get() = SimpleDateFormat(
        ISO_LIKE_DATE_TIME_MS_PATTERN,
        Locale.getDefault()
    )

// must be ISO time format for sane sorting yyyy, MM, dd, ...
// and only allowed file name characters (on all systems, Windows has the smallest set)
// not used any more, because we don't create old format
// and detection handles millisec as optional
//val BACKUP_DATE_TIME_FORMATTER_OLD = DateTimeFormatter.ofPattern(FILE_DATE_TIME_PATTERN)

// use millisec, because computers (and users) can be faster than a sec
val BACKUP_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(FILE_DATE_TIME_MS_PATTERN)
val BACKUP_DATE_TIME_SHOW_FORMATTER = DateTimeFormatter.ofPattern(ISO_LIKE_DATE_TIME_PATTERN)
val DATE_TIME_AS_VERSION_CODE_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_AS_VERSION_CODE_PATTERN)

val currentTimestamp: String
    get() {
        val simpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
        return simpleDateFormat.format(Date())
    }