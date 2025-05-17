package com.example.walpaper_deneme03;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WallpaperNetActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WallpaperNetAdapter adapter;
    private List<String> likedImageUrls = new ArrayList<>();

    private DatabaseReference likesRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walpaper_net);

        recyclerView = findViewById(R.id.recyclerViewNet);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WallpaperNetAdapter(this, likedImageUrls);
        recyclerView.setAdapter(adapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        likesRef = FirebaseDatabase.getInstance().getReference("likes");

        fetchLikedImages();
        adapter.fetchPhotosFromFirebase();

        adapter.setOnItemLongClickListener(position -> {
            String photoId = likedImageUrls.get(position);
            likeImage(photoId);
        });

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                String photoId = likedImageUrls.get(position);

                // photoId'den url çek ve indir
                DatabaseReference photoUrlRef = FirebaseDatabase.getInstance()
                        .getReference("photoLikes")
                        .child(photoId)
                        .child("url");

                photoUrlRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String imageUrl = snapshot.getValue(String.class);
                            downloadImageAndSaveAsJpg(imageUrl);
                        } else {
                            Toast.makeText(WallpaperNetActivity.this, "URL bulunamadı aq", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("WallpaperNet", "Firebase hata: " + error.getMessage());
                    }
                });

                // Itemi resetle yoksa görünüm kayar
                adapter.notifyItemChanged(position);
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(WallpaperNetActivity.this, FavoritesActivity.class);
        startActivity(intent);
        finish();
    }

    private void fetchLikedImages() {
        likesRef.orderByChild("likeCount").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                likedImageUrls.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String url = child.getKey();
                    likedImageUrls.add(url);
                }
                Collections.reverse(likedImageUrls);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("WallpaperNet", "Firebase error: " + error.getMessage());
            }
        });
    }

    private void likeImage(String photoId) {
        likesRef.child(photoId).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Map<String, Object> likeData = (Map<String, Object>) currentData.getValue();
                String userId = currentUser.getUid();

                if (likeData == null) {
                    likeData = new HashMap<>();
                    likeData.put("likeCount", 1);
                    likeData.put(userId, true);
                } else if (!likeData.containsKey(userId)) {
                    Long count = (Long) likeData.get("likeCount");
                    likeData.put("likeCount", count + 1);
                    likeData.put(userId, true);
                }
                currentData.setValue(likeData);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (committed) {
                    Toast.makeText(WallpaperNetActivity.this, "Beğendin moruk 👍", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void downloadImageAndSaveAsJpg(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Favoriler");
                        if (!directory.exists()) directory.mkdirs();

                        File file = new File(directory, "fav_" + System.currentTimeMillis() + ".jpg");
                        try (FileOutputStream out = new FileOutputStream(file)) {
                            resource.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            Toast.makeText(WallpaperNetActivity.this, "İndirdim moruk 📥", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(WallpaperNetActivity.this, "Sıkıntı çıktı aq", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }
}
