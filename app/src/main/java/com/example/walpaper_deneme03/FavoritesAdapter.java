package com.example.walpaper_deneme03;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavViewHolder> {

    private Context context;
    private List<File> imageFiles;

    public FavoritesAdapter(Context context, List<File> imageFiles) {
        this.context = context;
        this.imageFiles = imageFiles;
    }

    @NonNull
    @Override
    public FavViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite, parent, false);
        return new FavViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavViewHolder holder, int position) {
        File imageFile = imageFiles.get(position);
        Glide.with(context).load(imageFile).into(holder.imageView);

        // Uzun tıklama işlemi
        holder.itemView.setOnLongClickListener(v -> {
            // Kullanıcıya duvar kağıdı yapmak isteyip istemediğini soralım
            showWallpaperDialog(imageFile);
            return true; // uzun tıklamayı yakaladık, başka işlem yapmasın
        });
    }

    @Override
    public int getItemCount() {
        return imageFiles.size();
    }

    private void showWallpaperDialog(File imageFile) {
        // Burada görseli duvar kağıdı olarak ayarlama işlemi yapılacak.
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Duvar Kağıdı Olarak Ayarla")
                .setMessage("Bu görseli duvar kağıdı olarak ayarlamak istiyor musun moruk?")
                .setPositiveButton("Evet", (dialog, which) -> setWallpaper(imageFile))
                .setNegativeButton("Hayır", null)
                .show();
    }

    private void setWallpaper(File imageFile) {
        Glide.with(context)
                .asBitmap()
                .load(imageFile)
                .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                        try {
                            android.app.WallpaperManager wallpaperManager = android.app.WallpaperManager.getInstance(context);
                            wallpaperManager.setBitmap(resource);
                            Toast.makeText(context, "Duvar kağıdın ayarlandı, kral!", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, "Duvar kağıdı ayarlanamadı aq", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
                });
    }

    public static class FavViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public FavViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.favoriteImageView);
        }
    }
}
