package com.example.walpaper_deneme03;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private EditText sifre, kullanici_adi;
    private TextView sifremi_unuttum;
    private Button btnGiris_yap, btnkayit_ol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login); // Yeni XML dosyasını buna göre isimlendir

        initComponenets();
        registerEventHandlers();
        setupTextWatchers();
    }

    private void initComponenets() {
        kullanici_adi = findViewById(R.id.kullanici_adi);
        sifre = findViewById(R.id.sifre);
        btnGiris_yap = findViewById(R.id.btnGiris_yap);
        btnkayit_ol = findViewById(R.id.btnkayit_ol);
        sifremi_unuttum = findViewById(R.id.sifremi_unuttum);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser mevcutKullanici = mAuth.getCurrentUser();
        updateUI(mevcutKullanici);
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            if (user.isEmailVerified()) {
                Toast.makeText(this, user.getEmail() + R.string.logged_in, Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, FavoritesActivity.class));
                finish();
            } else {
                Toast.makeText(this, R.string.not_correct_email, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void registerEventHandlers() {
        btnkayit_ol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String kullanici = kullanici_adi.getText().toString();
                String sifresi = sifre.getText().toString();
                mAuth.createUserWithEmailAndPassword(kullanici, sifresi).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(LoginActivity.this, R.string.should_correct_mail, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }
                            updateUI(user);
                        } else {
                            Log.w(TAG, "Kayıt başarısız", task.getException());
                            Toast.makeText(LoginActivity.this, R.string.register_failed, Toast.LENGTH_LONG).show();
                            updateUI(null);
                        }
                    }
                });
            }
        });
        sifremi_unuttum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetPassword();
            }
        });

        btnGiris_yap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String kullanici = kullanici_adi.getText().toString();
                String sifresi = sifre.getText().toString();
                if (kullanici.isEmpty() || sifresi.isEmpty()) {
                    Toast.makeText(LoginActivity.this, R.string.enter_email_and_password, Toast.LENGTH_LONG).show();
                    return;
                }
                mAuth.signInWithEmailAndPassword(kullanici, sifresi).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                Intent intent = new Intent(LoginActivity.this, FavoritesActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, R.string.not_correct_email, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.w(TAG, "Giriş başarısız", task.getException());
                            Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

    }

    private void resetPassword() {
        String email = kullanici_adi.getText().toString().trim();
        if (email.isEmpty()) {
            kullanici_adi.setError(getString(R.string.enter_email));
            return;
        }
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(LoginActivity.this, R.string.should_correct_mail, Toast.LENGTH_LONG).show();
                    }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginActivity.this, R.string.someting_happened, Toast.LENGTH_LONG).show();
                    }
                });


    }


    private void setupTextWatchers() {
        kullanici_adi.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String email = kullanici_adi.getText().toString().trim();
                if (!isMailValid(email)) {
                    kullanici_adi.setError(getString(R.string.invalid_username));
                    btnkayit_ol.setVisibility(View.INVISIBLE);
                } else {
                    kullanici_adi.setError(null);
                    kontrolEtVeButonGoster();
                }

            }
        });
        sifre.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString().trim();

                if (!isPasswordValid(password)) {
                    sifre.setError(getString(R.string.invalid_password));
                    btnkayit_ol.setVisibility(View.INVISIBLE);
                } else {
                    sifre.setError(null);
                    kontrolEtVeButonGoster();
                }
            }
        });
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8 && password.matches(".*[!@#$%^&*+=?.].*");
    }
    private boolean isMailValid(String email) {
        // Mailin geçerli olup olmadığını kontrol etmek için regex kullanıyoruz
        String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.com$";
        return email.matches(emailPattern);
    }

    private void kontrolEtVeButonGoster() {
        String kullaniciAdi = kullanici_adi.getText().toString().trim();
        String password = sifre.getText().toString().trim();
        if (isMailValid(kullaniciAdi) && isPasswordValid(password)) {
            btnkayit_ol.setVisibility(View.VISIBLE);
        } else {
            btnkayit_ol.setVisibility(View.INVISIBLE);
        }
    }
}
