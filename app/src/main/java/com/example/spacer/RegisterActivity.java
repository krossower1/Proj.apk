package com.example.spacer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etLogin, etPassword, etRepeatPassword, etWaga;

    // Dodanie bazy danych
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Button btnRegister, btnGoToLogin;

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        etWaga = findViewById(R.id.etWaga);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        ImageButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finishAffinity());

        // Inicjalizacja bazy danych
        dbHelper = new DatabaseHelper(this);

        // ZAMKNIECIE APLIKACJI
        btnClose.setOnClickListener(v -> finishAffinity());

        // Obsługa kliknięcia przycisku "Zarejestruj się"
        btnRegister.setOnClickListener(v -> {
            String login = etLogin.getText().toString().trim();
            String pass = etPassword.getText().toString();
            String repeat = etRepeatPassword.getText().toString();
            String waga = etWaga.getText().toString().trim();

            if (login.isEmpty() || pass.isEmpty() || repeat.isEmpty() || waga.isEmpty()) {
                // CUSTOM TOAST
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.register_container));
                layout.setBackgroundResource(R.drawable.toast_error_background);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.no_field));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                return;
            }

            if (!pass.equals(repeat)) {
                // CUSTOM TOAST
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.register_container));
                layout.setBackgroundResource(R.drawable.toast_error_background);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.passwords_not_matching));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                return;
            }

            // ZAPISZ DANE DO BAZY
            boolean inserted = dbHelper.addUser(login, pass, waga);
            if (inserted) {
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.register_container));

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.registration_successful));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                // Przejście do ekranu logowania
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                // CUSTOM TOAST: użytkownik już istnieje
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.register_container));
                layout.setBackgroundResource(R.drawable.toast_error_background);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.user_already_exists));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
            }

        });

        btnGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

    }
}
