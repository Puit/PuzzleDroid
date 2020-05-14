package com.p5m.puzzledroid.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * Class that represents the SQLite database.
 */
@Database(
        entities = {
                Score.class
        },
        exportSchema = false,
        version = 8
)
@TypeConverters({Converters.class})
public abstract class PuzzledroidDatabase extends RoomDatabase {
    public abstract ScoreDao scoreDao();
    private static final String DB_NAME = "puzzledroid_db";
    private static PuzzledroidDatabase instance;

    public static synchronized PuzzledroidDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    PuzzledroidDatabase.class, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
