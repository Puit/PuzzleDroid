package com.p5m.puzzledroid.view.mainActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.p5m.puzzledroid.R;

import java.io.File;
import java.util.List;

import timber.log.Timber;

/**
 * Adapter for the MainActivity gridView
 */
public class MainActivityAdapter extends BaseAdapter {
    private Context context;
    private List<String> imagesUrls;

    public MainActivityAdapter(Context c, List<String> imagesList) {
        Timber.i("ImageController");
        context = c;
        imagesUrls = imagesList;
    }

    public int getCount() {
        return imagesUrls.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // inflate the layout for each list row
        if (convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.grid_element, parent, false);
        }
        // Get current image to be displayed
        String currentImage = imagesUrls.get(position);
        // Add it to the ImageView
        ImageView imageView = convertView.findViewById(R.id.gridImageView);
        Timber.i("ImageAdapter currentImage: " + currentImage);

        Glide.with(context)
                .load(currentImage)
                .into(imageView);

        return convertView;
    }
}