package com.example.spacer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.LayoutInflater; // Libraries used for custom toast
import android.view.View; // --
import android.widget.TextView; // --
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageButton;
import android.content.SharedPreferences; // Import for checkbox
import android.widget.CheckBox; // Import for checkbox



public class LoginActivity extends AppCompatActivity {

    private EditText etLogin, etPassword;
    private Button btnLogin, btnGoToRegister;
    private ImageButton btnClose;
    private CheckBox cbRememberMe; // Variable for "Remember me" checkbox
    // Database helper
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI elements
        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        btnClose = findViewById(R.id.btnClose);
        cbRememberMe = findViewById(R.id.cbRememberMe);



        // Database initialization
        dbHelper = new DatabaseHelper(this);


        // Close the application
        btnClose.setOnClickListener(v -> finishAffinity());

        // SharedPreferences for remembering login credentials
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        String savedLogin = prefs.getString("login", "");
        String savedPass = prefs.getString("password", "");
        boolean isRemembered = prefs.getBoolean("rememberMe", false);

        // Set saved credentials if "Remember me" was checked
        etLogin.setText(isRemembered ? savedLogin : "");
        etPassword.setText(isRemembered ? savedPass : "");
        cbRememberMe.setChecked(isRemembered);


        // Listener for the login button
        btnLogin.setOnClickListener(v -> {
            String login = etLogin.getText().toString().trim();
            String pass = etPassword.getText().toString();

            // Check if fields are empty
            if (login.isEmpty() || pass.isEmpty()) {
                // Show custom error toast
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);
                layout.setBackgroundResource(R.drawable.toast_error_background);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.no_field));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                return;
            }



            // Verify login and password with the database
            boolean valid = dbHelper.checkUser(login, pass);

            if (valid) {
                // Show custom success toast
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.login_successful));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                // Save login credentials if checkbox is checked
                SharedPreferences.Editor editor = prefs.edit();
                if (cbRememberMe.isChecked()) {
                    editor.putString("login", login);
                    editor.putString("password", pass);
                    editor.putBoolean("rememberMe", true);
                } else {
                    // Clear saved credentials
                    editor.remove("login");
                    editor.remove("password");
                    editor.putBoolean("rememberMe", false);
                }
                editor.apply();

                String waga = dbHelper.getWaga(login, pass);
                int userId = dbHelper.getUserId(login, pass);

                // Go to the main screen (MainActivity)
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("waga", waga);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();
            } else {
                // Show custom error toast for failed login
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);
                layout.setBackgroundResource(R.drawable.toast_error_background);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.login_failed));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                return;
            }
        });

        // Button: GO TO REGISTRATION | TARGET: REGISTRATION
        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

    }
}
