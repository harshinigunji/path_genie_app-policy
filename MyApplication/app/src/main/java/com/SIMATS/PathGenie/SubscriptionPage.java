package com.SIMATS.PathGenie;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Collections;
import java.util.List;

/**
 * SubscriptionPage - Premium subscription offering page with Google Play
 * Billing integration.
 * Displayed after splash screen, allows users to subscribe or skip to login.
 * 
 * HOW GOOGLE PLAY BILLING WORKS:
 * 1. BillingClient connects to Google Play Store
 * 2. App queries available products (subscriptions) using the Product ID (SKU)
 * 3. When user taps Subscribe, the billing flow is launched
 * 4. Google Play handles the payment UI
 * 5. After purchase, app acknowledges and saves premium status
 */
public class SubscriptionPage extends AppCompatActivity implements PurchasesUpdatedListener {

    private static final String TAG = "SubscriptionPage";

    // ========================================================================
    // 🧪 DEBUG MODE - Set to FALSE before releasing to Play Store!
    // ========================================================================
    // When true: Shows a test dialog to simulate purchases without Play Console
    // When false: Uses real Google Play Billing (production mode)
    // ========================================================================
    private static final boolean DEBUG_MODE = false; // ✅ Production mode - uses real Google Play Billing

    // ========================================================================
    // 🔑 SUBSCRIPTION PRODUCT ID (SKU)
    // ========================================================================
    // This is obtained from Google Play Console:
    // 1. Go to Google Play Console > Your App > Monetize > Products > Subscriptions
    // 2. Create a new subscription product
    // 3. Enter a Product ID (e.g., "pathgenie_premium_monthly")
    // 4. Set up pricing, billing period, and other details
    // 5. Copy the Product ID here
    // ========================================================================
    private static final String SUBSCRIPTION_PRODUCT_ID = "pathgenie_premium_monthly";

    // SharedPreferences key for premium status
    private static final String PREFS_NAME = "subscription_prefs";
    private static final String KEY_IS_PREMIUM = "is_premium_user";

    // Billing client for Play Store communication
    private BillingClient billingClient;

    // Product details fetched from Play Store
    private ProductDetails productDetails;

    // UI elements
    private Button btnSubscribe;
    private TextView btnSkipForNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_subscription_page);

        // Setup edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Check if user already has premium subscription
        if (isPremiumUser()) {
            Log.d(TAG, "User already has premium, navigating to login");
            navigateToLoginPage();
            return;
        }

        initViews();
        setupBillingClient();
        setupClickListeners();
    }

    /**
     * Initialize view references.
     */
    private void initViews() {
        btnSubscribe = findViewById(R.id.btnSubscribe);
        btnSkipForNow = findViewById(R.id.btnSkipForNow);
    }

    /**
     * Setup click listeners for buttons.
     */
    private void setupClickListeners() {
        // Subscribe button - launches Google Play billing flow
        btnSubscribe.setOnClickListener(v -> launchSubscription());

        // Skip button - navigate to LoginPage
        btnSkipForNow.setOnClickListener(v -> navigateToLoginPage());
    }

    // ========================================================================
    // BILLING CLIENT SETUP
    // ========================================================================

    /**
     * Initialize and connect the BillingClient to Google Play.
     * This is the first step in the billing process.
     */
    private void setupBillingClient() {
        // Create billing client with purchase update listener
        billingClient = BillingClient.newBuilder(this)
                .setListener(this) // 'this' implements PurchasesUpdatedListener
                .enablePendingPurchases() // Required for subscriptions
                .build();

        // Start connection to Google Play
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected successfully");

                    // Query available subscription products
                    querySubscriptionProduct();

                    // Also check if user already has an active subscription
                    checkExistingPurchases();
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Reconnect when service disconnects
                Log.d(TAG, "Billing service disconnected, will retry");
                // You can implement retry logic here if needed
            }
        });
    }

    /**
     * Query the subscription product from Google Play.
     * This fetches the product details (price, description, etc.) from Play
     * Console.
     */
    private void querySubscriptionProduct() {
        // Build the query for our subscription product
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(SUBSCRIPTION_PRODUCT_ID)
                                .setProductType(BillingClient.ProductType.SUBS) // SUBS for subscription
                                .build()))
                .build();

        // Execute the query asynchronously
        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && productDetailsList != null
                    && !productDetailsList.isEmpty()) {

                // Store the product details for later use
                productDetails = productDetailsList.get(0);
                Log.d(TAG, "Product found: " + productDetails.getName());

                // Update UI with actual price from Play Console
                runOnUiThread(this::updatePriceFromProductDetails);
            } else {
                Log.e(TAG, "Failed to query products: " + billingResult.getDebugMessage());
            }
        });
    }

    /**
     * Check if user has any existing active subscriptions.
     * This is useful for restoring purchases after app reinstall.
     */
    private void checkExistingPurchases() {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();

        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        // User already has an active subscription
                        Log.d(TAG, "Found existing active subscription");
                        savePremiumStatus(true);
                        runOnUiThread(this::navigateToLoginPage);
                        return;
                    }
                }
            }
        });
    }

    /**
     * Update the price display with actual price from Google Play.
     */
    private void updatePriceFromProductDetails() {
        if (productDetails != null && productDetails.getSubscriptionOfferDetails() != null) {
            ProductDetails.SubscriptionOfferDetails offer = productDetails.getSubscriptionOfferDetails().get(0);

            // Get the pricing phase (e.g., "₹100/month")
            if (!offer.getPricingPhases().getPricingPhaseList().isEmpty()) {
                ProductDetails.PricingPhase pricingPhase = offer.getPricingPhases().getPricingPhaseList().get(0);
                String price = pricingPhase.getFormattedPrice();
                Log.d(TAG, "Subscription price: " + price);

                // You can update UI here if you want to show dynamic price
                // For now, the layout has hardcoded ₹100
            }
        }
    }

    // ========================================================================
    // PURCHASE FLOW
    // ========================================================================

    /**
     * Launch the Google Play subscription billing flow.
     * This opens the Google Play purchase dialog.
     * In DEBUG_MODE, shows a test dialog to simulate purchase.
     */
    private void launchSubscription() {
        // 🧪 DEBUG MODE: Show test dialog instead of real billing
        if (DEBUG_MODE) {
            showDebugPurchaseDialog();
            return;
        }

        // Production mode: Use real Google Play Billing
        if (productDetails == null) {
            Toast.makeText(this, "Subscription not available. Please try again.",
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Product details not loaded");
            return;
        }

        // Get the subscription offer
        if (productDetails.getSubscriptionOfferDetails() == null
                || productDetails.getSubscriptionOfferDetails().isEmpty()) {
            Toast.makeText(this, "No subscription offers available",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ProductDetails.SubscriptionOfferDetails offer = productDetails.getSubscriptionOfferDetails().get(0);

        // Build billing flow parameters
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offer.getOfferToken()) // Required for subscriptions
                                .build()))
                .build();

        // Launch the billing flow (Google Play purchase UI)
        BillingResult result = billingClient.launchBillingFlow(this, billingFlowParams);

        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: " + result.getDebugMessage());
        }
    }

    /**
     * 🧪 DEBUG MODE ONLY: Shows a test dialog to simulate purchase flow.
     * This allows testing the subscription flow without uploading to Play Console.
     * ⚠️ Remove or disable this in production!
     */
    private void showDebugPurchaseDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🧪 Test Mode - Premium Subscription")
                .setMessage("This is a TEST dialog for development.\n\n" +
                        "Product: Path Genie Premium\n" +
                        "Price: ₹100/month\n\n" +
                        "Choose an action to simulate:")
                .setPositiveButton("✅ Simulate Purchase", (dialog, which) -> {
                    // Simulate successful purchase
                    Log.d(TAG, "DEBUG: Simulating successful purchase");
                    Toast.makeText(this, "Welcome to Premium! 🎉", Toast.LENGTH_LONG).show();
                    savePremiumStatus(true);
                    navigateToLoginPage();
                })
                .setNegativeButton("❌ Cancel", (dialog, which) -> {
                    // Simulate cancelled purchase
                    Log.d(TAG, "DEBUG: Simulating cancelled purchase");
                    Toast.makeText(this, "Purchase cancelled", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("🔄 Reset Premium", (dialog, which) -> {
                    // Reset premium status for testing
                    Log.d(TAG, "DEBUG: Resetting premium status");
                    savePremiumStatus(false);
                    Toast.makeText(this, "Premium status reset. You are now a free user.",
                            Toast.LENGTH_SHORT).show();
                })
                .setCancelable(true)
                .show();
    }

    // ========================================================================
    // PURCHASE CALLBACK
    // ========================================================================

    /**
     * Called when a purchase is updated (completed, cancelled, or failed).
     * This is the PurchasesUpdatedListener callback.
     */
    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult,
            List<Purchase> purchases) {

        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {

            // Process each purchase
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }

        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {

            Log.d(TAG, "User cancelled the purchase");
            Toast.makeText(this, "Purchase cancelled", Toast.LENGTH_SHORT).show();

        } else {
            Log.e(TAG, "Purchase failed: " + billingResult.getDebugMessage());
            Toast.makeText(this, "Purchase failed. Please try again.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle a completed purchase.
     * This acknowledges the purchase and saves the premium status.
     */
    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            Log.d(TAG, "Purchase successful!");

            // IMPORTANT: You must acknowledge the purchase
            // If not acknowledged within 3 days, Google will refund it automatically
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

                billingClient.acknowledgePurchase(acknowledgeParams, ackResult -> {
                    if (ackResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged");

                        // Save premium status and navigate
                        savePremiumStatus(true);

                        runOnUiThread(() -> {
                            Toast.makeText(this, "Welcome to Premium! 🎉",
                                    Toast.LENGTH_LONG).show();
                            navigateToLoginPage();
                        });
                    } else {
                        Log.e(TAG, "Failed to acknowledge purchase: "
                                + ackResult.getDebugMessage());
                    }
                });
            } else {
                // Already acknowledged, just save and navigate
                savePremiumStatus(true);
                runOnUiThread(this::navigateToLoginPage);
            }

        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            // Purchase is pending (e.g., pending payment)
            Toast.makeText(this,
                    "Purchase is pending. You'll get access once payment is complete.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ========================================================================
    // PREMIUM STATUS MANAGEMENT
    // ========================================================================

    /**
     * Save the premium status to SharedPreferences.
     * In a production app, you should also verify this on your server.
     */
    private void savePremiumStatus(boolean isPremium) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply();
        Log.d(TAG, "Premium status saved: " + isPremium);
    }

    /**
     * Check if user has premium subscription.
     */
    private boolean isPremiumUser() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_PREMIUM, false);
    }

    /**
     * Static method to check premium status from other activities.
     * Usage: if (SubscriptionPage.isPremium(context)) { ... }
     */
    public static boolean isPremium(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_PREMIUM, false);
    }

    // ========================================================================
    // NAVIGATION
    // ========================================================================

    /**
     * Navigate to LoginPage.
     */
    private void navigateToLoginPage() {
        Intent intent = new Intent(SubscriptionPage.this, LoginPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ========================================================================
    // LIFECYCLE
    // ========================================================================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Always end the billing connection to prevent memory leaks
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}
