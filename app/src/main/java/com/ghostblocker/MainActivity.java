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

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try { areaCount = AreaRepository.load(this).size(); } catch (Exception ignored) {}
        buildUi();
        requestNotificationPermission();
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) refreshUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36, 44, 36, 36);
        root.setBackgroundColor(Color.rgb(16,17,20));

        TextView title = text("GHOSTBLOCKER", 28, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = text("Fixed ghost spots · no ADB required", 14, Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, 8, 0, 24);
        root.addView(subtitle, sp);

        modeText = text("", 18, Color.WHITE);
        modeText.setGravity(Gravity.CENTER);
        root.addView(modeText, new LinearLayout.LayoutParams(-1, -2));

        status = text("", 14, Color.LTGRAY);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams st = new LinearLayout.LayoutParams(-1, -2);
        st.setMargins(0, 8, 0, 26);
        root.addView(status, st);

        Button transparent = button("ACTIVATE · TRANSPARENT");
        transparent.setOnClickListener(v -> setMode(GhostState.Mode.TRANSPARENT));
        root.addView(transparent, buttonParams());

        Button black = button("SHOW · BLACK");
        black.setOnClickListener(v -> setMode(GhostState.Mode.BLACK));
        root.addView(black, buttonParams());

        Button clear = button("CLEAR / OFF");
        clear.setOnClickListener(v -> setMode(GhostState.Mode.OFF));
        root.addView(clear, buttonParams());

        Button permission = button("OVERLAY PERMISSION");
        permission.setOnClickListener(v -> openOverlaySettings());
        root.addView(permission, buttonParams());

        TextView info = text("Fixed areas: " + areaCount + "\nCoordinates are bundled in the APK.\nNo area editor is included.", 13, Color.LTGRAY);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
        ip.setMargins(0, 24, 0, 0);
        root.addView(info, ip);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        refreshUi();
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 58);
        p.setMargins(0, 6, 0, 6);
        return p;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        return b;
    }

    private TextView text(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        return t;
    }

    private void setMode(GhostState.Mode mode) {
        if (mode == GhostState.Mode.OFF) {
            GhostState.set(this, mode);
            stopService(new Intent(this, OverlayService.class));
            refreshUi();
            return;
        }
        if (!Settings.canDrawOverlays(this)) { openOverlaySettings(); return; }
        Intent i = new Intent(this, OverlayService.class)
                .setAction(OverlayService.ACTION_SET_MODE)
                .putExtra(OverlayService.EXTRA_MODE, mode.name());
        startServiceCompat(i);
        refreshUi();
    }

    private void startServiceCompat(Intent i) {
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    private void openOverlaySettings() {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 700);
        }
    }

    private void refreshUi() {
        if (modeText == null || status == null) return;
        GhostState.Mode m = GhostState.get(this);
        modeText.setText("MODE: " + m.name());
        status.setText("Overlay permission: " + (Settings.canDrawOverlays(this) ? "GRANTED" : "NOT GRANTED")
                + "\nFixed ghost areas: " + areaCount);
    }
}
