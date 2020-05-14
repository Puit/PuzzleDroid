package com.p5m.puzzledroid.util;

import android.app.Application;
import android.media.MediaPlayer;

import com.p5m.puzzledroid.R;

import timber.log.Timber;

/**
 * Override Application to set up some initial configuration
 */
public class PuzzleDroidApplication extends Application {
    private static PuzzleDroidApplication sInstance;
    public MediaPlayer mp;
    /**
     * Called before the first screen is shown to the user
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());

        //Create music and start
        sInstance = this;
        mp = MediaPlayer.create(this, R.raw.song1);
        mp.start();
    }
    public static PuzzleDroidApplication getInstance() {
        return PuzzleDroidApplication.sInstance;
    }


}
