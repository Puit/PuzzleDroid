package com.p5m.puzzledroid.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.p5m.puzzledroid.R;
import com.p5m.puzzledroid.database.PuzzledroidDatabase;
import com.p5m.puzzledroid.database.Score;
import com.p5m.puzzledroid.database.ScoreDao;
import com.p5m.puzzledroid.database.ScoreFirebase;
import com.p5m.puzzledroid.util.AppExecutors;
import com.p5m.puzzledroid.util.Utils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * View that displays the user scores.
 */
public class ScoresActivity extends AppCompatActivity {
    private ArrayList<ScoreFirebase> scores;
    private TextView scoresView;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scores);

        // Get the View, user and firestore objects
        scoresView = findViewById(R.id.scores_list);
        firebaseUser = Utils.firebaseUser;
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Retrieve the scores list and then show them
        retrieveScores();
    }

    /**
     * Retrieve the scores form Firebase.
     */
    private void retrieveScores() {
        CollectionReference scoresRef = firebaseFirestore.collection("scores");
        scoresRef.orderBy("Score").limit(10).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    ArrayList<ScoreFirebase> scoreList = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : task.getResult()) {
                        ScoreFirebase scoreFirebase = documentSnapshot.toObject(ScoreFirebase.class);
                        scoreList.add(scoreFirebase);
                    }
//                    Timber.i("RESULT: %s", scoreList.toString());
                    scores = scoreList;
                    showScores();
                } else {
                    Timber.i("Get failed with %s", task.getException());
                }
            }
        });
    }

    /**
     * Set the scores from Firebase to the view.
     */
    private void showScores() {
        if (scores.isEmpty()) {
            scoresView.setText("There are no scores yet.");
        } else {
            scoresView.setText(ScoreFirebase.formatScoreList(scores, this));
        }
    }
}
