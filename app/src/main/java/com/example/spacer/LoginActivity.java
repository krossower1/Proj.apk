package com.example.spacer;

import android.content.Intent;
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
import android.content.SharedPreferences;

/**
 * LoginActivity handles user authentication.
 * It provides a user interface for logging in, with options to remember credentials and navigate to the registration screen.
 */
public class LoginActivity extends AppCompatActivity {

    // UI elements
    private EditText etLogin, etPassword;
    private CheckBox cbRememberMe;
    // Database helper for user authentication
    private DatabaseHelper dbHelper;

    /**
     * Called when the activity is first created.
     * This is where you should do all of your normal static set up: create views, bind data to lists, etc.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI elements by finding them in the layout
        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoToRegister = findViewById(R.id.btnGoToRegister);
        ImageButton btnClose = findViewById(R.id.btnClose);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        // Initialize the database helper
        dbHelper = new DatabaseHelper(this);

        // Set an OnClickListener for the close button to exit the app
        btnClose.setOnClickListener(v -> finishAffinity());

        // Get shared preferences for storing "Remember me" data
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        String savedLogin = prefs.getString("login", "");
        String savedPass = prefs.getString("password", "");
        boolean isRemembered = prefs.getBoolean("rememberMe", false);

        // If "Remember me" was checked previously, populate the login and password fields
        etLogin.setText(isRemembered ? savedLogin : "");
        etPassword.setText(isRemembered ? savedPass : "");
        cbRememberMe.setChecked(isRemembered);

        // Set an OnClickListener for the login button
        btnLogin.setOnClickListener(v -> {
            String login = etLogin.getText().toString().trim();
            String pass = etPassword.getText().toString();

            // Check if login or password fields are empty
            if (login.isEmpty() || pass.isEmpty()) {
                // Show a custom error toast if fields are empty
                showToast(getString(R.string.no_field), true);
                return;
            }

            // Check user credentials against the database
            boolean valid = dbHelper.checkUser(login, pass);

            if (valid) {
                // Show a custom success toast on successful login
                showToast(getString(R.string.login_successful), false);

                // Save or clear credentials based on the "Remember me" checkbox
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

                // Retrieve user data after successful login
                String waga = dbHelper.getWaga(login, pass);
                int userId = dbHelper.getUserId(login, pass);

                // Navigate to the MainActivity, passing user data
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("waga", waga);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish(); // Finish LoginActivity so the user can't navigate back to it
            } else {
                // Show a custom error toast for invalid credentials
                showToast(getString(R.string.login_failed), true);
                return;
            }
        });

        // Set an OnClickListener for the "Go to Register" button
        btnGoToRegister.setOnClickListener(v -> {
            // Navigate to the RegisterActivity
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish(); // Finish LoginActivity
        });
    }

    /**
     * Displays a custom toast message.
     * @param message The message to be displayed.
     * @param isError True if the toast should have an error background, false otherwise.
     */
    private void showToast(String message, boolean isError) {
        LayoutInflater inflater = getLayoutInflater();
        // Inflate the custom toast layout.
        View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));

        // Set a background resource for the toast based on whether it is an error or not.
        if (isError) {
            layout.setBackgroundResource(R.drawable.toast_error_background);
        }

        // Set the text of the toast message.
        TextView text = layout.findViewById(R.id.text_toast);
        text.setText(message);

        // Create and show the toast.
        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}
