package com.example.cnam_go;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText textLogin, textPassword;

    private Button btnRegister;

    private TextView tvBackToLogin;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = FirebaseFirestore.getInstance();

        textLogin = findViewById(R.id.textLogin);
        textPassword = findViewById(R.id.textPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnRegister.setOnClickListener(v -> registerUser());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String login = textLogin.getText().toString().trim();
        String password = textPassword.getText().toString().trim();

        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Le mot de passe doit avoir au moins 6 caractères", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("User")
                .whereEqualTo("Login", login)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(RegisterActivity.this, "Ce login est déjà utilisé", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String currentDate = sdf.format(new java.util.Date());

                    Map<String, Object> user = new HashMap<>();
                    user.put("Login", login);
                    user.put("Password", password);
                    user.put("Money", 1500L);
                    user.put("DailyDate", "");
                    user.put("CreationDate", currentDate);

                    db.collection("User")
                            .add(user)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(RegisterActivity.this, "Compte créé avec succès !", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(RegisterActivity.this, "Erreur lors de la création : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Erreur de vérification : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
