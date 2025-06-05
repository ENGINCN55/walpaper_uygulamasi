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
    private DatabaseReference userFavoritesRef;
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
        likesRef = FirebaseDatabase.getInstance().getReference("likes").child(currentUser.getUid());
        userFavoritesRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid()).child("favorites");
        //fetchLikedImages();
        fetchPhotosFromFirebase();



        adapter.setOnItemLongClickListener(position -> {
            if (position >= 0 && position < likedImageUrls.size()) { // Güvenlik kontrolü
                String imageUrl = likedImageUrls.get(position);
                likeImage(imageUrl);
            }
        });

        adapter.setOnItemSwipeListener(position -> {
            String imageUrl = likedImageUrls.get(position);
            saveFavoriteImageToFirebase(imageUrl);

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
                if (position >= 0 && position < likedImageUrls.size()) { // Güvenlik kontrolü
                    if (adapter.swipeListener != null) {
                        adapter.swipeListener.onItemSwiped(position);
                    }
                }
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

    private void likeImage(String imageUrl) {
        String photoKey = imageUrl.hashCode() + "";

        DatabaseReference photoLikesRef = FirebaseDatabase.getInstance()
                .getReference("photoLikes")
                .child(photoKey);

        photoLikesRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer currentValue = null;
                if (currentData.hasChild("likeCount"))
                    currentValue = currentData.child("likeCount").getValue(Integer.class);

                if (currentValue == null) {
                    currentData.child("likeCount").setValue(1);
                    currentData.child("url").setValue(imageUrl);
                } else {
                    currentData.child("likeCount").setValue(currentValue + 1);
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (committed) {
                    Toast.makeText(WallpaperNetActivity.this, R.string.liked, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(WallpaperNetActivity.this, R.string.someting_happened, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void fetchPhotosFromFirebase() {
        DatabaseReference photoLikesRef = FirebaseDatabase.getInstance().getReference("photoLikes");
        photoLikesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> photoUrls = new ArrayList<>();
                for (DataSnapshot photoSnapshot : snapshot.getChildren()) {
                    String url = photoSnapshot.child("url").getValue(String.class);
                    if (url != null) {
                        photoUrls.add(url);
                    }
                }
                updatePhotos(photoUrls);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // error handling burada yapılabilir
            }
        });
    }
    public void updatePhotos(List<String> newPhotoUrls) {
        this.likedImageUrls = newPhotoUrls;
        adapter.updatePhotos(newPhotoUrls);
    }



    private void saveFavoriteImageToFirebase(String imageUrl) {
        userFavoritesRef.orderByValue().equalTo(imageUrl).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(WallpaperNetActivity.this, R.string.already_favorites, Toast.LENGTH_SHORT).show();
                } else {
                    String imageId = "fav_" + System.currentTimeMillis();
                    userFavoritesRef.child(imageId).setValue(imageUrl)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    downloadImageAndSaveToGallery(imageUrl);
                                    Toast.makeText(WallpaperNetActivity.this, R.string.get_favorites, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(WallpaperNetActivity.this, R.string.someting_happened, Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WallpaperNetActivity.this, R.string.someting_happened, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadImageAndSaveToGallery(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyWallpapers");
                        if (!directory.exists()) directory.mkdirs();

                        File file = new File(directory, "wallpaper_" + System.currentTimeMillis() + ".jpg");
                        try (FileOutputStream out = new FileOutputStream(file)) {
                            resource.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            Toast.makeText(WallpaperNetActivity.this, R.string.get_walpaper, Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(WallpaperNetActivity.this, R.string.someting_happened, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }
}