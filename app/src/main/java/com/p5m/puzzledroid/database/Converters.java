package com.p5m.puzzledroid.database;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Class so Room can persist Data values (converting them to/from Long values).
 */
public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
