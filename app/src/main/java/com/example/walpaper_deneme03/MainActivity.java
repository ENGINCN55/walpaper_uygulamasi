package com.example.walpaper_deneme03;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private EditText sifre, kullanici_adi;
    private Button btnGiris_yap, btnkayit_ol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        initComponenets();
        registerEventHandlers();
        setupTextWatchers(); // TextWatcher'ı burada başlatıyoruz
    }

    private void initComponenets() {
        kullanici_adi = findViewById(R.id.kullanici_adi);
        sifre = findViewById(R.id.sifre);
        btnGiris_yap = findViewById(R.id.btnGiris_yap);
        btnkayit_ol = findViewById(R.id.btnkayit_ol);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser mevcutKullanici = mAuth.getCurrentUser();
        updateUI(mevcutKullanici);
    }

    public void updateUI(FirebaseUser user) {
        if (user != null) {
            if (user.isEmailVerified()) {
                // Mail doğrulandı, giriş yapılabilir
                Toast.makeText(this, user.getEmail() + " giriş yaptı", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, FavoritesActivity.class);
                startActivity(intent);
            } else {
                // Mail doğrulanmamış, kullanıcıya doğrulama yapması için uyarı verilecek
                Toast.makeText(this, "Mailinizi doğrulayıp tekrar giriş yapın", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void registerEventHandlers() {
        btnkayit_ol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String kullanici = kullanici_adi.getText().toString();
                String sifresi = sifre.getText().toString();
                mAuth.createUserWithEmailAndPassword(kullanici, sifresi).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Mail doğrulama linki gönderiliyor
                                user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(MainActivity.this, "Mail doğrulama linki gönderildi. Lütfen mailinizi doğrulayın.", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }
                            updateUI(user);
                        } else {
                            Log.w(TAG, "Kayıt başarısız", task.getException());
                            Toast.makeText(MainActivity.this, "Kayıt başarısız", Toast.LENGTH_LONG).show();
                            updateUI(null);
                        }
                    }
                });
            }
        });

        btnGiris_yap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String kullanici = kullanici_adi.getText().toString();
                String sifresi = sifre.getText().toString();
                mAuth.signInWithEmailAndPassword(kullanici, sifresi).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                // Giriş başarılı ve mail doğrulanmış
                                Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
                                startActivity(intent);
                            } else {
                                // Mail doğrulanmamış
                                Toast.makeText(MainActivity.this, "Mailinizi doğruladıktan sonra tekrar giriş yapın", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.w(TAG, "Giriş başarısız", task.getException());
                            Toast.makeText(MainActivity.this, "Giriş başarısız", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    // TextWatcher ile şifre kontrolü
    private void setupTextWatchers() {
        sifre.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString().trim();
                if (!isPasswordValid(password)) {
                    sifre.setError("Şifre en az 8 karakter ve 1 özel karakter içermeli");
                    btnkayit_ol.setVisibility(View.INVISIBLE); // Kaydol butonunu gizle
                } else {
                    sifre.setError(null);
                    kontrolEtVeButonGoster(); // Butonun görünürlüğünü kontrol et
                }
            }
        });
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8 && password.matches(".*[!@#$%^&*+=?.].*");
    }

    private void kontrolEtVeButonGoster() {
        String kullaniciAdi = kullanici_adi.getText().toString().trim();
        String password = sifre.getText().toString().trim();

        // Mail ve şifre doğruysa Kaydol butonunu göster
        if (kullaniciAdi.length() >= 3 && isPasswordValid(password)) {
            btnkayit_ol.setVisibility(View.VISIBLE);
        } else {
            btnkayit_ol.setVisibility(View.INVISIBLE);
        }
    }
}
