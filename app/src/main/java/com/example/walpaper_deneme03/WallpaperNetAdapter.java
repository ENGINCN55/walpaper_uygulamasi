package com.example.walpaper_deneme03;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class WallpaperNetAdapter extends RecyclerView.Adapter<WallpaperNetAdapter.PhotoViewHolder> {
    private Context context;
    private List<String> imageUrls;
    private OnItemLongClickListener longClickListener;
    public OnItemSwipeListener swipeListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public interface OnItemSwipeListener {
        void onItemSwiped(int position);
    }

    public WallpaperNetAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnItemSwipeListener(OnItemSwipeListener listener) {
        this.swipeListener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wallpaper_net, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        String realUrl = imageUrls.get(position);
        Glide.with(context)
                .load(realUrl)
                .into(holder.imageView);
    }


    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewNet);

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(getAdapterPosition());
                }
                return true;
            });
        }
    }

    public void updatePhotos(List<String> newPhotoUrls) {
        this.imageUrls = newPhotoUrls;
        notifyDataSetChanged();
    }


}