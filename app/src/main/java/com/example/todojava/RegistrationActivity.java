package com.example.todojava;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.todojava.utils.RegistrationManager;
import com.example.todojava.utils.UserImageSelector; // Your existing import
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

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

    // The UserImageSelector will manage everything related to image picking.
    private UserImageSelector userImageSelector;

    // --- NO LAUNCHERS NEEDED HERE ---


    // The UserImageSelector handles them internally.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        initializeViews();

        // --- THIS IS THE CORRECT, SIMPLIFIED IMPLEMENTATION ---
        // 1. Initialize UserImageSelector. It automatically registers its own launchers.
        userImageSelector = new UserImageSelector(this, profileImageView);

        // 2. Set the click listener to call the simple `showImageSourceDialog` method.
        profileImageView.setOnClickListener(v -> userImageSelector.showImageSourceDialog());
        // --- End of fix ---

        // Set other listeners
        registerButton.setOnClickListener(v -> registerButtonClick());
        loginLinkTextView.setOnClickListener(v -> {
            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // --- onActivityResult is NOT needed and should remain deleted ---

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
        Log.d(TAG, "Register button clicked");

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

        // --- FIX: This method name is different in your UserImageSelector ---
        // Change from getSelectedImageFile() to createImageFile()
        File imageFile = userImageSelector.createImageFile();

        // Check if an image was selected, though your registration manager might handle null
        if (imageFile == null) {
            Log.w(TAG, "No profile image was selected by the user.");
            // You might want to show a toast here, but for now we'll proceed
        }

        RegistrationManager registrationManager = new RegistrationManager(RegistrationActivity.this);

        // --- THIS IS THE LINE TO FIX ---
        // The original call was missing the 'username' argument.
        registrationManager.startRegistration(
                email,
                password,
                username,   // <-- ADD THE USERNAME VARIABLE HERE
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