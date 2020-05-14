package com.p5m.puzzledroid.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO of the Room database.
 */
@Dao
public interface ScoreDao {

    @Insert
    void insert(Score score);

    @Update
    void update(Score score);

    @Delete
    void delete(Score score);

    @Query("Select * from scores")
    List<Score> getScores();

    @Query("SELECT * from scores WHERE id = :key")
    Score get(int key);

    @Query("DELETE FROM scores")
    void clear();

    @Query("SELECT * FROM scores ORDER BY id DESC")
    List<Score> getAllScores();

    @Query("SELECT * FROM scores ORDER BY id ASC LIMIT 1")
    List<Score> getLastScore();

    @Query("SELECT * FROM scores ORDER BY score_seconds ASC LIMIT 3")
    List<Score> getTopThreeScores();
}
