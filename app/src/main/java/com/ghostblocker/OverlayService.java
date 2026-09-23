package com.ghostblocker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.List;

public class OverlayService extends Service {
    public static final String ACTION_SET_MODE = "com.ghostblocker.action.SET_MODE";
    public static final String EXTRA_MODE = "mode";
    public static final String ACTION_CLEAR = "com.ghostblocker.action.CLEAR";
    public static final String ACTION_STOP = "com.ghostblocker.action.STOP";

    private static final String CHANNEL_ID = "ghostblocker";
    private WindowManager wm;
    private final List<View> overlays = new ArrayList<>();
    private GhostState.Mode mode = GhostState.Mode.OFF;

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createChannel();
        startForeground(1001, notification());
        mode = GhostState.get(this);
        if (mode != GhostState.Mode.OFF && Settings.canDrawOverlays(this)) applyMode(mode);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_CLEAR.equals(action)) setMode(GhostState.Mode.OFF);
            else if (ACTION_STOP.equals(action)) stopSelf();
            else if (ACTION_SET_MODE.equals(action)) {
                String value = intent.getStringExtra(EXTRA_MODE);
                if (value != null) {
                    try { setMode(GhostState.Mode.valueOf(value)); } catch (Exception ignored) {}
                }
            }
        }
        return START_STICKY;
    }

    public synchronized void setMode(GhostState.Mode newMode) {
        mode = newMode;
        GhostState.set(this, newMode);
        removeOverlays();
        if (newMode != GhostState.Mode.OFF && Settings.canDrawOverlays(this)) addOverlays(newMode);
    }

    private void applyMode(GhostState.Mode m) { removeOverlays(); addOverlays(m); }

    private void addOverlays(GhostState.Mode m) {
        try {
            for (GhostArea area : AreaRepository.load(this)) {
                View v = new View(this);
                v.setBackgroundColor(m == GhostState.Mode.BLACK ? Color.BLACK : Color.TRANSPARENT);
                v.setClickable(true);
                v.setFocusable(false);
                v.setOnTouchListener((view, event) -> true);

                int type = Build.VERSION.SDK_INT >= 26
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE;
                int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        area.w, area.h, type, flags, PixelFormat.TRANSLUCENT);
                lp.gravity = Gravity.TOP | Gravity.START;
                lp.x = area.x;
                lp.y = area.y;
                lp.setTitle("GhostBlocker:" + area.name);
                wm.addView(v, lp);
                overlays.add(v);
            }
        } catch (Exception ignored) {
            removeOverlays();
        }
    }

    private void removeOverlays() {
        for (View v : overlays) {
            try { wm.removeViewImmediate(v); } catch (Exception ignored) {}
        }
        overlays.clear();
    }

    public int overlayCount() { return overlays.size(); }
    public GhostState.Mode currentMode() { return mode; }

    private Notification notification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(com.ghostblocker.R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL_ID,
                    "GhostBlocker", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }

    @Override public void onDestroy() {
        removeOverlays();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
