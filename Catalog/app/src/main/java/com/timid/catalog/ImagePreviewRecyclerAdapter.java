package com.timid.catalog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.widget.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import com.squareup.picasso.Picasso;
import android.content.Context;

public class ImagePreviewRecyclerAdapter extends RecyclerView.Adapter<ImagePreviewRecyclerAdapter.ViewHolder> {

    public ArrayList<Category.NoteItem> noteList = new ArrayList<>();
    private ActionCallback callback;

    public ImagePreviewRecyclerAdapter(ArrayList<Category.NoteItem> noteList, ActionCallback callback) {
        this.noteList = noteList;
        this.callback = callback;
    }

    @Override
    public ImagePreviewRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_preview, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ImagePreviewRecyclerAdapter.ViewHolder holder, final int position) {
        //gets the scaled width and height
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(noteList.get(position).imgUrl, options);

        float scaleFactor = Math.min((float) options.outWidth / (float) holder.thumbnail.getLayoutParams().width,
                (float) options.outHeight / (float) holder.thumbnail.getLayoutParams().height);

        final int targetW = (int) (options.outWidth / scaleFactor);
        final int targetH = (int) (options.outHeight / scaleFactor);

        if (targetW > 0 && targetH > 0) {
            //resize the thumbnail holder to fil the dimensions
            holder.thumbnail.getLayoutParams().width = targetW;
            holder.thumbnail.getLayoutParams().height = targetH;

            //get the file of the url
            File file = new File(noteList.get(position).imgUrl);
            //load pic with picasso library
            Picasso.with(holder.thumbnail.getContext())
                    .load(file)
                    .resize(targetW, targetH)
                    .into(holder.thumbnail);
        }
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;

        public ViewHolder(View itemView) {
            super(itemView);
            thumbnail = (ImageView) itemView.findViewById(R.id.image_preview_tile);

            //size reduction to create space between pictures
            int sizeReduction = (int) (MainActivity.dptopx * 3);

            int width = MainActivity.displayPoint.x;
            if(!ViewCategoryActivity.isPortrait){
                width = MainActivity.displayPoint.y;
            }

            //sets thumbnail dimensions
            thumbnail.getLayoutParams().width = width / 3 - sizeReduction;
            thumbnail.getLayoutParams().height = width / 3 - sizeReduction;

            thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.onImageClick(getAdapterPosition(), thumbnail);
                }
            });
            //set the root component's dimensions
            itemView.getLayoutParams().width = width / 3;
            itemView.getLayoutParams().height = width / 3 - sizeReduction;
        }
    }

    public interface ActionCallback {
        void onImageClick(int position, View v);
    }
}