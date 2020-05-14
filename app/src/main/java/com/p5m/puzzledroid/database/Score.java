package com.p5m.puzzledroid.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;
import java.util.List;

/**
 * Class that represents a table from the database.
 */
@Entity(tableName = "scores")
public class Score {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "puzzle_name")
    private String puzzleName;
    @ColumnInfo(name = "initial_time")
    private Date initialTime;
    @ColumnInfo(name = "finish_time")
    private Date finishTime;
    @ColumnInfo(name = "score_seconds")
    private int scoreSeconds;

    /**
     * Constructor, necessary for Room.
     * @param id
     * @param puzzleName
     * @param initialTime
     * @param finishTime
     * @param scoreSeconds
     */
    public Score(int id, String puzzleName, Date initialTime, Date finishTime, int scoreSeconds) {
        this.id = id;
        this.puzzleName = puzzleName;
        this.initialTime = initialTime;
        this.finishTime = finishTime;
        this.scoreSeconds = scoreSeconds;
    }

    /**
     * Constructor that will be used in the program.
     */
    @Ignore
    public Score() { }

    @Ignore
    @Override
    public String toString() {
        return "Score{" +
                "id=" + id +
                ", puzzleName='" + puzzleName + '\'' +
                ", initialTime='" + initialTime + '\'' +
                ", finishTime='" + finishTime + '\'' +
                ", scoreSeconds=" + scoreSeconds +
                '}';
    }

    /*
    The setters and getters are necessary for Room
     */
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPuzzleName() {
        return puzzleName;
    }

    public void setPuzzleName(String puzzleName) {
        this.puzzleName = puzzleName;
    }

    public Date getInitialTime() {
        return initialTime;
    }

    public void setInitialTime(Date initialTime) {
        this.initialTime = initialTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public int getScoreSeconds() {
        return scoreSeconds;
    }

    public void setScoreSeconds(int scoreSeconds) {
        this.scoreSeconds = scoreSeconds;
    }

    /**
     * Format a list of scores to be shown in the Scores view.
     */
    @Ignore
    public static String formatScores(List<Score> scores) {
        String result = "";
        for (Score score : scores) {
            result = result
                    + "Puzzle: " + score.getPuzzleName() + "\n"
                    + "Time: " + score.getFinishTime() + "\n"
                    + "Score: " + score.getScoreSeconds() + " seconds" + "\n\n";
        }
        return result;
    }
}
