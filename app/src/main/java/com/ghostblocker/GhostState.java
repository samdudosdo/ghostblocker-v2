package com.ghostblocker;

import android.content.Context;

public final class GhostState {
    public enum Mode { OFF, TRANSPARENT, BLACK }
    private static final String PREFS = "ghostblocker_state";
    private static final String KEY_MODE = "mode";
    private GhostState() {}

    public static Mode get(Context c) {
        String s = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_MODE, Mode.OFF.name());
        try { return Mode.valueOf(s); } catch (Exception e) { return Mode.OFF; }
    }

    public static void set(Context c, Mode mode) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_MODE, mode.name()).apply();
    }
}
