package com.machiav3lli.backup.data.dbs

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {

    @TypeConverter
    fun toStringSet(string: String?): Set<String> = when (string) {
        null, "" -> emptySet()
        else     -> string
            .split(",")
            .filter(String::isNotEmpty)
            .toHashSet()
    }


    @JvmName("stringSetToString")
    @TypeConverter
    fun toString(set: Set<String>): String =
        if (set.isNotEmpty()) set.joinToString(",")
        else ""

    @TypeConverter
    fun toStringList(string: String?): List<String> = when (string) {
        null, "", "[]" -> emptyList()
        else           -> string.removeSurrounding("[", "]")
            .split(",")
            .filter(String::isNotEmpty)
    }

    @TypeConverter
    fun toString(list: List<String>): String = list.toString()

    @TypeConverter
    fun toStringArray(string: String?): Array<String> = when (string) {
        null, "" -> emptyArray()
        else     -> string
            .split(",")
            .filter(String::isNotEmpty)
            .toTypedArray()
    }

    @TypeConverter
    fun toString(array: Array<String>): String =
        if (array.isNotEmpty()) array.joinToString(",")
        else ""


    @TypeConverter
    fun toLocalDateTime(string: String): LocalDateTime =
        LocalDateTime.parse(string, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    @TypeConverter
    fun toString(localDateTime: LocalDateTime): String =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime)
}