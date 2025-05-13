package com.example.walpaper_deneme03;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;


import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<String> photoUrls;

    // Constructor
    public PhotoAdapter() {
        photoUrls = new ArrayList<>();
    }
    public String getPhotoAt(int position) {
        return photoUrls.get(position); // Pozisyona göre URL döndürür
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
        String photoUrl = photoUrls.get(position);
        Glide.with(holder.itemView.getContext())
                .load(photoUrl)
                .into(holder.imageView);

    }

    @Override
    public int getItemCount() {
        return photoUrls.size();
    }

    // Update photos
    public void updatePhotos(List<String> newUrls) {
        photoUrls.clear();
        photoUrls.addAll(newUrls);
        notifyDataSetChanged();
    }

    // ViewHolder class
    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

}