package com.p5m.puzzledroid.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.p5m.puzzledroid.util.PuzzlePiece;
import com.p5m.puzzledroid.util.PuzzleDroidApplication;
import com.p5m.puzzledroid.R;
import com.p5m.puzzledroid.util.TouchListener;
import com.p5m.puzzledroid.database.PuzzledroidDatabase;
import com.p5m.puzzledroid.database.Score;
import com.p5m.puzzledroid.database.ScoreDao;
import com.p5m.puzzledroid.util.AppExecutors;
import com.p5m.puzzledroid.util.UnsolvedImages;
import com.p5m.puzzledroid.util.Utils;
import com.p5m.puzzledroid.view.mainActivity.MainActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import timber.log.Timber;

import static java.lang.Math.abs;

public class PuzzleActivity extends AppCompatActivity {
    // All the pieces that make up the puzzle
    ArrayList<PuzzlePiece> pieces;
    String imageUrl;

    // The score that will be recorded for this puzzle
    Score score;
    // Firebase
    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser firebaseUser;

    Animation animation;
    ImageView imageView;
    Bitmap puzzlePiece;
    RelativeLayout layout;

    //Audio
    MediaPlayer mp;
    Button audioOff;

    public final static String EXTRA_MESSAGE_LAST_SCORE = "Desktop-P5M-app.lastScore";
    public final static String EXTRA_MESSAGE_RECORD = "Desktop-P5M-app.record";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mp = PuzzleDroidApplication.getInstance().mp;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle);
        Timber.i("onCreate");

        // Get the data from the intent
        Intent intent = getIntent();
        imageUrl = intent.getStringExtra("assetName");
        Timber.i("AssetName: %s", imageUrl);

        // Find the Views
        imageView = findViewById(R.id.imageView);
        layout = findViewById(R.id.layout);

        // Load the image to the View (it is done asynchronously)
        // It will call the code of the cutting of the image once it's loaded
        loadImageToView();

        // Create the Score object with the current time
        score = new Score();
        score.setPuzzleName(imageUrl);
        score.setInitialTime(Calendar.getInstance().getTime());

        // Firebase
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseUser = Utils.firebaseUser;
    }
    protected void onPause(){
        super.onPause();
        mp.pause();
    }

    protected void onResume(){
        super.onResume();
        mp.start();
    }
    public void onAudioClick(View view) {
        audioOff = (Button)findViewById(R.id.audioOff);
        audioOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mp != null && mp.isPlaying()) {
                    mp.pause();
                }
                else{
                    mp.start();
                }
            }
        });
    }

    /**
     * Loads the image to the View, using Glide
     * It does so asynchronously.
     * Once it's loaded, run the code that makes the pieces out of it.
     */
    private void loadImageToView() {
        Timber.i("loadImageToView: %s", imageUrl);
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        imageView.setImageBitmap(resource);
                        Timber.i("BITMAP IMAGE IS LOADED WITH GLIDE");
                        // Once it's loaded, continue with the necessary code
                        onImageLoaded();
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
//        File imgFile = new File(assetName);
//        if(imgFile.exists()) {
//            Timber.i("Exists");
//            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//            imageView.setImageBitmap(myBitmap);
//        } else {
//            Timber.i("Does not exist");
//        }
    }

    /**
     * This method is called when Glide finishes loading the image.
     */
    private void onImageLoaded() {
        pieces = cutImage();
        TouchListener touchListener = new TouchListener(PuzzleActivity.this);
        // Shuffle pieces order
        Collections.shuffle(pieces);
        for (PuzzlePiece piece : pieces) {
            piece.setOnTouchListener(touchListener);
            layout.addView(piece);
            // randomize position, on the bottom of the screen
            RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) piece.getLayoutParams();
            lParams.leftMargin = new Random().nextInt(layout.getWidth() - piece.width);
            lParams.topMargin = layout.getHeight() - piece.height;
            piece.setLayoutParams(lParams);
        }
    }

    /**
     * Returns the pieces made from the image.
     * @return
     */
    private ArrayList<PuzzlePiece> cutImage() {
        Timber.i("cutImage");
        int rows = 3;
        int cols = 4;
//        int rows = 1;
//        int cols = 1;
        int piecesNumber = rows * cols;

        ImageView imageView = findViewById(R.id.imageView);
        ArrayList<PuzzlePiece> pieces = new ArrayList<>(piecesNumber);

        // Get the scaled bitmap of the source image
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        int[] dimensions = getPositionInImage(imageView);
        int scaledBitmapLeft = dimensions[0];
        int scaledBitmapTop = dimensions[1];
        int scaledBitmapWidth = dimensions[2];
        int scaledBitmapHeight = dimensions[3];

        int croppedImageWidth = scaledBitmapWidth - 2 * abs(scaledBitmapLeft);
        int croppedImageHeight = scaledBitmapHeight - 2 * abs(scaledBitmapTop);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledBitmapWidth, scaledBitmapHeight, true);
        Bitmap croppedBitmap = Bitmap.createBitmap(scaledBitmap, abs(scaledBitmapLeft), abs(scaledBitmapTop), croppedImageWidth, croppedImageHeight);

        // Calculate the with and height of the pieces
        int pieceWidth = croppedImageWidth/cols;
        int pieceHeight = croppedImageHeight/rows;

        // Create each bitmap piece and add it to the resulting array
        // The image bitmap has to be centered: x=0 and y=0
        int yCoord = 0;
        for (int row = 0; row < rows; row++) {
            int xCoord = 0;
            for (int col = 0; col < cols; col++) {
                // calculate offset for each piece
                int offsetX = 0;
                int offsetY = 0;
                if (col > 0) {
                    offsetX = pieceWidth / 3;
                }
                if (row > 0) {
                    offsetY = pieceHeight / 3;
                }
                // Apply the offset to each piece
                Bitmap pieceBitmap = Bitmap.createBitmap(croppedBitmap, xCoord - offsetX, yCoord - offsetY, pieceWidth + offsetX, pieceHeight + offsetY);
                PuzzlePiece piece = new PuzzlePiece(getApplicationContext());
                piece.setImageBitmap(pieceBitmap);
                piece.x = xCoord - offsetX + imageView.getLeft();
                piece.y = yCoord - offsetY + imageView.getTop();
                piece.width = pieceWidth + offsetX;
                piece.height = pieceHeight + offsetY;

                // this bitmap will hold our final puzzle piece image
                puzzlePiece = Bitmap.createBitmap(pieceWidth + offsetX, pieceHeight + offsetY, Bitmap.Config.ARGB_8888);

                // draw path
                int bumpSize = pieceHeight / 4;
                Canvas canvas = new Canvas(puzzlePiece);
                Path path = new Path();
                path.moveTo(offsetX, offsetY);
                if (row == 0) {
                    // top side piece
                    path.lineTo(pieceBitmap.getWidth(), offsetY);
                } else {
                    // top bump
                    path.lineTo(offsetX + (pieceBitmap.getWidth() - offsetX) / 3, offsetY);
                    path.cubicTo(offsetX + (pieceBitmap.getWidth() - offsetX) / 6, offsetY - bumpSize, offsetX + (pieceBitmap.getWidth() - offsetX) / 6 * 5, offsetY - bumpSize, offsetX + (pieceBitmap.getWidth() - offsetX) / 3 * 2, offsetY);
                    path.lineTo(pieceBitmap.getWidth(), offsetY);
                }

                if (col == cols - 1) {
                    // right side piece
                    path.lineTo(pieceBitmap.getWidth(), pieceBitmap.getHeight());
                } else {
                    // right bump
                    path.lineTo(pieceBitmap.getWidth(), offsetY + (pieceBitmap.getHeight() - offsetY) / 3);
                    path.cubicTo(pieceBitmap.getWidth() - bumpSize,offsetY + (pieceBitmap.getHeight() - offsetY) / 6, pieceBitmap.getWidth() - bumpSize, offsetY + (pieceBitmap.getHeight() - offsetY) / 6 * 5, pieceBitmap.getWidth(), offsetY + (pieceBitmap.getHeight() - offsetY) / 3 * 2);
                    path.lineTo(pieceBitmap.getWidth(), pieceBitmap.getHeight());
                }

                if (row == rows - 1) {
                    // bottom side piece
                    path.lineTo(offsetX, pieceBitmap.getHeight());
                } else {
                    // bottom bump
                    path.lineTo(offsetX + (pieceBitmap.getWidth() - offsetX) / 3 * 2, pieceBitmap.getHeight());
                    path.cubicTo(offsetX + (pieceBitmap.getWidth() - offsetX) / 6 * 5,pieceBitmap.getHeight() - bumpSize, offsetX + (pieceBitmap.getWidth() - offsetX) / 6, pieceBitmap.getHeight() - bumpSize, offsetX + (pieceBitmap.getWidth() - offsetX) / 3, pieceBitmap.getHeight());
                    path.lineTo(offsetX, pieceBitmap.getHeight());
                }

                if (col == 0) {
                    // left side piece
                    path.close();
                } else {
                    // left bump
                    path.lineTo(offsetX, offsetY + (pieceBitmap.getHeight() - offsetY) / 3 * 2);
                    path.cubicTo(offsetX - bumpSize, offsetY + (pieceBitmap.getHeight() - offsetY) / 6 * 5, offsetX - bumpSize, offsetY + (pieceBitmap.getHeight() - offsetY) / 6, offsetX, offsetY + (pieceBitmap.getHeight() - offsetY) / 3);
                    path.close();
                }

                // mask the piece
                Paint paint = new Paint();
                paint.setColor(0XFF000000);
                paint.setStyle(Paint.Style.FILL);

                canvas.drawPath(path, paint);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                canvas.drawBitmap(pieceBitmap, 0, 0, paint);

                // draw a white border
                Paint border = new Paint();
                border.setColor(0X80FFFFFF);
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(8.0f);
                canvas.drawPath(path, border);

                // draw a black border
                border = new Paint();
                border.setColor(0X80000000);
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(3.0f);
                canvas.drawPath(path, border);

                // set the resulting bitmap to the piece
                piece.setImageBitmap(puzzlePiece);

                pieces.add(piece);
                xCoord += pieceWidth;
            }
            yCoord += pieceHeight;
        }
        return pieces;
    }

    private int[] getPositionInImage(ImageView imageView) {
        int[] ret = new int[4];
        Timber.i("getPositionInImage");

        if (imageView == null || imageView.getDrawable() == null)
            return ret;

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int actW = Math.round(origW * scaleX);
        final int actH = Math.round(origH * scaleY);

        ret[2] = actW;
        ret[3] = actH;

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - actH)/2;
        int left = (int) (imgViewW - actW)/2;

        ret[0] = left;
        ret[1] = top;

        return ret;
    }
    /**
     * Check if the puzzle is finished. If all pieces are non-movable, it is.
     */
    public void checkEnd() {
        Timber.i("checkEnd");
        boolean allUnmovable = true;
        for (PuzzlePiece piece : pieces) {
            if (piece.movable) {
                allUnmovable = false;
                break;
            }
        }
        if (allUnmovable) {
            //Show animation before finishing the puzzle
            animation = AnimationUtils.loadAnimation(PuzzleActivity.this,R.anim.bounce);
            imageView.bringToFront();
            //Set Alpha to avoid opacity
            imageView.setAlpha((float) 1.0);
            imageView.startAnimation(animation);

            //delay before calling onFinish
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    onFinishPuzzle();
                }
            }, 1500);
        }
    }

    /**
     * Called upon finishing the puzzle. Store the score and exit the view.
     */
    private void onFinishPuzzle() {
        Timber.i("onFinishPuzzle");
        score.setFinishTime(Calendar.getInstance().getTime());
        // Calculate the seconds between the two dates (the units are milliseconds)
        long difference = score.getFinishTime().getTime() - score.getInitialTime().getTime();
        score.setScoreSeconds((int) (difference / 1000));
        final ScoreDao scoreDao = PuzzledroidDatabase.getInstance(this).scoreDao();

        // Add score to Firebase database
        String userName = firebaseUser.getDisplayName();
        Map<String, Object> userEntry = new HashMap<>();
        userEntry.put("Date", score.getFinishTime().toString());
        userEntry.put("Score", score.getScoreSeconds());
        userEntry.put("puzzleName", score.getPuzzleName());
        userEntry.put("user", userName);

        // Add a new document with a generated ID
        firebaseFirestore.collection("scores")
                .add(userEntry)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Timber.i("DocumentSnapshot added with ID: %s", documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.i( "Error adding document %s", e);
                    }
                });
        //Send intent to the app notification to show the last score
        Intent reply = new Intent();
        reply.putExtra(EXTRA_MESSAGE_LAST_SCORE, Integer.toString(score.getScoreSeconds()));
        setResult(RESULT_OK, reply);

        // Insert the new score to the calendar
        //insertToCalendar();

        // If the puzzle was created by the random action, remove it from the unsolved images
        if (MainActivity.selectedOrRandom == "random") {
            UnsolvedImages.removeUnsolvedImage(imageUrl);
            Timber.i("Removed unsolved image: " + imageUrl);
        }

        // Exit the view
        finish();
    }

    /**
     * Insert the new score to the calendar.
     */
    private void insertToCalendar() {
        Date finishTime = score.getFinishTime();
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, finishTime.getTime())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, finishTime.getTime())
                .putExtra(CalendarContract.Events.TITLE, "Puzzledroid Score")
                .putExtra(CalendarContract.Events.HAS_ALARM, false)
                .putExtra(CalendarContract.Events.DESCRIPTION, "Puzzle: " +
                        score.getPuzzleName() + "\n" +
                        "Score: " + score.getScoreSeconds() + " seconds.");
        startActivity(intent);
    }

    private void setImageFromPath(String mCurrentPhotoPath, ImageView imageView) {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();
        Timber.i("setImageFromPath");

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        Bitmap rotatedBitmap = bitmap;

        // rotate bitmap if needed
        try {
            ExifInterface ei = new ExifInterface(mCurrentPhotoPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;
            }
        } catch (IOException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }

        imageView.setImageBitmap(rotatedBitmap);
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Timber.i("rotateImage");
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
    //Info button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                Intent webviewIntent = new Intent(this, HelpActivity.class);
                startActivity(webviewIntent);
                return true;
            case R.id.scores:
                startActivity(new Intent(this, ScoresActivity.class));
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
