package com.p5m.puzzledroid.database;

import com.p5m.puzzledroid.R;
import com.p5m.puzzledroid.view.PuzzleActivity;
import com.p5m.puzzledroid.view.ScoresActivity;
import com.p5m.puzzledroid.view.mainActivity.MainActivity;

import java.util.ArrayList;

/**
 * Score object returned by Firebase.
 */
public class ScoreFirebase {
    private String Date;
    private int Score;
    private String puzzleName;
    private String user;

    public ScoreFirebase(){}

    public ScoreFirebase(String date, int score, String puzzleName, String user) {
        Date = date;
        Score = score;
        this.puzzleName = puzzleName;
        this.user = user;
    }

    @Override
    public String toString() {
        return "ScoreFirebase{" +
                "Date='" + Date + '\'' +
                ", Score=" + Score +
                ", puzzleName='" + puzzleName + '\'' +
                ", user='" + user + '\'' +
                '}';
    }

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }

    public int getScore() {
        return Score;
    }

    public void setScore(int score) {
        Score = score;
    }

    public String getPuzzleName() {
        return puzzleName;
    }

    public void setPuzzleName(String puzzleName) {
        this.puzzleName = puzzleName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Format the scores to show the list.
     * @param scoreList
     * @return
     */
    public static String formatScoreList(ArrayList<ScoreFirebase> scoreList, ScoresActivity scoresActivity) {
        String result = "";
        int counter = 1;
        for (ScoreFirebase score : scoreList) {
            result += counter + ". " + score.user + "\n" +
            "    " + scoresActivity.getResources().getString(R.string.score) + ": " + score.Score + " " + scoresActivity.getResources().getString(R.string.seconds) + "\n" +
                    "    " + scoresActivity.getResources().getString(R.string.date) + ": " + score.Date + "\n" +
                    "    Puzzle: " + score.puzzleName.substring(92, 102) + "\n\n";
            counter++;
        }
        return result;
    }
}
