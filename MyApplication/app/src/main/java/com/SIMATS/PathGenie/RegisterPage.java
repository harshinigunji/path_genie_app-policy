package com.SIMATS.PathGenie;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.SIMATS.PathGenie.network.ApiConfig;
import com.SIMATS.PathGenie.network.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * RegisterPage Activity handles new user registration.
 * Features:
 * - Full name, email, password input
 * - Password confirmation validation
 * - Password visibility toggle
 * - API integration with register.php
 * - Navigation to LoginPage on success
 */
public class RegisterPage extends AppCompatActivity {

    // UI Components
    private ImageView backButton;
    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private ImageView passwordToggle;
    private Button signupButton;
    private TextView loginText;

    // State
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_page);

        // Setup edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        initViews();

        // Setup click listeners
        setupClickListeners();

        // Setup real-time validation
        setupTextWatchers();
    }

    /**
     * Initialize all view references.
     */
    private void initViews() {
        backButton = findViewById(R.id.backButton);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        passwordToggle = findViewById(R.id.passwordToggle);
        signupButton = findViewById(R.id.signupButton);
        loginText = findViewById(R.id.loginText);
    }

    /**
     * Setup click listeners for all interactive elements.
     */
    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Sign up button
        signupButton.setOnClickListener(v -> attemptSignup());

        // Password visibility toggle
        passwordToggle.setOnClickListener(v -> togglePasswordVisibility());

        // Google sign-in (placeholder)

        // Login navigation
        loginText.setOnClickListener(v -> navigateToLogin());
    }

    /**
     * Toggle password field visibility.
     */
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            confirmPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordToggle.setImageResource(R.drawable.ic_eye_off);
        } else {
            // Show password
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            confirmPasswordEditText
                    .setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordToggle.setImageResource(R.drawable.ic_eye_on);
        }
        isPasswordVisible = !isPasswordVisible;

        // Keep cursor at end
        passwordEditText.setSelection(passwordEditText.getText().length());
        confirmPasswordEditText.setSelection(confirmPasswordEditText.getText().length());
    }

    /**
     * Setup TextWatchers for real-time validation while typing.
     */
    private void setupTextWatchers() {
        // Full Name - real-time validation (no numbers allowed)
        fullNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String name = s.toString().trim();
                if (!name.isEmpty() && !isValidName(name)) {
                    fullNameEditText.setError("Name should contain only letters (no numbers)");
                } else {
                    fullNameEditText.setError(null);
                }
            }
        });

        // Email - real-time validation
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String email = s.toString().trim();
                if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailEditText.setError("Please enter a valid email format");
                } else {
                    emailEditText.setError(null);
                }
            }
        });

        // Password - real-time validation
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                if (!password.isEmpty() && !isStrongPassword(password)) {
                    passwordEditText.setError(getPasswordStrengthError(password));
                } else {
                    passwordEditText.setError(null);
                }
                // Also check confirm password if it has content
                String confirm = confirmPasswordEditText.getText().toString();
                if (!confirm.isEmpty() && !password.equals(confirm)) {
                    confirmPasswordEditText.setError("Passwords do not match");
                } else if (!confirm.isEmpty()) {
                    confirmPasswordEditText.setError(null);
                }
            }
        });

        // Confirm Password - real-time validation
        confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String confirm = s.toString();
                String password = passwordEditText.getText().toString();
                if (!confirm.isEmpty() && !password.equals(confirm)) {
                    confirmPasswordEditText.setError("Passwords do not match");
                } else {
                    confirmPasswordEditText.setError(null);
                }
            }
        });
    }

    /**
     * Validate that name contains only letters and spaces (no numbers).
     *
     * @param name The name to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidName(String name) {
        // Name should contain only letters, spaces, and common name characters
        // (hyphens, apostrophes)
        // No numbers allowed
        String namePattern = "^[a-zA-Z\\s'-]+$";
        return name.matches(namePattern);
    }

    /**
     * Validate that password is strong.
     * Requirements:
     * - At least 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one number
     * - At least one special character
     *
     * @param password The password to validate
     * @return true if strong, false otherwise
     */
    private boolean isStrongPassword(String password) {
        if (password.length() < 8) {
            return false;
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowercase = true;
            } else if (Character.isDigit(c)) {
                hasNumber = true;
            } else if (specialChars.indexOf(c) >= 0) {
                hasSpecial = true;
            }
        }

        return hasUppercase && hasLowercase && hasNumber && hasSpecial;
    }

    /**
     * Get password strength error message.
     *
     * @param password The password to check
     * @return Error message describing missing requirements
     */
    private String getPasswordStrengthError(String password) {
        StringBuilder error = new StringBuilder("Password must have: ");
        boolean needsComma = false;

        if (password.length() < 8) {
            error.append("at least 8 characters");
            needsComma = true;
        }

        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasNumber = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0);

        if (!hasUppercase) {
            if (needsComma)
                error.append(", ");
            error.append("one uppercase");
            needsComma = true;
        }
        if (!hasLowercase) {
            if (needsComma)
                error.append(", ");
            error.append("one lowercase");
            needsComma = true;
        }
        if (!hasNumber) {
            if (needsComma)
                error.append(", ");
            error.append("one number");
            needsComma = true;
        }
        if (!hasSpecial) {
            if (needsComma)
                error.append(", ");
            error.append("one special character (!@#$%^&*...)");
        }

        return error.toString();
    }

    /**
     * Validate inputs and attempt registration API call.
     */
    private void attemptSignup() {
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        // Validate full name - not empty
        if (fullName.isEmpty()) {
            fullNameEditText.setError("Full name is required");
            fullNameEditText.requestFocus();
            return;
        }

        // Validate full name - minimum length
        if (fullName.length() < 2) {
            fullNameEditText.setError("Name must be at least 2 characters");
            fullNameEditText.requestFocus();
            return;
        }

        // Validate full name - no numbers allowed
        if (!isValidName(fullName)) {
            fullNameEditText.setError("Name should contain only letters (no numbers)");
            fullNameEditText.requestFocus();
            return;
        }

        // Validate email - not empty
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        // Validate email - proper format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email (e.g., user@example.com)");
            emailEditText.requestFocus();
            return;
        }

        // Validate password - not empty
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        // Validate password - strong password requirements
        if (!isStrongPassword(password)) {
            passwordEditText.setError(getPasswordStrengthError(password));
            passwordEditText.requestFocus();
            return;
        }

        // Validate confirm password - not empty
        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.setError("Please confirm your password");
            confirmPasswordEditText.requestFocus();
            return;
        }

        // Validate confirm password - matches
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            confirmPasswordEditText.requestFocus();
            return;
        }

        // Show loading state
        setLoadingState(true);

        // Make API call
        registerUser(fullName, email, password);
    }

    /**
     * Make registration - first Firebase Auth, then PHP API.
     *
     * @param name     User's full name
     * @param email    User's email
     * @param password User's password
     */
    private void registerUser(String name, String email, String password) {
        // First, create Firebase Auth user (for password reset functionality)
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Firebase user created, now register in PHP backend
                        registerUserInPhp(name, email, password);
                    } else {
                        setLoadingState(false);
                        String errorMessage = "Registration failed.";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("email address is already in use")) {
                                    errorMessage = "Email already registered. Please login.";
                                } else if (exceptionMessage.contains("badly formatted")) {
                                    errorMessage = "Please enter a valid email address.";
                                } else if (exceptionMessage.contains("weak password")) {
                                    errorMessage = "Password is too weak. Use at least 6 characters.";
                                } else {
                                    errorMessage = exceptionMessage;
                                }
                            }
                        }
                        Toast.makeText(RegisterPage.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Register user in PHP backend after Firebase Auth success.
     *
     * @param name     User's full name
     * @param email    User's email
     * @param password User's password
     */
    private void registerUserInPhp(String name, String email, String password) {
        // Create JSON request body
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("name", name);
            requestBody.put("email", email);
            requestBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            setLoadingState(false);
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.REGISTER,
                requestBody,
                response -> {
                    setLoadingState(false);
                    handleRegisterResponse(response);
                },
                error -> {
                    setLoadingState(false);
                    handleRegisterError(error);
                });

        // Add to request queue
        VolleySingleton.getInstance(this).addToRequestQueue(request, "register");
    }

    /**
     * Handle successful registration response.
     *
     * @param response JSON response from server
     */
    private void handleRegisterResponse(JSONObject response) {
        try {
            boolean status = response.getBoolean("status");

            if (status) {
                // Registration successful
                Toast.makeText(this, "Account created successfully! Please login.", Toast.LENGTH_LONG).show();

                // Navigate to Login
                navigateToLogin();

            } else {
                // Registration failed
                String message = response.optString("message", "Registration failed");
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle registration error.
     *
     * @param error Volley error object
     */
    private void handleRegisterError(com.android.volley.VolleyError error) {
        String message = "Network error. Please try again.";

        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            if (statusCode == 409) {
                message = "Email already registered";
            } else if (statusCode == 500) {
                message = "Server error. Please try later.";
            }

            // Try to parse error response
            try {
                String responseBody = new String(error.networkResponse.data, "UTF-8");
                JSONObject errorJson = new JSONObject(responseBody);
                if (errorJson.has("message")) {
                    message = errorJson.getString("message");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (error instanceof com.android.volley.NoConnectionError) {
            message = "No internet connection";
        } else if (error instanceof com.android.volley.TimeoutError) {
            message = "Connection timed out";
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Enable/disable loading state.
     *
     * @param isLoading true to show loading state
     */
    private void setLoadingState(boolean isLoading) {
        signupButton.setEnabled(!isLoading);
        signupButton.setText(isLoading ? "Creating account..." : "Sign Up");
        fullNameEditText.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        confirmPasswordEditText.setEnabled(!isLoading);
    }

    /**
     * Navigate to Login activity.
     */
    private void navigateToLogin() {
        Intent intent = new Intent(RegisterPage.this, LoginPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any pending requests
        VolleySingleton.getInstance(this).cancelRequests("register");
    }
}
