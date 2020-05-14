package com.p5m.puzzledroid.view;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.p5m.puzzledroid.R;
import com.p5m.puzzledroid.database.Score;
import com.p5m.puzzledroid.database.ScoreDao;
import com.p5m.puzzledroid.database.PuzzledroidDatabase;
import com.p5m.puzzledroid.util.AppExecutors;
import com.p5m.puzzledroid.view.mainActivity.MainActivity;

import java.util.List;

import timber.log.Timber;

/**
 * Simple initial splash screen that lasts 3 seconds.
 */
public class SplashScreenActivity extends AppCompatActivity {

    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.i("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent=new Intent(SplashScreenActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        },3000);

        // Display the top three scores
        final ScoreDao scoreDao = PuzzledroidDatabase.getInstance(getApplicationContext()).scoreDao();
        // Run the database access code on another thread/scope
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                List<Score> scores = scoreDao.getTopThreeScores();
                Timber.i("Top Three Scores: %s", scores.toString());
            }
        });
    }
}
