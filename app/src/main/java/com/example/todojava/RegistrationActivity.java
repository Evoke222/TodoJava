package com.example.todojava;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.todojava.utils.RegistrationManager;
import com.example.todojava.utils.UserImageSelector;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.Random;

import java.io.File;

public class RegistrationActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputEditText emailConfirmEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText usernameEditText;

    private Button registerButton;
    private ImageView profileImageView;
    private TextView loginLinkTextView;

    private FirebaseAuth auth;
    private UserImageSelector userImageSelector;

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_DARK_MODE = "is_dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Apply theme preference immediately before super.onCreate
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            Log.i("RegistrationActivity", "User already signed in, navigating to FeedActivity");
            Intent intent = new Intent(RegistrationActivity.this, FeedActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initializeViews();

        userImageSelector = new UserImageSelector(this, profileImageView);
        profileImageView.setOnClickListener(v -> userImageSelector.showImageSourceDialog());

        registerButton.setOnClickListener(v -> registerButtonClick());
        loginLinkTextView.setOnClickListener(v -> {
            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void initializeViews() {
        TextInputLayout usernameInputLayout = findViewById(R.id.textFieldUsername);
        TextInputLayout emailInputLayout = findViewById(R.id.textFieldEmail);
        TextInputLayout emailConfirmInputLayout = findViewById(R.id.textFieldEmailConfirm);
        TextInputLayout passwordInputLayout = findViewById(R.id.textFieldPassword);

        usernameEditText = (TextInputEditText) usernameInputLayout.getEditText();
        emailEditText = (TextInputEditText) emailInputLayout.getEditText();
        emailConfirmEditText = (TextInputEditText) emailConfirmInputLayout.getEditText();
        passwordEditText = (TextInputEditText) passwordInputLayout.getEditText();

        registerButton = findViewById(R.id.buttonRegister);
        profileImageView = findViewById(R.id.iv_profile_picture);
        loginLinkTextView = findViewById(R.id.textViewLoginLink);
    }

    private void registerButtonClick() {
        String email = emailEditText.getText().toString().trim();
        String emailConfirm = emailConfirmEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();

        if (email.isEmpty() || emailConfirm.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.equals(emailConfirm)) {
            Toast.makeText(this, "Email addresses do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        File imageFile = userImageSelector.createImageFile();

        RegistrationManager registrationManager = new RegistrationManager(RegistrationActivity.this);
        registrationManager.startRegistration(
                email,
                password,
                username,
                imageFile,
                new RegistrationManager.OnResultCallback(){
                    @Override
                    public void onResult(boolean success, String message) {
                        if (success) {
                            Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegistrationActivity.this, FeedActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(RegistrationActivity.this, "Registration failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
