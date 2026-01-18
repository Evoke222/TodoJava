package com.example.todojava;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.todojava.utils.RegistrationManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText; // Import this
import com.google.android.material.textfield.TextInputLayout;   // Import this
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;

public class RegistrationActivity extends AppCompatActivity {

    // Change these to TextInputEditText for type safety
    private TextInputEditText emailEditText;
    private TextInputEditText emailConfirmEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText usernameEditText;

    private Button registerButton;
    private ShapeableImageView profileImageView;
    private TextView loginLinkTextView;

    private FirebaseAuth auth;
    private File profileImageFile = null;

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

        // --- FIX IS HERE: More robust view initialization ---
        // Find the outer TextInputLayout first
        TextInputLayout usernameInputLayout = findViewById(R.id.textFieldUsername);
        TextInputLayout emailInputLayout = findViewById(R.id.textFieldEmail);
        TextInputLayout emailConfirmInputLayout = findViewById(R.id.textFieldEmailConfirm);
        TextInputLayout passwordInputLayout = findViewById(R.id.textFieldPassword);

        // Then get the EditText from inside the layout
        usernameEditText = (TextInputEditText) usernameInputLayout.getEditText();
        emailEditText = (TextInputEditText) emailInputLayout.getEditText();
        emailConfirmEditText = (TextInputEditText) emailConfirmInputLayout.getEditText();
        passwordEditText = (TextInputEditText) passwordInputLayout.getEditText();
        // --- END OF FIX ---

        registerButton = findViewById(R.id.buttonRegister);
        profileImageView = findViewById(R.id.iv_profile_picture);
        loginLinkTextView = findViewById(R.id.textViewLoginLink);

        registerButton.setOnClickListener(v -> registerButtonClick());

        loginLinkTextView.setOnClickListener(v -> {
            // Correctly finish the activity to go back to the login screen
            finish();
        });

        profileImageView.setOnClickListener(v -> {
            // Placeholder for image picker logic
            Toast.makeText(this, "Choose a profile picture.", Toast.LENGTH_SHORT).show();
            // openImagePicker(); // You will implement this method later
        });
    }

    private void registerButtonClick() {
        Log.d(TAG, "Register button clicked");

        // We can be sure these are not null now
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

        RegistrationManager registrationManager = new RegistrationManager(RegistrationActivity.this);
        registrationManager.startRegistration(
                email,
                password,
                profileImageFile, // Pass the File object (can be null)
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
