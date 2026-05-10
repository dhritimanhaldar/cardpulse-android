package com.cardpulse.app.data

import androidx.room.TypeConverter
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromSource(value: String): TransactionSource = TransactionSource.valueOf(value)

    @TypeConverter
    fun sourceToString(source: TransactionSource): String = source.name

    @TypeConverter
    fun fromStatus(value: String): TransactionStatus = TransactionStatus.valueOf(value)

    @TypeConverter
    fun statusToString(status: TransactionStatus): String = status.name
}
