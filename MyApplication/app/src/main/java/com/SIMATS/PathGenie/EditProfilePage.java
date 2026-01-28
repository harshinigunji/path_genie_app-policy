package com.SIMATS.PathGenie;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.SIMATS.PathGenie.network.ApiConfig;
import com.SIMATS.PathGenie.network.VolleyMultipartRequest;
import com.SIMATS.PathGenie.utils.SessionManager;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputFilter;

/**
 * EditProfilePage - Allows user to edit their profile
 * Updated:
 * - Loads existing profile image
 * - Robust upload with resize and timeout policy
 */
public class EditProfilePage extends AppCompatActivity {

    private static final String TAG = "EditProfilePage";

    // UI Components
    private ImageView backButton;
    private TextView btnSave;
    private ProgressBar loadingProgress;
    private ScrollView contentScrollView;
    private ImageView profileImage;
    private View btnCamera; // FrameLayout
    private TextView btnChangePhoto;
    private EditText etFullName, etEmail, etPhone, etDateOfBirth;
    private EditText etEducationLevel, etSchool, etBoard, etAspiringCareer;
    private LinearLayout cardChangePassword;
    private Button btnSaveChanges;

    private SessionManager sessionManager;
    private int userId;

    // Image Picking
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile_page);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        if (userId == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupImagePickers();
        initViews();
        setupClickListeners();
        setupTextWatchers(); // Add real-time validation
        fetchProfileData();
    }

    private void setupImagePickers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        processImage(bitmap);
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            processImage(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void processImage(Bitmap bitmap) {
        // Show immediately locally
        profileImage.setImageBitmap(bitmap);
        // Upload
        uploadProfileImage(bitmap);
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        btnSave = findViewById(R.id.btnSave);
        loadingProgress = findViewById(R.id.loadingProgress);
        contentScrollView = findViewById(R.id.contentScrollView);
        profileImage = findViewById(R.id.profileImage);
        btnCamera = findViewById(R.id.btnCamera);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        etEducationLevel = findViewById(R.id.etEducationLevel);
        etSchool = findViewById(R.id.etSchool);
        etBoard = findViewById(R.id.etBoard);
        etAspiringCareer = findViewById(R.id.etAspiringCareer);

        cardChangePassword = findViewById(R.id.cardChangePassword);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveProfile());
        btnSaveChanges.setOnClickListener(v -> saveProfile());

        cardChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(EditProfilePage.this, ChangePasswordPage.class);
            startActivity(intent);
        });

        etDateOfBirth.setOnClickListener(v -> showDatePicker());

        View.OnClickListener photoListener = v -> showImageSourceDialog();
        btnCamera.setOnClickListener(photoListener);
        btnChangePhoto.setOnClickListener(photoListener);
        profileImage.setOnClickListener(photoListener);
    }

    /**
     * Setup TextWatchers for real-time validation while typing.
     */
    private void setupTextWatchers() {
        // Full Name - real-time validation (no numbers allowed)
        etFullName.addTextChangedListener(new TextWatcher() {
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
                    etFullName.setError("Name should contain only letters (no numbers)");
                } else {
                    etFullName.setError(null);
                }
            }
        });

        // Email - real-time validation
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String email = s.toString().trim();
                if (!email.isEmpty() && !isValidEmail(email)) {
                    etEmail.setError("Please enter a valid email format");
                } else {
                    etEmail.setError(null);
                }
            }
        });

        // Phone - real-time validation with max 10 digits filter
        // Set max length filter to 10 digits
        etPhone.setFilters(new InputFilter[] { new InputFilter.LengthFilter(10) });

        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String phone = s.toString().replaceAll("[^0-9]", "");
                if (phone.length() > 0 && phone.length() < 10) {
                    etPhone.setError("Phone number must be exactly 10 digits");
                } else if (phone.length() == 10) {
                    etPhone.setError(null); // Valid
                } else {
                    etPhone.setError(null); // Empty is OK (optional field)
                }
            }
        });

        // Education Level - real-time validation
        etEducationLevel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String level = s.toString().trim();
                if (!level.isEmpty() && !isValidEducationLevel(level)) {
                    etEducationLevel.setError("Only letters and numbers allowed");
                } else {
                    etEducationLevel.setError(null);
                }
            }
        });

        // School - real-time validation
        etSchool.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String school = s.toString().trim();
                if (!school.isEmpty() && !isValidSchoolName(school)) {
                    etSchool.setError("Invalid characters in school name");
                } else {
                    etSchool.setError(null);
                }
            }
        });

        // Board - real-time validation
        etBoard.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String board = s.toString().trim();
                if (!board.isEmpty() && !isValidBoard(board)) {
                    etBoard.setError("Only letters and numbers allowed");
                } else {
                    etBoard.setError(null);
                }
            }
        });

        // Aspiring Career - real-time validation
        etAspiringCareer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String career = s.toString().trim();
                if (!career.isEmpty() && !isValidCareer(career)) {
                    etAspiringCareer.setError("Invalid characters in career field");
                } else {
                    etAspiringCareer.setError(null);
                }
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) - 15; // Default to 15 years ago
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Validate BEFORE setting the date
                    Calendar today = Calendar.getInstance();
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    // Calculate age
                    int age = today.get(Calendar.YEAR) - selectedYear;
                    // Adjust if birthday hasn't occurred this year
                    if (today.get(Calendar.MONTH) < selectedMonth ||
                            (today.get(Calendar.MONTH) == selectedMonth
                                    && today.get(Calendar.DAY_OF_MONTH) < selectedDay)) {
                        age--;
                    }

                    // Check if future date
                    if (selectedDate.after(today)) {
                        etDateOfBirth.setText(""); // Clear the field
                        etDateOfBirth.setError("Date of birth cannot be in the future. Please select a valid date.");
                        Toast.makeText(this, "Invalid date! Date of birth cannot be in the future.", Toast.LENGTH_SHORT)
                                .show();
                    } else if (age < 10) {
                        // Age less than 10 - don't set the date, show error
                        etDateOfBirth.setText(""); // Clear the field
                        etDateOfBirth.setError("Age must be at least 10 years. Please select a valid date of birth.");
                        Toast.makeText(this, "Invalid age! You must be at least 10 years old.", Toast.LENGTH_SHORT)
                                .show();
                    } else if (age > 100) {
                        etDateOfBirth.setText(""); // Clear the field
                        etDateOfBirth.setError("Please enter a valid date of birth.");
                        Toast.makeText(this, "Invalid age! Please select a valid date of birth.", Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        // Valid date - set it
                        String date = selectedYear + "-" + String.format("%02d", selectedMonth + 1)
                                + "-" + String.format("%02d", selectedDay);
                        etDateOfBirth.setText(date);
                        etDateOfBirth.setError(null); // Clear any previous error
                    }
                }, year, month, day);

        // Prevent selecting future dates
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // Set minimum date to 100 years ago
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -100);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void showImageSourceDialog() {
        String[] options = { "Take Photo", "Choose from Gallery", "Remove Photo" };
        new AlertDialog.Builder(this)
                .setTitle("Change Profile Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndLaunch();
                    } else if (which == 1) {
                        galleryLauncher.launch("image/*");
                    } else {
                        confirmRemoveProfileImage();
                    }
                })
                .show();
    }

    private void confirmRemoveProfileImage() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Profile Photo")
                .setMessage("Are you sure you want to remove your profile photo?")
                .setPositiveButton("Remove", (dialog, which) -> removeProfileImage())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeProfileImage() {
        loadingProgress.setVisibility(View.VISIBLE);
        String url = ApiConfig.getBaseUrl() + "remove_profile_image.php";

        com.android.volley.toolbox.StringRequest request = new com.android.volley.toolbox.StringRequest(
                Request.Method.POST, url,
                response -> {
                    loadingProgress.setVisibility(View.GONE);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean status = jsonObject.optBoolean("status", false);
                        if (status) {
                            Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
                            profileImage.setImageResource(R.drawable.ic_avatar);
                        } else {
                            Toast.makeText(this, "Failed to remove picture", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loadingProgress.setVisibility(View.GONE);
                    Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void checkCameraPermissionAndLaunch() {
        if (checkSelfPermission(
                android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null);
        } else {
            requestPermissions(new String[] { android.Manifest.permission.CAMERA }, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(null);
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadProfileImage(Bitmap bitmap) {
        // Show loading but don't block user from editing other fields
        // Or show a toast saying "Uploading..."
        Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show();

        String url = ApiConfig.getBaseUrl() + "upload_profile_image.php";

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url,
                response -> {
                    try {
                        String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                        Log.d(TAG, "Upload Response: " + jsonString);
                        JSONObject jsonObject = new JSONObject(jsonString);
                        boolean status = jsonObject.optBoolean("status", false);
                        String message = jsonObject.optString("message", "Upload failed");

                        if (status) {
                            Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                            // If we received a new URL, we could log it
                            String imageUrl = jsonObject.optString("image_url");
                            Log.d(TAG, "New Image URL: " + imageUrl);
                        } else {
                            Toast.makeText(this, "Upload failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing upload response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    String errorMsg = "Network Error";
                    if (error.networkResponse != null) {
                        errorMsg += " (" + error.networkResponse.statusCode + ")";
                    }
                    Toast.makeText(this, errorMsg + ". Try a smaller image.", Toast.LENGTH_LONG).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                // Resize image before upload to avoid timeouts with large files
                byte[] imageData = getFileDataFromDrawable(bitmap);
                params.put("image", new DataPart("profile_" + System.currentTimeMillis() + ".jpg",
                        imageData, "image/jpeg"));
                return params;
            }
        };

        // Set explicit retry policy for slower connections or large files
        // 30 seconds timeout, 1 retry
        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(multipartRequest);
    }

    private byte[] getFileDataFromDrawable(Bitmap bitmap) {
        // Resize bitmap if too large (max 1024x1024)
        int maxHeight = 1024;
        int maxWidth = 1024;
        float scale = Math.min(((float) maxHeight / bitmap.getWidth()), ((float) maxWidth / bitmap.getHeight()));

        Bitmap finalBitmap = bitmap;
        if (scale < 1) {
            int width = Math.round(bitmap.getWidth() * scale);
            int height = Math.round(bitmap.getHeight() * scale);
            finalBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // Compress to JPEG 80%
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private void fetchProfileData() {
        loadingProgress.setVisibility(View.VISIBLE);
        contentScrollView.setVisibility(View.GONE);

        String url = ApiConfig.getBaseUrl() + "get_profile.php?user_id=" + userId;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    loadingProgress.setVisibility(View.GONE);
                    contentScrollView.setVisibility(View.VISIBLE);

                    try {
                        boolean status = response.optBoolean("status", false);
                        if (status) {
                            JSONObject data = response.getJSONObject("data");
                            populateForm(data);
                        } else {
                            Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response", e);
                    }
                },
                error -> {
                    loadingProgress.setVisibility(View.GONE);
                    contentScrollView.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Network error", error);
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }

    private void populateForm(JSONObject data) {
        try {
            etFullName.setText(data.optString("full_name", ""));
            etEmail.setText(data.optString("email", ""));
            etPhone.setText(data.optString("phone", ""));

            String dob = data.optString("date_of_birth", "");
            if (!dob.isEmpty() && !dob.equals("null")) {
                etDateOfBirth.setText(dob);
            }

            etEducationLevel.setText(data.optString("education_level", ""));
            etSchool.setText(data.optString("current_school", ""));
            etBoard.setText(data.optString("board", ""));
            etAspiringCareer.setText(data.optString("aspiring_career", ""));

            // Load existing profile image
            String imagePath = data.optString("profile_image", "");
            if (!imagePath.isEmpty()) {
                // If path is relative (uploads/...), prepend base url
                String fullUrl = imagePath.startsWith("http") ? imagePath : ApiConfig.getBaseUrl() + imagePath;
                loadProfileImage(fullUrl);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error populating form", e);
        }
    }

    private void loadProfileImage(String url) {
        ImageRequest imageRequest = new ImageRequest(url,
                bitmap -> profileImage.setImageBitmap(bitmap),
                0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                error -> {
                    Log.e(TAG, "Error loading image: " + error.getMessage());
                });
        Volley.newRequestQueue(this).add(imageRequest);
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
     * Validate email format.
     *
     * @param email The email to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Validate phone number format.
     * Must contain exactly 10 digits.
     *
     * @param phone The phone number to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidPhone(String phone) {
        // Remove any spaces, dashes, or parentheses
        String cleanPhone = phone.replaceAll("[\\s\\-()]+", "");
        // Check if it contains exactly 10 digits
        return cleanPhone.matches("^[0-9]{10}$");
    }

    /**
     * Calculate age from date of birth string (format: YYYY-MM-DD).
     *
     * @param dob Date of birth string
     * @return Age in years, or -1 if invalid format
     */
    private int calculateAge(String dob) {
        try {
            String[] parts = dob.split("-");
            if (parts.length != 3)
                return -1;

            int birthYear = Integer.parseInt(parts[0]);
            int birthMonth = Integer.parseInt(parts[1]);
            int birthDay = Integer.parseInt(parts[2]);

            Calendar today = Calendar.getInstance();
            int currentYear = today.get(Calendar.YEAR);
            int currentMonth = today.get(Calendar.MONTH) + 1; // Calendar months are 0-indexed
            int currentDay = today.get(Calendar.DAY_OF_MONTH);

            int age = currentYear - birthYear;

            // Adjust if birthday hasn't occurred this year
            if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
                age--;
            }

            return age;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Validate that date of birth represents an age of at least 10 years.
     *
     * @param dob Date of birth string (format: YYYY-MM-DD)
     * @return true if age is at least 10, false otherwise
     */
    private boolean isValidDateOfBirth(String dob) {
        int age = calculateAge(dob);
        return age >= 10;
    }

    /**
     * Validate education level field.
     * Should contain only letters, numbers, and spaces (e.g., "10th", "12th",
     * "Bachelor's").
     *
     * @param level The education level to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidEducationLevel(String level) {
        // Allow letters, numbers, spaces, apostrophes (for Bachelor's, Master's etc.)
        return level.matches("^[a-zA-Z0-9\\s']+$");
    }

    /**
     * Validate that education level matches the user's age.
     * Returns an error message if mismatch, null if valid.
     *
     * @param educationLevel The education level entered
     * @param age            The user's calculated age
     * @return Error message if mismatch, null if valid or cannot determine
     */
    private String getEducationAgeError(String educationLevel, int age) {
        if (educationLevel.isEmpty() || age < 0) {
            return null; // Cannot validate if either is empty/invalid
        }

        String levelLower = educationLevel.toLowerCase().trim();

        // Define expected age ranges for each education level
        // 10th grade: typically 14-18 years
        if (levelLower.contains("10th") || levelLower.contains("10 th") || levelLower.equals("10") ||
                levelLower.contains("sslc") || levelLower.contains("secondary")) {
            if (age < 14) {
                return "For 10th grade, age should be at least 14 years";
            }
            if (age > 18) {
                return "For 10th grade, age should not exceed 18 years";
            }
        }
        // 11th grade: typically 15-19 years
        else if (levelLower.contains("11th") || levelLower.contains("11 th") || levelLower.equals("11")) {
            if (age < 15) {
                return "For 11th grade, age should be at least 15 years";
            }
            if (age > 19) {
                return "For 11th grade, age should not exceed 19 years";
            }
        }
        // 12th grade: typically 16-20 years
        else if (levelLower.contains("12th") || levelLower.contains("12 th") || levelLower.equals("12") ||
                levelLower.contains("hsc") || levelLower.contains("higher secondary") ||
                levelLower.contains("plus two") || levelLower.contains("+2")) {
            if (age < 16) {
                return "For 12th grade, age should be at least 16 years";
            }
            if (age > 20) {
                return "For 12th grade, age should not exceed 20 years";
            }
        }
        // Bachelor's/Undergraduate/Graduation: typically 17-25 years
        else if (levelLower.contains("bachelor") || levelLower.contains("undergraduate") ||
                levelLower.contains("graduation") || levelLower.contains("graduate") ||
                levelLower.contains("ug") || levelLower.contains("b.tech") ||
                levelLower.contains("btech") || levelLower.contains("b.e") ||
                levelLower.contains("b.sc") || levelLower.contains("bsc") ||
                levelLower.contains("b.com") || levelLower.contains("bcom") ||
                levelLower.contains("b.a") || levelLower.contains("bba") ||
                levelLower.contains("bca") || levelLower.contains("b.pharma") ||
                levelLower.contains("bpharm") || levelLower.contains("llb") ||
                levelLower.contains("b.ed") || levelLower.contains("bed")) {
            if (age < 17) {
                return "For Graduation/Bachelor's degree, age should be at least 17 years";
            }
            if (age > 25) {
                return "For Graduation/Bachelor's degree, age should not exceed 25 years";
            }
        }
        // Master's/Postgraduate/Postgraduation: typically 21-30 years
        else if (levelLower.contains("master") || levelLower.contains("postgraduate") ||
                levelLower.contains("postgraduation") || levelLower.contains("post graduate") ||
                levelLower.contains("post graduation") ||
                levelLower.contains("pg") || levelLower.contains("m.tech") ||
                levelLower.contains("mtech") || levelLower.contains("m.e") ||
                levelLower.contains("m.sc") || levelLower.contains("msc") ||
                levelLower.contains("m.com") || levelLower.contains("mcom") ||
                levelLower.contains("m.a") || levelLower.contains("mba") ||
                levelLower.contains("mca") || levelLower.contains("m.pharma") ||
                levelLower.contains("mpharm") || levelLower.contains("llm") ||
                levelLower.contains("m.ed") || levelLower.contains("med")) {
            if (age < 21) {
                return "For Postgraduation/Master's degree, age should be at least 21 years";
            }
            if (age > 30) {
                return "For Postgraduation/Master's degree, age should not exceed 30 years";
            }
        }
        // PhD/Doctorate: typically 24+ years
        else if (levelLower.contains("phd") || levelLower.contains("ph.d") ||
                levelLower.contains("doctorate") || levelLower.contains("doctoral")) {
            if (age < 24) {
                return "For PhD, age should be at least 24 years";
            }
        }

        return null; // No error or unrecognized education level
    }

    /**
     * Validate school/institution name.
     * Should contain only letters, numbers, spaces, and common punctuation.
     *
     * @param school The school name to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidSchoolName(String school) {
        // Allow letters, numbers, spaces, periods, commas, hyphens, apostrophes
        return school.matches("^[a-zA-Z0-9\\s.,'\\-]+$");
    }

    /**
     * Validate board name.
     * Should contain only letters, numbers, and spaces (e.g., "CBSE", "ICSE",
     * "State Board").
     *
     * @param board The board name to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidBoard(String board) {
        return board.matches("^[a-zA-Z0-9\\s]+$");
    }

    /**
     * Validate aspiring career field.
     * Should contain only letters, numbers, spaces, and common punctuation.
     *
     * @param career The career field to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidCareer(String career) {
        // Allow letters, numbers, spaces, hyphens, ampersands, slashes
        return career.matches("^[a-zA-Z0-9\\s\\-&/]+$");
    }

    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String dob = etDateOfBirth.getText().toString().trim();
        String educationLevel = etEducationLevel.getText().toString().trim();
        String school = etSchool.getText().toString().trim();
        String board = etBoard.getText().toString().trim();
        String aspiringCareer = etAspiringCareer.getText().toString().trim();

        // ============ FULL NAME VALIDATION ============
        if (fullName.isEmpty()) {
            etFullName.setError("Name is required");
            etFullName.requestFocus();
            return;
        }

        if (fullName.length() < 2) {
            etFullName.setError("Name must be at least 2 characters");
            etFullName.requestFocus();
            return;
        }

        if (!isValidName(fullName)) {
            etFullName.setError("Name should contain only letters (no numbers)");
            etFullName.requestFocus();
            return;
        }

        // ============ EMAIL VALIDATION ============
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            etEmail.setError("Please enter a valid email (e.g., user@example.com)");
            etEmail.requestFocus();
            return;
        }

        // ============ PHONE VALIDATION ============
        if (!phone.isEmpty() && !isValidPhone(phone)) {
            etPhone.setError("Please enter a valid 10-digit phone number");
            etPhone.requestFocus();
            return;
        }

        // ============ DATE OF BIRTH VALIDATION ============
        if (!dob.isEmpty()) {
            int age = calculateAge(dob);
            if (age < 0) {
                etDateOfBirth.setError("Invalid date format");
                etDateOfBirth.requestFocus();
                return;
            }
            if (age < 10) {
                etDateOfBirth.setError("Age must be at least 10 years");
                etDateOfBirth.requestFocus();
                return;
            }
            if (age > 100) {
                etDateOfBirth.setError("Please enter a valid date of birth");
                etDateOfBirth.requestFocus();
                return;
            }
        }

        // ============ EDUCATION LEVEL VALIDATION ============
        if (!educationLevel.isEmpty() && !isValidEducationLevel(educationLevel)) {
            etEducationLevel.setError("Education level should contain only letters and numbers");
            etEducationLevel.requestFocus();
            return;
        }

        // ============ EDUCATION LEVEL vs AGE CROSS-VALIDATION ============
        if (!educationLevel.isEmpty() && !dob.isEmpty()) {
            int age = calculateAge(dob);
            String educationAgeError = getEducationAgeError(educationLevel, age);
            if (educationAgeError != null) {
                etEducationLevel.setError(educationAgeError);
                etEducationLevel.requestFocus();
                return;
            }
        }

        // ============ SCHOOL VALIDATION ============
        if (!school.isEmpty() && !isValidSchoolName(school)) {
            etSchool.setError("School name contains invalid characters");
            etSchool.requestFocus();
            return;
        }

        // ============ BOARD VALIDATION ============
        if (!board.isEmpty() && !isValidBoard(board)) {
            etBoard.setError("Board name should contain only letters and numbers");
            etBoard.requestFocus();
            return;
        }

        // ============ ASPIRING CAREER VALIDATION ============
        if (!aspiringCareer.isEmpty() && !isValidCareer(aspiringCareer)) {
            etAspiringCareer.setError("Career field contains invalid characters");
            etAspiringCareer.requestFocus();
            return;
        }

        // All validations passed - proceed with saving
        loadingProgress.setVisibility(View.VISIBLE);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("user_id", userId);
            requestBody.put("full_name", fullName);
            requestBody.put("email", email);
            requestBody.put("phone", phone);
            requestBody.put("date_of_birth", dob);
            requestBody.put("education_level", educationLevel);
            requestBody.put("current_school", school);
            requestBody.put("board", board);
            requestBody.put("aspiring_career", aspiringCareer);

            String url = ApiConfig.getBaseUrl() + "update_profile.php";
            Log.d(TAG, "Saving profile to: " + url);

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                    response -> {
                        loadingProgress.setVisibility(View.GONE);
                        Log.d(TAG, "Response: " + response.toString());

                        try {
                            boolean status = response.optBoolean("status", false);
                            if (status) {
                                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                String message = response.optString("message", "Failed to update profile");
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing response", e);
                            Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        loadingProgress.setVisibility(View.GONE);
                        Log.e(TAG, "Network error", error);
                        Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                    });

            queue.add(request);

        } catch (Exception e) {
            loadingProgress.setVisibility(View.GONE);
            Log.e(TAG, "Error creating request", e);
            Toast.makeText(this, "Error saving profile", Toast.LENGTH_SHORT).show();
        }
    }
}
