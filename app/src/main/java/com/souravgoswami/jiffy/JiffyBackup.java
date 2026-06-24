package com.souravgoswami.jiffy;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class JiffyBackup {
    private static final int SCHEMA_VERSION = 1;
    private static final String FORMAT = "jiffy_preferences_backup";
    private static final Set<String> BLACKLISTED_KEYS = blacklistedKeys();

    private JiffyBackup() {
    }

    static String exportJson(Context context, SharedPreferences prefs) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("schema_version", SCHEMA_VERSION);
        root.put("package_name", context.getPackageName());
        root.put("created_at_epoch_ms", System.currentTimeMillis());
        root.put("preferences", preferencesJson(prefs));
        return root.toString(2);
    }

    static int restoreJson(SharedPreferences prefs, String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (!FORMAT.equals(root.optString("format"))) {
            throw new JSONException("Unsupported backup format");
        }
        if (root.optInt("schema_version", 0) > SCHEMA_VERSION) {
            throw new JSONException("Unsupported backup schema");
        }

        JSONObject preferences = root.getJSONObject("preferences");
        SharedPreferences.Editor editor = prefs.edit().clear();
        int restored = 0;
        JSONArray names = preferences.names();
        if (names != null) {
            for (int index = 0; index < names.length(); index++) {
                String key = names.getString(index);
                if (isBlacklisted(key)) {
                    continue;
                }
                JSONObject entry = preferences.getJSONObject(key);
                putPreference(editor, key, entry);
                restored++;
            }
        }
        if (!editor.commit()) {
            throw new JSONException("Could not write restored preferences");
        }
        return restored;
    }

    private static JSONObject preferencesJson(SharedPreferences prefs) throws JSONException {
        JSONObject object = new JSONObject();
        Map<String, ?> all = prefs.getAll();
        List<String> keys = new ArrayList<>(all.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            if (isBlacklisted(key)) {
                continue;
            }
            Object value = all.get(key);
            if (value != null) {
                object.put(key, preferenceJson(value));
            }
        }
        return object;
    }

    private static boolean isBlacklisted(String key) {
        return BLACKLISTED_KEYS.contains(key);
    }

    private static Set<String> blacklistedKeys() {
        Set<String> keys = new HashSet<>();
        keys.add("stopwatch_running");
        keys.add("timer_running");
        return Collections.unmodifiableSet(keys);
    }

    private static JSONObject preferenceJson(Object value) throws JSONException {
        JSONObject object = new JSONObject();
        if (value instanceof Boolean) {
            object.put("type", "boolean");
            object.put("value", value);
        } else if (value instanceof Integer) {
            object.put("type", "integer");
            object.put("value", value);
        } else if (value instanceof Long) {
            object.put("type", "long");
            object.put("value", value);
        } else if (value instanceof Float) {
            object.put("type", "float");
            object.put("value", ((Float) value).doubleValue());
        } else if (value instanceof Set) {
            object.put("type", "string_set");
            object.put("value", stringSetJson((Set<?>) value));
        } else {
            object.put("type", "string");
            object.put("value", String.valueOf(value));
        }
        return object;
    }

    private static JSONArray stringSetJson(Set<?> set) {
        List<String> values = new ArrayList<>();
        for (Object value : set) {
            if (value != null) {
                values.add(String.valueOf(value));
            }
        }
        Collections.sort(values);
        JSONArray array = new JSONArray();
        for (String value : values) {
            array.put(value);
        }
        return array;
    }

    private static void putPreference(SharedPreferences.Editor editor, String key, JSONObject entry)
            throws JSONException {
        String type = entry.getString("type");
        switch (type) {
            case "boolean":
                editor.putBoolean(key, entry.getBoolean("value"));
                break;
            case "integer":
                editor.putInt(key, entry.getInt("value"));
                break;
            case "long":
                editor.putLong(key, entry.getLong("value"));
                break;
            case "float":
                editor.putFloat(key, (float) entry.getDouble("value"));
                break;
            case "string_set":
                editor.putStringSet(key, readStringSet(entry.getJSONArray("value")));
                break;
            case "string":
                editor.putString(key, entry.optString("value", ""));
                break;
            default:
                throw new JSONException("Unsupported preference type: " + type);
        }
    }

    private static Set<String> readStringSet(JSONArray array) throws JSONException {
        Set<String> values = new HashSet<>();
        for (int index = 0; index < array.length(); index++) {
            values.add(array.getString(index));
        }
        return values;
    }
}
