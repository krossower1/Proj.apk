package com.example.spacer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.ImageButton;


import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etLogin, etPassword, etRepeatPassword;
    private Button btnRegister, btnGoToLogin;

    // Dodanie bazy danych
    // ------------------------------------------
    private DatabaseHelper dbHelper;
    // ------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        ImageButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finishAffinity());

        // Inicjalizacja bazy danych
        // ------------------------------------------
        dbHelper = new DatabaseHelper(this);
        // ------------------------------------------

        // ZAMKNIECIE APLIKACJI
        btnClose.setOnClickListener(v -> finishAffinity());

        // Obsługa kliknięcia przycisku "Zarejestruj się"
        btnRegister.setOnClickListener(v -> {
            String login = etLogin.getText().toString().trim();
            String pass = etPassword.getText().toString();
            String repeat = etRepeatPassword.getText().toString();

            if (login.isEmpty() || pass.isEmpty() || repeat.isEmpty()) {
                // CUSTOM TOAST
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText("Wypełnij wszystkie pola!");

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                // ----------------------------
                return;
            }

            if (!pass.equals(repeat)) {
                // CUSTOM TOAST
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText("Hasła nie są takie same!");

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                // ----------------------------
                return;
            }






            // ZAPISZ DANE DO BAZY
            // ------------------------------------------
            boolean inserted = dbHelper.addUser(login, pass);
            if (inserted) {
                // ----------------------------
                // 🔹 CUSTOM TOAST
                // ----------------------------
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText("Rejestracja zakończona sukcesem!");

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                // ----------------------------

                // Przejście do ekranu logowania
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // kończymy ekran rejestracji
            } else {
                // ----------------------------
                // CUSTOM TOAST: użytkownik już istnieje
                // ----------------------------
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText("Błąd: użytkownik już istnieje!");

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
            }
            // ------------------------------------------

        });

        btnGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

    }
}
