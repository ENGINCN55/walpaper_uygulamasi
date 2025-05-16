package com.example.walpaper_deneme03;

import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FavoritesAdapter adapter;
    private List<File> favoriteImages;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        setupBottomNavigation();

        recyclerView = findViewById(R.id.favoritesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        favoriteImages = getFavoriteImages();

        if (favoriteImages.isEmpty()) {
            Toast.makeText(this, "Hiç favori kaydetmemişsin moruk!", Toast.LENGTH_SHORT).show();
        }

        adapter = new FavoritesAdapter(this, favoriteImages);
        recyclerView.setAdapter(adapter);
    }
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Burada doğru item ID'sini seçiyoruz
        bottomNav.setSelectedItemId(R.id.nav_favorites);

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {  // Ana Sayfa
                startActivity(new Intent(FavoritesActivity.this, walpaper_page.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_favorites) {  // Favoriler
                return true;  // Burada hiç bir şey yapmana gerek yok, çünkü zaten aktif.
            } else if (item.getItemId() == R.id.nav_settings) {  // Ayarlar
                startActivity(new Intent(FavoritesActivity.this, ayarlar.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_walpapernet) {  // Walaper Net) {
                startActivity(new Intent(FavoritesActivity.this, WallpaperNetActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }



    private List<File> getFavoriteImages() {
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Favoriler");
        if (!directory.exists() || !directory.isDirectory()) return new ArrayList<>();

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"));
        if (files != null) {
            return new ArrayList<>(Arrays.asList(files));
        } else {
            return new ArrayList<>();
        }
    }
}
