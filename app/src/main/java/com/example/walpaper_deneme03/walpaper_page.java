package com.example.walpaper_deneme03;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class walpaper_page extends AppCompatActivity {

    private EditText searchEditText;
    private RecyclerView recyclerView;
    private PhotoAdapter photoAdapter;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private PermissionUtil permissionUtil;
    private String pendingPhotoUrlForFavorite = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walpaper_page);

        permissionUtil = new PermissionUtil();

        searchEditText = findViewById(R.id.searchEditText);
        recyclerView = findViewById(R.id.photosRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        photoAdapter = new PhotoAdapter();
        recyclerView.setAdapter(photoAdapter);

        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = v.getText().toString().replaceAll("\\s+", "");
                    if (!query.isEmpty()) {
                        fetchPhotos(query);
                    }
                    return true;
                }
                return false;
            }
        });

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                String photoUrl = photoAdapter.getPhotoAt(position);
                if (direction == ItemTouchHelper.LEFT) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.isEmailVerified()) {
                        checkAndRequestStoragePermission(photoUrl);
                    } else {
                        Toast.makeText(walpaper_page.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                    }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    showWallpaperDialog(photoUrl);
                }
                photoAdapter.notifyItemChanged(position);
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);

        photoAdapter.setOnItemLongClickListener(new PhotoAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(String photoUrl) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(walpaper_page.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                    return;
                }

                String userId = user.getUid();
                String photoKey = Base64.encodeToString(photoUrl.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

                DatabaseReference likesRef = FirebaseDatabase.getInstance()
                        .getReference("Likes")
                        .child(userId);

                DatabaseReference photoLikesRef = FirebaseDatabase.getInstance()
                        .getReference("photoLikes")
                        .child(photoKey);

                likesRef.child(photoKey).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            likesRef.child(photoKey).setValue(true);

                            photoLikesRef.runTransaction(new Transaction.Handler() {
                                @NonNull
                                @Override
                                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                    Integer currentValue = null;
                                    if (currentData.hasChild("likeCount"))
                                        currentValue = currentData.child("likeCount").getValue(Integer.class);

                                    if (currentValue == null) {
                                        currentData.child("likeCount").setValue(1);
                                        currentData.child("url").setValue(photoUrl);
                                    } else {
                                        currentData.child("likeCount").setValue(currentValue + 1);
                                    }
                                    return Transaction.success(currentData);
                                }

                                @Override
                                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                                    if (committed) {
                                        Toast.makeText(walpaper_page.this, R.string.liked, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(walpaper_page.this, R.string.someting_happened, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(walpaper_page.this, R.string.already_like, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        });
    }

    private void checkAndRequestStoragePermission(String photoUrl) {
        if (permissionUtil.isPermissionGranted(this)) {
            saveFavoriteImageToFirebase(photoUrl);
        } else {
            pendingPhotoUrlForFavorite = photoUrl;
            showStoragePermissionDialog();
        }
    }

    private void showStoragePermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.storage_permission_granted)
                .setMessage(R.string.do_you_wanna_grant_storage_permission_granted)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    permissionUtil.setPermissionGranted(walpaper_page.this, true);
                    Toast.makeText(walpaper_page.this, R.string.permission_granted, Toast.LENGTH_SHORT).show();

                    if (pendingPhotoUrlForFavorite != null) {
                        saveFavoriteImageToFirebase(pendingPhotoUrlForFavorite);
                        pendingPhotoUrlForFavorite = null;
                    }
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    permissionUtil.setPermissionGranted(walpaper_page.this, false);
                    Toast.makeText(walpaper_page.this, R.string.permission_not_granted, Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionUtil.setPermissionGranted(this, true);
                Toast.makeText(this, R.string.permission_already_granted, Toast.LENGTH_SHORT).show();
                if (pendingPhotoUrlForFavorite != null) {
                    saveFavoriteImageToFirebase(pendingPhotoUrlForFavorite);
                    pendingPhotoUrlForFavorite = null;
                }
            } else {
                permissionUtil.setPermissionGranted(this, false);
                Toast.makeText(this, R.string.permission_not_granted, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(walpaper_page.this, FavoritesActivity.class);
        startActivity(intent);
        finish();
    }

    public class PermissionUtil {
        private static final String PREFS_NAME = "app_prefs";
        private static final String KEY_PERMISSION_GRANTED = "storage_permission_granted";

        public void setPermissionGranted(Context context, boolean granted) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_PERMISSION_GRANTED, granted).apply();
        }

        public boolean isPermissionGranted(Context context) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            }

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(KEY_PERMISSION_GRANTED, false);
        }
    }

    private void showWallpaperDialog(String imageUrl) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.makeit_walpaper)
                .setMessage(R.string.do_you_wanna_make_walpaper)
                .setPositiveButton(R.string.yes, (dialog, which) -> setWallpaper(imageUrl))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void setWallpaper(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        try {
                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(walpaper_page.this);
                            wallpaperManager.setBitmap(resource);
                            Toast.makeText(walpaper_page.this, R.string.get_walpaper, Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(walpaper_page.this, R.string.someting_happened, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }
    private void saveFavoriteImageToFirebase(String imageUrl) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userFavoritesRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("favorites");

        userFavoritesRef.orderByValue().equalTo(imageUrl).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(walpaper_page.this, R.string.already_favorites, Toast.LENGTH_SHORT).show();
                } else {
                    String imageId = "fav_" + System.currentTimeMillis();
                    userFavoritesRef.child(imageId).setValue(imageUrl)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    downloadImageAndSaveAsJpg(imageUrl);
                                    Toast.makeText(walpaper_page.this, R.string.get_favorites, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(walpaper_page.this, R.string.someting_happened, Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(walpaper_page.this, R.string.someting_happened, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadImageAndSaveAsJpg(String imageUrl) {
        if (!permissionUtil.isPermissionGranted(this)) {
            return;
        }else {

        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        try {
                            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyWallpapers");
                            if (!directory.exists()) {
                                directory.mkdirs();
                            }

                            String fileName = "img_" + System.currentTimeMillis() + ".jpg";
                            File file = new File(directory, fileName);
                            FileOutputStream out = new FileOutputStream(file);
                            resource.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            out.flush();
                            out.close();

                            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            mediaScanIntent.setData(Uri.fromFile(file));
                            sendBroadcast(mediaScanIntent);

                            Toast.makeText(walpaper_page.this, R.string.downloaded, Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(walpaper_page.this, R.string.someting_happened, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });}

    }


    private void fetchPhotos(final String query) {
        String url = "https://api.pexels.com/v1/search?query=" + query + "&per_page=10&page=1";
        String apiKey = "8WGcKgpsUe3Dt67w2pFcVZOF3FMK6p6qxgav6n5enW3YNZa9FdTLYRsZ";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", apiKey)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(walpaper_page.this, R.string.someting_happened, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        JSONArray photosArray = jsonResponse.getJSONArray("photos");
                        List<String> photoUrls = new ArrayList<>();

                        for (int i = 0; i < photosArray.length(); i++) {
                            JSONObject photo = photosArray.getJSONObject(i);
                            String imageUrl = photo.getJSONObject("src").getString("original");
                            photoUrls.add(imageUrl);
                        }

                        runOnUiThread(() -> photoAdapter.updatePhotos(photoUrls));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public String Base64(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
}