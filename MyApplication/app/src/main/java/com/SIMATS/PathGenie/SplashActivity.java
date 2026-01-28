package com.SIMATS.PathGenie;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * SplashActivity - Animated splash screen shown on app launch.
 * Displays Path Genie logo with smooth floating animations.
 * Always navigates to LoginPage after the splash.
 * LoginPage handles session check and redirects to HomePage if already logged
 * in.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 3200; // 3.2 seconds
    private static final long ENTRANCE_ANIMATION_DURATION = 900;
    private static final long FLOAT_ANIMATION_DURATION = 1500;

    private ImageView splashLogo;
    private TextView splashTagline;
    private ValueAnimator floatAnimator;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // Setup edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        startEntranceAnimation();
        scheduleNavigation();
    }

    /**
     * Initialize view references.
     */
    private void initViews() {
        splashLogo = findViewById(R.id.splashLogo);
        splashTagline = findViewById(R.id.splashTagline);

        // Set initial states for animation
        splashLogo.setAlpha(0f);
        splashLogo.setScaleX(0.2f);
        splashLogo.setScaleY(0.2f);
        splashLogo.setRotation(-5f);

        // Set initial state for tagline
        splashTagline.setAlpha(0f);
        splashTagline.setTranslationY(30f);
    }

    /**
     * Start the grand entrance animation with scale, fade, and rotation.
     */
    private void startEntranceAnimation() {
        AnimatorSet entranceSet = new AnimatorSet();

        // Scale up from small to normal with overshoot effect
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(splashLogo, "scaleX", 0.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(splashLogo, "scaleY", 0.2f, 1f);

        // Fade in
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(splashLogo, "alpha", 0f, 1f);

        // Subtle rotation for dynamic feel
        ObjectAnimator rotation = ObjectAnimator.ofFloat(splashLogo, "rotation", -5f, 0f);

        entranceSet.playTogether(scaleX, scaleY, fadeIn, rotation);
        entranceSet.setDuration(ENTRANCE_ANIMATION_DURATION);
        entranceSet.setInterpolator(new OvershootInterpolator(1.1f));

        // Start floating animation after entrance completes
        entranceSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                startFloatingAnimation();
                animateTagline();
            }
        });

        entranceSet.start();
    }

    /**
     * Animate the tagline with fade-in and slide-up effect.
     */
    private void animateTagline() {
        splashTagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Create smooth, magical floating animation that makes the genie look alive.
     */
    private void startFloatingAnimation() {
        // Floating up and down animation
        floatAnimator = ValueAnimator.ofFloat(0f, 1f);
        floatAnimator.setDuration(FLOAT_ANIMATION_DURATION);
        floatAnimator.setRepeatMode(ValueAnimator.REVERSE);
        floatAnimator.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        floatAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();

            // Smooth vertical floating (hover effect)
            float translateY = -20f * value;
            splashLogo.setTranslationY(translateY);

            // Subtle scale breathing effect
            float scale = 1f + (0.03f * value);
            splashLogo.setScaleX(scale);
            splashLogo.setScaleY(scale);

            // Very subtle rotation for natural movement
            float rotationValue = -1f + (2f * value);
            splashLogo.setRotation(rotationValue);
        });

        floatAnimator.start();

        // Add subtle shadow/glow pulse effect
        startGlowAnimation();
    }

    /**
     * Subtle glow pulse animation for magical effect.
     */
    private void startGlowAnimation() {
        ObjectAnimator glowAnimator = ObjectAnimator.ofFloat(splashLogo, "alpha", 1f, 0.92f, 1f);
        glowAnimator.setDuration(2000);
        glowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        glowAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        glowAnimator.start();
    }

    /**
     * Schedule navigation after delay.
     */
    private void scheduleNavigation() {
        handler.postDelayed(this::navigateToSubscriptionPage, SPLASH_DELAY);
    }

    /**
     * Navigate to SubscriptionPage.
     * SubscriptionPage allows users to subscribe or skip to LoginPage.
     */
    private void navigateToSubscriptionPage() {
        // Stop floating animation
        if (floatAnimator != null) {
            floatAnimator.cancel();
        }

        // Navigate to SubscriptionPage first
        // User can subscribe or skip to LoginPage
        Intent intent = new Intent(SplashActivity.this, SubscriptionPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Apply fade transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null);
        if (floatAnimator != null) {
            floatAnimator.cancel();
        }
    }
}
