package com.example.spacer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etLogin, etPassword;
    private Button btnLogin, btnGoToRegister;
    private ImageButton btnClose;
    private CheckBox cbRememberMe;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "UserPrefs"; // zgodnie z AccountActivity
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        btnClose = findViewById(R.id.btnClose);
        cbRememberMe = findViewById(R.id.cbRememberMe); // ============ czesc nowa // ===================

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Wczytanie zapisanych danych przy starcie
        String savedLogin = sharedPreferences.getString(KEY_USERNAME, "");
        String savedPass = sharedPreferences.getString(KEY_PASSWORD, "");
        boolean isRemembered = !savedLogin.isEmpty() && !savedPass.isEmpty();

        if (isRemembered) {
            etLogin.setText(savedLogin);
            etPassword.setText(savedPass);
            cbRememberMe.setChecked(true);
        }

        // Zamknięcie aplikacji
        btnClose.setOnClickListener(v -> finishAffinity());
<<<<<<< Updated upstream
        // ------------------------------------------

        // ============ czesc nowa // ===================
        // SharedPreferences dla zapamiętywania loginu
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        String savedLogin = prefs.getString("login", "");
        String savedPass = prefs.getString("password", "");
        boolean isRemembered = prefs.getBoolean("rememberMe", false);

        etLogin.setText(isRemembered ? savedLogin : "");
        etPassword.setText(isRemembered ? savedPass : "");
        cbRememberMe.setChecked(isRemembered);
        // ================================================================



=======
>>>>>>> Stashed changes

        // Logowanie
        btnLogin.setOnClickListener(v -> {
            String loginInput = etLogin.getText().toString().trim();
            String passInput = etPassword.getText().toString();

<<<<<<< Updated upstream
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
=======
            if (loginInput.isEmpty() || passInput.isEmpty()) {
                showToast("Wypełnij wszystkie pola!");
>>>>>>> Stashed changes
                return;
            }

            String storedLogin = sharedPreferences.getString(KEY_USERNAME, "");
            String storedPass = sharedPreferences.getString(KEY_PASSWORD, "");

<<<<<<< Updated upstream

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

                // ============ czesc nowa // ===================
                // Zapis loginu jeśli checkbox zaznaczony
                SharedPreferences.Editor editor = prefs.edit();
=======
            if (loginInput.equals(storedLogin) && passInput.equals(storedPass)) {
                // LOGIN POPRAWNY
                SharedPreferences.Editor editor = sharedPreferences.edit();
>>>>>>> Stashed changes
                if (cbRememberMe.isChecked()) {
                    editor.putString(KEY_USERNAME, loginInput);
                    editor.putString(KEY_PASSWORD, passInput);
                } else {
                    editor.remove(KEY_USERNAME);
                    editor.remove(KEY_PASSWORD);
                }
                editor.apply();

<<<<<<< Updated upstream
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
=======
                showToast("Logowanie udane!");
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                showToast("Niepoprawny login lub hasło!");
>>>>>>> Stashed changes
            }
            // ------------------------------------------

        });

<<<<<<< Updated upstream

        //Nazwa przycisku: PRZEJDŹ DO REJESTRACJI | CEL: REJESTRACJA
=======
        // Przejście do rejestracji
>>>>>>> Stashed changes
        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

<<<<<<< Updated upstream


=======
    // Metoda do wyświetlania custom toasta
    private void showToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, null);
        TextView text = layout.findViewById(R.id.text_toast);
        text.setText(message);
        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
>>>>>>> Stashed changes
    }
}
