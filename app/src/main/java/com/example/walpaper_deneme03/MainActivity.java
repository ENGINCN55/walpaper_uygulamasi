package com.example.walpaper_deneme03;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main); // İstersen burada basit bir splash ekran tasarımı koyabilirsin

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null && user.isEmailVerified()) {
            // Kullanıcı giriş yapmış ve maili doğrulanmış
            startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
        } else {
            // Kullanıcı yoksa veya mail doğrulanmamışsa login ekranına yolla
            Toast.makeText(this, R.string.not_correct_email, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }

        finish(); // Bu activity'i kapatıyoruz, geri gelinmesin
    }
}
