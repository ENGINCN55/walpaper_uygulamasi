package com.example.walpaper_deneme03;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ayarlar extends AppCompatActivity {

    private Button btnDownloadFromCloud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ayarlar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initUI();
        setupListeners();
    }

    private void initUI() {
        btnDownloadFromCloud = findViewById(R.id.btnDownloadFromCloud); // xml'deki buton id'si bu olmalı
    }

    private void setupListeners() {
        btnDownloadFromCloud.setOnClickListener(v -> fetchFavoriteImagesAndDownload());
    }

    private void fetchFavoriteImagesAndDownload() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference favRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("favorites");

        favRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ayarlar.this, R.string.no_favorites, Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot favSnap : snapshot.getChildren()) {
                    String imageUrl = favSnap.getValue(String.class);
                    if (imageUrl != null) {
                        downloadImageToGallery(imageUrl);
                    }
                }

                Toast.makeText(ayarlar.this, R.string.download_starded, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ayarlar.this, R.string.someting_happened + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadImageToGallery(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        saveImageToGallery(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void saveImageToGallery(Bitmap bitmap) {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyWallpapers");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "fav_" + System.currentTimeMillis() + ".jpg";
        File file = new File(directory, fileName);

        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            // Galeriye bildir
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.someting_happened, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(ayarlar.this, FavoritesActivity.class);
        startActivity(intent);
        finish();
    }
}
