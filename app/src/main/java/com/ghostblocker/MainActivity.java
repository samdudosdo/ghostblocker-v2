package com.ghostblocker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView status;
    private TextView modeText;
    private int areaCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            areaCount = AreaRepository.load(this).size();
        } catch (Exception ignored) {
        }

        buildUi();
        requestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (status != null) {
            refreshUi();
        }
    }

    private void buildUi() {

        int paddingHorizontal = dp(24);
        int paddingTop = dp(32);
        int paddingBottom = dp(28);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
                paddingHorizontal,
                paddingTop,
                paddingHorizontal,
                paddingBottom
        );
        root.setBackgroundColor(Color.rgb(16, 17, 20));

        // ============================================================
        // TITLE
        // ============================================================

        TextView title = text(
                "GHOSTBLOCKER",
                28,
                Color.WHITE
        );

        title.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        -1,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        root.addView(title, titleParams);

        // ============================================================
        // SUBTITLE
        // ============================================================

        TextView subtitle = text(
                "Fixed ghost spots · no ADB required",
                14,
                Color.LTGRAY
        );

        subtitle.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(
                        -1,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        subtitleParams.setMargins(
                0,
                dp(8),
                0,
                dp(22)
        );

        root.addView(subtitle, subtitleParams);

        // ============================================================
        // CURRENT MODE
        // ============================================================

        modeText = text(
                "",
                18,
                Color.WHITE
        );

        modeText.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams modeParams =
                new LinearLayout.LayoutParams(
                        -1,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        root.addView(modeText, modeParams);

        // ============================================================
        // STATUS
        // ============================================================

        status = text(
                "",
                14,
                Color.LTGRAY
        );

        status.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        -1,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        statusParams.setMargins(
                0,
                dp(8),
                0,
                dp(22)
        );

        root.addView(status, statusParams);

        // ============================================================
        // MENU 1 - TRANSPARENT
        // ============================================================

        Button transparent =
                button("ACTIVATE · TRANSPARENT");

        transparent.setOnClickListener(
                v -> setMode(GhostState.Mode.TRANSPARENT)
        );

        root.addView(
                transparent,
                buttonParams()
        );

        // ============================================================
        // MENU 2 - BLACK
        // ============================================================

        Button black =
                button("SHOW · BLACK");

        black.setOnClickListener(
                v -> setMode(GhostState.Mode.BLACK)
        );

        root.addView(
                black,
                buttonParams()
        );

        // ============================================================
        // MENU 3 - OFF
        // ============================================================

        Button clear =
                button("CLEAR / OFF");

        clear.setOnClickListener(
                v -> setMode(GhostState.Mode.OFF)
        );

        root.addView(
                clear,
                buttonParams()
        );

        // ============================================================
        // MENU 4 - OVERLAY PERMISSION
        // ============================================================

        Button permission =
                button("OVERLAY PERMISSION");

        permission.setOnClickListener(
                v -> openOverlaySettings()
        );

        root.addView(
                permission,
                buttonParams()
        );

        // ============================================================
        // INFORMATION
        // ============================================================

        TextView info = text(
                "Fixed areas: " + areaCount
                        + "\nCoordinates are bundled in the APK."
                        + "\nNo area editor is included.",
                13,
                Color.LTGRAY
        );

        info.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams infoParams =
                new LinearLayout.LayoutParams(
                        -1,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        infoParams.setMargins(
                0,
                dp(20),
                0,
                0
        );

        root.addView(info, infoParams);

        // ============================================================
        // SCROLL VIEW
        // ============================================================

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);
        scroll.addView(root);

        setContentView(scroll);

        refreshUi();
    }

    // ================================================================
    // BUTTON LAYOUT
    // ================================================================

    private LinearLayout.LayoutParams buttonParams() {

        int height = dp(64);
        int margin = dp(6);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        height
                );

        params.setMargins(
                0,
                margin,
                0,
                margin
        );

        return params;
    }

    // ================================================================
    // BUTTON CREATION
    // ================================================================

    private Button button(String label) {

        Button button =
                new Button(this);

        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);

        button.setGravity(
                Gravity.CENTER
        );

        button.setPadding(
                dp(12),
                0,
                dp(12),
                0
        );

        return button;
    }

    // ================================================================
    // TEXT CREATION
    // ================================================================

    private TextView text(
            String value,
            float size,
            int color
    ) {

        TextView text =
                new TextView(this);

        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);

        return text;
    }

    // ================================================================
    // DP CONVERSION
    // ================================================================

    private int dp(int value) {

        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
                        + 0.5f
        );
    }

    // ================================================================
    // SET GHOST MODE
    // ================================================================

    private void setMode(
            GhostState.Mode mode
    ) {

        // ------------------------------------------------------------
        // OFF
        // ------------------------------------------------------------

        if (mode == GhostState.Mode.OFF) {

            GhostState.set(
                    this,
                    mode
            );

            stopService(
                    new Intent(
                            this,
                            OverlayService.class
                    )
            );

            refreshUi();

            return;
        }

        // ------------------------------------------------------------
        // OVERLAY PERMISSION
        // ------------------------------------------------------------

        if (!Settings.canDrawOverlays(this)) {

            openOverlaySettings();

            return;
        }

        // ------------------------------------------------------------
        // START OVERLAY SERVICE
        // ------------------------------------------------------------

        Intent intent =
                new Intent(
                        this,
                        OverlayService.class
                )
                        .setAction(
                                OverlayService.ACTION_SET_MODE
                        )
                        .putExtra(
                                OverlayService.EXTRA_MODE,
                                mode.name()
                        );

        startServiceCompat(intent);

        refreshUi();
    }

    // ================================================================
    // START SERVICE COMPATIBILITY
    // ================================================================

    private void startServiceCompat(
            Intent intent
    ) {

        if (Build.VERSION.SDK_INT >= 26) {

            startForegroundService(intent);

        } else {

            startService(intent);
        }
    }

    // ================================================================
    // OPEN OVERLAY PERMISSION
    // ================================================================

    private void openOverlaySettings() {

        Intent intent =
                new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse(
                                "package:" + getPackageName()
                        )
                );

        startActivity(intent);
    }

    // ================================================================
    // NOTIFICATION PERMISSION
    // ================================================================

    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    700
            );
        }
    }

    // ================================================================
    // REFRESH UI STATUS
    // ================================================================

    private void refreshUi() {

        if (modeText == null
                || status == null) {

            return;
        }

        GhostState.Mode mode =
                GhostState.get(this);

        modeText.setText(
                "MODE: " + mode.name()
        );

        status.setText(
                "Overlay permission: "
                        + (
                        Settings.canDrawOverlays(this)
                                ? "GRANTED"
                                : "NOT GRANTED"
                )
                        + "\nFixed ghost areas: "
                        + areaCount
        );
    }
}
