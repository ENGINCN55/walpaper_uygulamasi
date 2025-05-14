package com.example.walpaper_deneme03;

import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class walpaper_page extends AppCompatActivity {

    private EditText searchEditText;
    private RecyclerView recyclerView;
    private PhotoAdapter photoAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walpaper_page);

        searchEditText = findViewById(R.id.searchEditText);
        recyclerView = findViewById(R.id.photosRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        photoAdapter = new PhotoAdapter();
        recyclerView.setAdapter(photoAdapter);



        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // Enter tuşuna basıldığında veya 'done' işareti verildiğinde tetiklenir
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = v.getText().toString().replaceAll("\\s+", ""); // boşlukları sil
                    if (!query.isEmpty()) {
                        fetchPhotos(query); // API'ye yollanacak veri
                    }
                    return true; // işlem tamamlandı
                }
                return false;
            }
        });


        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                String photoUrl = photoAdapter.getPhotoAt(position);

                if (direction == ItemTouchHelper.LEFT) {
                    downloadImageAndSaveAsJpg(photoUrl);
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.isEmailVerified()) {
                        saveFavoriteImageToFirebase(photoUrl);
                    } else {
                        Toast.makeText(walpaper_page.this, "user null geliyo", Toast.LENGTH_SHORT).show();
                    }


                } else if (direction == ItemTouchHelper.RIGHT) {
                    showWallpaperDialog(photoUrl);
                }

                photoAdapter.notifyItemChanged(position);
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(walpaper_page.this, FavoritesActivity.class);
        startActivity(intent);
        finish();
    }




    private void showWallpaperDialog(String imageUrl) {
        new AlertDialog.Builder(this)
                .setTitle("Duvar Kağıdı Olarak Ayarla")
                .setMessage("Bu görseli duvar kağıdı olarak ayarlamak istiyor musun moruk?")
                .setPositiveButton("Evet", (dialog, which) -> setWallpaper(imageUrl))
                .setNegativeButton("Hayır", null)
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
                            Toast.makeText(walpaper_page.this, "Arka plan yapıldı kral!", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(walpaper_page.this, "Duvar kağıdı ayarlanamadı aq", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }
    private void saveFavoriteImageToFirebase(String imageUrl) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userFavoritesRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("favorites");

        // Önce, bu görselin zaten eklenip eklenmediğini kontrol et
        userFavoritesRef.orderByValue().equalTo(imageUrl).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Eğer URL zaten varsa, kullanıcıyı bilgilendir
                    Toast.makeText(walpaper_page.this, "Bu görsel zaten favorilerinde!", Toast.LENGTH_SHORT).show();
                } else {
                    String imageId = "fav_" + System.currentTimeMillis();
                    userFavoritesRef.child(imageId).setValue(imageUrl)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(walpaper_page.this, "Favorilere eklendi!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(walpaper_page.this, "Favoriye eklerken bir hata oluştu", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(walpaper_page.this, "Veri alınamadı!", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void downloadImageAndSaveAsJpg(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Favoriler");
                        if (!directory.exists()) directory.mkdirs();

                        File file = new File(directory, "fav_" + System.currentTimeMillis() + ".jpg");

                        try (FileOutputStream out = new FileOutputStream(file)) {
                            resource.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            out.flush();
                            Toast.makeText(walpaper_page.this, "Favorilere kaydettim moruk", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(walpaper_page.this, "Kaydederken çuvalladık aq", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
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
            @Override public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(walpaper_page.this, "API patladı moruk", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(okhttp3.Call call, Response response) throws IOException {
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
}
