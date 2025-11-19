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
import android.widget.ImageButton;
import android.content.SharedPreferences; // import do checkbox
import android.widget.CheckBox; // import do checkbox



public class LoginActivity extends AppCompatActivity {

    private EditText etLogin, etPassword;
    private Button btnLogin, btnGoToRegister;
    private ImageButton btnClose;
    private CheckBox cbRememberMe; //zmienna do checkbox "Zapamiętaj mnie"
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
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        btnClose = findViewById(R.id.btnClose);
        cbRememberMe = findViewById(R.id.cbRememberMe);



        // Inicjalizacja bazy danych
        // ------------------------------------------
        dbHelper = new DatabaseHelper(this);
        // ------------------------------------------


        // ZAMKNIECIE APLIKACJI
        // ------------------------------------------
        btnClose.setOnClickListener(v -> finishAffinity());
        // ------------------------------------------

        // SharedPreferences dla zapamiętywania loginu
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        String savedLogin = prefs.getString("login", "");
        String savedPass = prefs.getString("password", "");
        boolean isRemembered = prefs.getBoolean("rememberMe", false);

        etLogin.setText(isRemembered ? savedLogin : "");
        etPassword.setText(isRemembered ? savedPass : "");
        cbRememberMe.setChecked(isRemembered);
        // ================================================================




        btnLogin.setOnClickListener(v -> {
            String login = etLogin.getText().toString().trim();
            String pass = etPassword.getText().toString();

            if (login.isEmpty() || pass.isEmpty()) {
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

                // Zapis loginu jeśli checkbox zaznaczony
                SharedPreferences.Editor editor = prefs.edit();
                if (cbRememberMe.isChecked()) {
                    editor.putString("login", login);
                    editor.putString("password", pass);
                    editor.putBoolean("rememberMe", true);
                } else {
                    editor.remove("login");
                    editor.remove("password");
                    editor.putBoolean("rememberMe", false);
                }
                editor.apply();
                // ================================================================

                String waga = dbHelper.getWaga(login, pass);

                // Przejście do ekranu głównego (MainActivity)
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("waga", waga);
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
                return;
            }
        });

        //Nazwa przycisku: PRZEJDŹ DO REJESTRACJI | CEL: REJESTRACJA
        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

    }
}
