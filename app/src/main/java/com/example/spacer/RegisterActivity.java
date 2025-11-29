package com.example.spacer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    // UI elements
    private EditText etLogin, etPassword, etRepeatPassword, etWaga;

    // Database helper
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Button btnRegister, btnGoToLogin;

        // Initialize UI elements
        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        etWaga = findViewById(R.id.etWaga);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        ImageButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finishAffinity());

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        // Close the application when the close button is clicked
        btnClose.setOnClickListener(v -> finishAffinity());

        // Handle the "Register" button click
        btnRegister.setOnClickListener(v -> {
            // Get user input from EditText fields
            String login = etLogin.getText().toString().trim();
            String pass = etPassword.getText().toString();
            String repeat = etRepeatPassword.getText().toString();
            String waga = etWaga.getText().toString().trim();

            // Check if any of the fields are empty
            if (login.isEmpty() || pass.isEmpty() || repeat.isEmpty() || waga.isEmpty()) {
                // Show a custom toast message if any field is empty
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

            // Check if the passwords match
            if (!pass.equals(repeat)) {
                // Show a custom toast message if the passwords don't match
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);
                layout.setBackgroundResource(R.drawable.toast_error_background);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.passwords_not_matching));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                return;
            }

            // Add user to the database
            boolean inserted = dbHelper.addUser(login, pass, waga);
            if (inserted) {
                // Show a custom toast message if registration is successful
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.registration_successful));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                // Go to the login screen
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                // Show a custom toast message if the user already exists
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);
                layout.setBackgroundResource(R.drawable.toast_error_background);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.user_already_exists));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
            }

        });

        // Handle the "Go to Login" button click
        btnGoToLogin.setOnClickListener(v -> {
            // Go to the login screen
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

    }
}
