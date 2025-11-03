package com.example.spacer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.LayoutInflater; //biblioteki użyte do custom toast
import android.view.View; // --
import android.widget.TextView; // --
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etLogin, etPassword;
    private Button btnLogin, btnExit, btnGoToRegister;

    // Dodanie bazy danych
    // ------------------------------------------
    private DatabaseHelper dbHelper;
    // ------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnExit = findViewById(R.id.btnExit);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);

        // Inicjalizacja bazy danych
        // ------------------------------------------
        dbHelper = new DatabaseHelper(this);
        // ------------------------------------------

        btnLogin.setOnClickListener(v -> {
            String login = etLogin.getText().toString().trim();
            String pass = etPassword.getText().toString();

            if (login.isEmpty() || pass.isEmpty()) {
                // ----------------------------
                // 🔹 CUSTOM TOAST DLA BŁĘDNYCH DANYCH
                // ----------------------------
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




            // ------------------------------------------
            // WERYFIKACJA LOGINU I HASŁA
            // ------------------------------------------
            boolean valid = dbHelper.checkUser(login, pass);

            if (valid) {
                // ----------------------------
                // CUSTOM TOAST
                // ----------------------------
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText("Zalogowano pomyślnie!");

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                // ----------------------------

                // 🔹 Przejście do ekranu głównego (MainActivity)
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            } else { // CUSTOM TOAST
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText("Błędny login lub hasło!");

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                // ----------------------------
                return;
            }
            // ------------------------------------------

        });


        //Nazwa przycisku: PRZEJDŹ DO REJESTRACJI | CEL: REJESTRACJA
        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        btnExit.setOnClickListener(v -> finishAffinity());
    }
}
