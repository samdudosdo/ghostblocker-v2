package com.ghostblocker;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public final class AreaRepository {
    private AreaRepository() {}

    public static List<GhostArea> load(Context context) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                context.getAssets().open("ghost_areas.json"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        JSONArray arr = new JSONArray(sb.toString());
        List<GhostArea> result = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            result.add(new GhostArea(
                    o.optString("name", "area" + i),
                    o.getInt("x"), o.getInt("y"),
                    o.getInt("w"), o.getInt("h")));
        }
        return result;
    }
}
