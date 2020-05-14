package com.p5m.puzzledroid.view.mainActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;

import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.p5m.puzzledroid.database.ScoreFirebase;
import com.p5m.puzzledroid.util.PuzzleDroidApplication;
import com.p5m.puzzledroid.R;
import com.p5m.puzzledroid.util.UnsolvedImages;
import com.p5m.puzzledroid.util.Utils;
import com.p5m.puzzledroid.view.HelpActivity;
import com.p5m.puzzledroid.view.PuzzleActivity;
import com.p5m.puzzledroid.view.ScoresActivity;
import com.p5m.puzzledroid.view.SplashScreenActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.core.content.FileProvider;

import timber.log.Timber;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 2;
    static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 3;
    static final int REQUEST_IMAGE_GALLERY = 4;

    //Audio
    MediaPlayer mp;
    Button audioOff;

    //Notification strings
    public String not_title;
    public String not_subtitle;
    public String not_show_not;
    public String seconds;

    // "random" or "selected". For the PuzzleControllerActivity to know how the puzzle started
    public static String selectedOrRandom;

    String photoPath;

    // Firebase
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private int RC_SIGN_IN = 12345;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser firebaseUser;


    // Links to the images
    private ArrayList<String> imagesUrls;

    // Views
    private GridView gridView;
    private Button signOutButton;

    //Notification management
    private static final String PRIMARY_CHANNEL_ID = "Desktop-P5M-app.ChannelID";
    private static final int NOTIFICATION_ID = 0;
    private static final String ACTION_UPDATE_NOTIFICATION = "Desktop-P5M-app.ACTION_UPDATE_NOTIFICATION";
    private NotificationManager miNotifyManager;
    private NotificationReceiver miReceiver = new NotificationReceiver();
    int lastScore, recordScore;
    private static final int PUZZLE_CONTROLLER_ID = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.i("OnCreate");
        super.onCreate(savedInstanceState);
        loadLocale();
        setContentView(R.layout.activity_main);
        createNotificationChannel();
        registerReceiver(miReceiver, new IntentFilter(ACTION_UPDATE_NOTIFICATION));
        mp = PuzzleDroidApplication.getInstance().mp;

        // Find some Views
        gridView = findViewById(R.id.grid);
        signOutButton = findViewById(R.id.sign_out_button);

        // Get the images list (path where they will be saved)
        imagesUrls = getFirebaseImagesWithUrl();

        // Load the GridView
        loadGridView();

        UnsolvedImages unsolvedImages = UnsolvedImages.getInstance();
        unsolvedImages.setUnsolvedImages(imagesUrls);
        unsolvedImages.setNumberOfImages(imagesUrls.size());
        Timber.i("Unsolved images list filled: %s", unsolvedImages.toString());

        // Change actionbar title, if not it will go according to the default language system
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.app_name));

        Button changeLang = findViewById(R.id.changeMyLang);
        changeLang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChangeLanguageDialog();
            }
        });

        // Firebase
        // Choose authentication providers
        providers = Arrays.asList(new AuthUI.IdpConfig.GoogleBuilder().build());
        firebaseFirestore = FirebaseFirestore.getInstance();
        doGoogleLogin();

        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doGoogleLogOut();
            }
        });
    }

    /**
     * Log in to the user's Google account.
     */
    private void doGoogleLogin() {
        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    /**
     * Log out of the Google account. It always prompts you again to log in.
     */
    private void doGoogleLogOut() {
        AuthUI.getInstance()
                .signOut(getApplicationContext())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        doGoogleLogin();
                    }
                });
    }

    /**
     * Return the list of the urls of each image.
     */
    private ArrayList<String> getFirebaseImagesWithUrl() {
        ArrayList<String> images = new ArrayList<>();
        images.add("https://firebasestorage.googleapis.com/v0/b/puzzledroid-p5m.appspot.com/o/firebase_images%2Fanimal-17542_1280.jpg?alt=media&token=06fcfe13-8790-435f-ab58-c54d98e8b254");
        images.add("https://firebasestorage.googleapis.com/v0/b/puzzledroid-p5m.appspot.com/o/firebase_images%2Fcalico-518375_1280.jpg?alt=media&token=d309f481-a6fb-485b-8f9b-72aef4b36c1c");
        images.add("https://firebasestorage.googleapis.com/v0/b/puzzledroid-p5m.appspot.com/o/firebase_images%2Fcat-1474092_1280.jpg?alt=media&token=9e1e06b3-c2f3-440f-b9ec-70b1edfa721e");
        images.add("https://firebasestorage.googleapis.com/v0/b/puzzledroid-p5m.appspot.com/o/firebase_images%2Fcat-2093639_1280.jpg?alt=media&token=c4ddd58f-dfb7-4d45-8fd1-d80a3724b09d");
        images.add("https://firebasestorage.googleapis.com/v0/b/puzzledroid-p5m.appspot.com/o/firebase_images%2Fcat-4082223_1280.jpg?alt=media&token=d9da7fde-a9b4-45de-b146-44259e42eb33");
        images.add("https://firebasestorage.googleapis.com/v0/b/puzzledroid-p5m.appspot.com/o/firebase_images%2Fcat-4665180_1280.jpg?alt=media&token=d46e6ffa-6186-4763-b137-b70308d9d480");
        images.add("https://firebasestorage.googleapis.com/v0/b/puzzledroid-p5m.appspot.com/o/firebase_images%2Fcat-4919903_1280.jpg?alt=media&token=c7b63ee9-f8fa-460b-9087-8b49be0d264e");
        images.add("https://firebasestorage.googleapis.com/v0/b/puzzledroid-p5m.appspot.com/o/firebase_images%2Fcat-814141_1280.jpg?alt=media&token=984876e9-490f-4c02-a442-d537ab128caa");
        images.add("https://firebasestorage.googleapis.com/v0/b/puzzledroid-p5m.appspot.com/o/firebase_images%2Fvintage-986051_1280.png?alt=media&token=a0ad516c-c74a-40e2-a2c0-d1e86a3d5f3d");
        return images;
    }

    /**
     * Download all images from our Firebase storage.
     */
    private void downloadAllImages() {
        storageReference = firebaseStorage.getInstance().getReference();
        for (String imageName : imagesUrls) {
            downloadFromFirebase(imageName);
        }
        Timber.i("ALL IMAGES DOWNLOADED");
    }

    /**
     * Download image from our Firebase storage, using its fileName path
     * Does so asynchronously.
     *
     * @param fileName
     */
    public void downloadFromFirebase(final String fileName) {
        StorageReference fileReference = storageReference.child(fileName);
        fileReference.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Timber.i("Success! Filename: " + fileName);
                        String url = uri.toString();
                        Timber.i("PATH: " + getApplicationInfo().dataDir);
                        Timber.i("Url: " + url);
                        downloadFile(MainActivity.this, fileName, ""
                                , url);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.i("Failure! " + e.getMessage());
                    }
                });
    }

    /**
     * Method called when all the images are downloaded.
     * It is useful because the downlaod is asynchronous. Therefore, before loading the gridView,
     * all the images must be downloaded.
     */
    private void onFinishedDownloadedAllImages() {
        loadGridView();
    }

    /**
     * Loads the gridView.
     */
    private void loadGridView() {
        gridView.setAdapter(new MainActivityAdapter(this, imagesUrls));
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Timber.i("Create Intent");
                Intent intent = new Intent(getApplicationContext(), PuzzleActivity.class);
                int index = i % imagesUrls.size();
                String image = imagesUrls.get(index);
                Timber.i("Image: " + image + ", Index: " + index);
                intent.putExtra("assetName", image);
                selectedOrRandom = "selected";
                startActivityForResult(intent, PUZZLE_CONTROLLER_ID);
            }
        });
    }

    /**
     * Download image from URL
     *
     * @param context
     * @param fileName
     * @param fileExtension
     * @param url
     */
    public void downloadFile(Context context, String fileName, String fileExtension, String url) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationInExternalPublicDir("", fileName + fileExtension);
        downloadManager.enqueue(request);
    }

    private void showChangeLanguageDialog() {
        //array of languages to display in alert dialog
        final String[] listItems = {"English", "Spanish"};
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        mBuilder.setTitle("Choose language...");
        mBuilder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    setLocale("en");
                    recreate();
                }
                if (i == 1) {
                    setLocale("es");
                    recreate();
                }
            }
        });
        AlertDialog mDialog = mBuilder.create();
        //show alert dialog
        mBuilder.show();
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        //save data to shared preferences
        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("My_Lang", lang);
        editor.apply();
    }

    //load language saved in shared preferences
    public void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("Settings", Activity.MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "");
        setLocale(language);
    }
    //Pause and Resume music (en segundo plano)

    protected void onPause() {
        super.onPause();
        mp.pause();
    }

    protected void onResume() {
        super.onResume();
        mp.start();
    }

    public void onAudioClick(View view) {
        audioOff = (Button) findViewById(R.id.audioOff);
        audioOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mp != null && mp.isPlaying()) {
                    mp.pause();
                } else {
                    mp.start();
                }
            }
        });
    }

    /**
     * Make a puzzle of one of the unsolved images.
     *
     * @param view
     */
    public void onRandomClick(View view) {
        Timber.i("onRandomClick");
        List<String> unsolvedImages = UnsolvedImages.getInstance().getUnsolvedImages();
        if (unsolvedImages.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    "You have completed all the images.",
                    Toast.LENGTH_LONG).show();
        } else {
            String randomImage = UnsolvedImages.getRandomUnsolvedImage();
            Timber.i("Random Image: " + randomImage);
            Toast.makeText(getApplicationContext(),
                    "Current random image: " + UnsolvedImages.getNumberOfSolvedImages() + "/" +
                            UnsolvedImages.getInstance().getNumberOfImages() + ".",
                    Toast.LENGTH_LONG).show();
            Intent intent = new Intent(getApplicationContext(), PuzzleActivity.class);
            intent.putExtra("assetName", randomImage);
            selectedOrRandom = "random";
            startActivityForResult(intent, PUZZLE_CONTROLLER_ID);
        }
    }

    public void onCameraClick(View view) {
/*        AlertDialog.Builder camAlert = new AlertDialog.Builder(MainActivity.this);
        camAlert.setTitle("Próximamente");
        camAlert.setMessage("Esta función estará lista en el segundo producto");
        camAlert.setPositiveButton("De acuerdo", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        camAlert.show();*/

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                if (photoFile != null) {
                    Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                }
            } catch (IOException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG);
            }
        }
    }

    public void onGalleryClick(View view) {
        Timber.i("onGalleryClick");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGE_GALLERY);
        }
    }

    private File createImageFile() throws IOException {
        Timber.i("createImageFile");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // permission not granted, initiate request
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        } else {
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            //Picture is saved in download because pictures and DCIM folders are not instantly reacheable from Gallery
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            photoPath = image.getAbsolutePath(); // save this to use in the intent

            return image;
        }

        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Timber.i("onRequestPermissionsResult");
        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onCameraClick(new View(this));
                }

                return;
            }
        }
    }

    // Method called when coming from an Intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.i("onActivityResult");
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Intent intent = new Intent(this, PuzzleActivity.class);
            intent.putExtra("mCurrentPhotoPath", photoPath);
            startActivity(intent);
        }
        if (requestCode == REQUEST_IMAGE_GALLERY && resultCode == RESULT_OK) {
            Uri uri = data.getData();

            Intent intent = new Intent(this, PuzzleActivity.class);
            intent.putExtra("mCurrentPhotoUri", uri.toString());
            startActivity(intent);
        }
        if (requestCode == PUZZLE_CONTROLLER_ID && resultCode == RESULT_OK) {
            lastScore = Integer.parseInt(data.getStringExtra(PuzzleActivity.EXTRA_MESSAGE_LAST_SCORE));
            sendNotification();
        }
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                // Add the firebase user to the variable
                firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                Utils.firebaseUser = firebaseUser;
            } else {
                // It is mandatory to be logged in
                Toast.makeText(this, "You must log in.", Toast.LENGTH_LONG);
                doGoogleLogin();
            }
        }
    }

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

    public void createNotificationChannel() {
        miNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //notification strings
        not_title = getResources().getString(R.string.not_title);
        not_subtitle = getResources().getString(R.string.not_subtitle);
        not_show_not = getResources().getString(R.string.not_show_not);
        seconds = getResources().getString(R.string.seconds);

        //para que en las versiones antiguas siga funcinando
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Crearemos el canal
            NotificationChannel nChannel = new NotificationChannel(PRIMARY_CHANNEL_ID,
                    not_title, NotificationManager.IMPORTANCE_HIGH);
            nChannel.enableLights(true);
            nChannel.setLightColor(Color.RED);
            nChannel.enableVibration(true);
            nChannel.setDescription(not_subtitle);
            miNotifyManager.createNotificationChannel(nChannel);
        }
    }

/*    public void notificacionPersonalizada(View view) {

        RemoteViews rmViewsSmall = new RemoteViews(getPackageName(), R.layout.customsmall);
        RemoteViews rmViewsGrande = new RemoteViews(getPackageName(), R.layout.customnotification);

        rmViewsGrande.setTextViewText(R.id.tvCustom1, "¡Nueva puntuación!");
        rmViewsGrande.setTextViewText(R.id.tvCustom2, "Tu nueva puntuación ha sido de: ");

        Notification noti = new NotificationCompat.Builder(getApplicationContext(),
                PRIMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setCustomContentView(rmViewsSmall)
                .setCustomBigContentView(rmViewsGrande)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .build();
        miNotifyManager.notify(1, noti);
    }*/

    public void sendNotification() {

        Intent updateIntent = new Intent(ACTION_UPDATE_NOTIFICATION);
        PendingIntent pIntent = PendingIntent.getBroadcast(
                this, NOTIFICATION_ID, updateIntent, PendingIntent.FLAG_ONE_SHOT);


        NotificationCompat.Builder nBuilder = getNotificationBuilder();

        nBuilder.addAction(R.drawable.ic_notification, "Ver puntuación", pIntent);
        miNotifyManager.notify(NOTIFICATION_ID, nBuilder.build());

    }

    private NotificationCompat.Builder getNotificationBuilder() {
        Intent notiIntent = new Intent(this, MainActivity.class);
        //PendingIntent es como un intent normal pero lo dejas abierto y ya se ejecutará cuando le llames
        PendingIntent pIntent = PendingIntent.getActivity(this, NOTIFICATION_ID,
                notiIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL_ID)
                .setContentTitle("¡Nueva puntuación!")
                .setContentText("Tu nueva puntuación ha sido de: ")
                .setContentIntent(pIntent) //al dar click abrirá lo que esté en pIntent
                .setAutoCancel(true) //al dar click cierra la notificacion
                .setSmallIcon(R.drawable.ic_notification);
        return notifyBuilder;
    }

    public void updateNotification() {
        //Bitmap androidImage = BitmapFactory.decodeResource(getResources(), R.drawable.bolivia);
        NotificationCompat.Builder nBuilder = getNotificationBuilder();
        nBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(Integer.toString(lastScore) + " segundos"));

        miNotifyManager.notify(NOTIFICATION_ID, nBuilder.build());
    }

    public class NotificationReceiver extends BroadcastReceiver {
        public NotificationReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            //Actualizaremos la notificacion
            updateNotification();
        }
    }
}